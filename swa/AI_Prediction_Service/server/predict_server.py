import os
import sys

### add source path
def add_path(path):
    if path not in sys.path:
        sys.path.insert(0, path)

root_dir = os.path.abspath(os.path.dirname(sys.argv[0]))
root_dir = root_dir[:-7]
add_path(root_dir)

from flask import Flask, request, jsonify
from flask_cors import CORS
#from service import Service
import argparse

import multiprocessing
from datetime import datetime

class Config:
    TITLE       = 'SWA'
    NAME        = 'SWA_Emulator'
    PORT        = 3000
    HOST            = '.'.join([str(0)] * 4)

    @staticmethod
    def update(attrs):
        if type(attrs) != dict:
            if not hasattr(attrs, '__dict__'):
                return
            attrs = attrs.__dict__
        for key, value in attrs.items():
            setattr(Config, key, value)

app = Flask(__name__)
CORS(app)
#service = Service()

@app.route("/swa/cifar10", methods=['POST'])
def cifar10():
    print('[Server]prediction')
    try:
        param = request.json
        print(param)
        return jsonify({'model':'CIFAR10', 'prediction_result': 'airplain'})
    except Exception as e:
        print(e)
        return jsonify({'result': False})


@app.route("/swa/cancer", methods=['POST'])
def cancer():
    print('[Server]prediction')
    try:
        param = request.json
        print(param)
        return jsonify({'model': 'CANCER', 'prediction_result': 'positive'})
    except Exception as e:
        print(e)
        return jsonify({'result': False})



@app.route("/")
def index():
    return 'index page'


def start():
    multiprocessing.freeze_support()
    try:
        from cheroot.wsgi import Server as WSGIServer, PathInfoDispatcher
    except ImportError:
        from cherrypy.wsgiserver import CherryPyWSGIServer as \
            WSGIServer, WSGIPathInfoDispatcher as PathInfoDispatcher

    d = PathInfoDispatcher({'/': app})
    server = WSGIServer((Config.HOST, Config.PORT), d)

    try:
        print('server start')
        print(Config.HOST, Config.PORT)
        server.start()
    except KeyboardInterrupt:
        print('<< STOP: %s (Stop Date: %s) >>' % (Config.TITLE, datetime.today().strftime("%Y%m%d")))
        server.stop()

if __name__ == "__main__":
    ### HOST & PORT
    parser = argparse.ArgumentParser()
    parser.add_argument('--HOST', default=Config.HOST, type=str, help='host address')
    parser.add_argument('--PORT', default=Config.PORT, type=int, help='host port')
    print(parser.parse_args())
    Config.update(parser.parse_args())

    ### check port
    def is_port_in_use(port):
        import socket
        with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
            return s.connect_ex(('127.0.0.1', port)) == 0

    if is_port_in_use(Config.PORT):
        print('<< PORT is in use >>')
        sys.exit(0)

    #start()
    app.run(host=Config.HOST, port=Config.PORT)