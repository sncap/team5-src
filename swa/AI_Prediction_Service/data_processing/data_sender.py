import requests

class DataSender:
    def __init__(self, config):
        self.ip = config['sip']
        self.port = config['sport']
        self.model = config['model']
        self.url = config['url']

    def predict(self, msg):
        try:
            HOST = '{}.myspace.example.com'.format(self.model)
            headers = {'Content-type': 'application/json', 'Host': HOST}
            addresss = '{}:{}{}'.format(self.ip, self.port, self.url)
            response = requests.post(addresss, data=msg, headers=headers)
            return response
        except Exception as e:
            return e

