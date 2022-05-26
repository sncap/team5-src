import os
import sys
import argparse
from multiprocessing import Process, Pool
import multiprocessing
from queue_handler import QueueHandler
root = os.path.dirname(os.path.abspath(os.path.dirname(__file__)))
print(root)

add_path_list = ['3rd_party_services', 'config', 'data_processing', 'data_sample', 'data_source', 'server', 'utils']
# topic_name: CIFAR10_DATA_SOURCE, CANCER_DATA_SOURCE

for add_path in add_path_list:
    if not root in sys.path:
        sys.path.append(root)

    if not add_path in sys.path:
        sys.path.append(os.path.join(root, add_path))

from data_consumer import DataConsumer
from data_sender import DataSender
from data_publisher import DataPublisher
import json

import time
import requests

def send_dlq(config, data, reason):
    error_config = config
    if 'CIFAR10' in config['rabbit_config']['topic'] :
        error_config['rabbit_config']['topic'] = 'CIFAR10_DLQ'
    else:
        error_config['rabbit_config']['topic'] = 'CANCER_DLQ'
    data_publisher = DataPublisher(config['rabbit_config'])
    msg = json.dumps({
        "data": data
    })

    print('[ERROR]Send Dead Letter Queue([{}]) for that reason {}'.format(error_config['rabbit_config']['topic'], reason))
    data_publisher.send_msg(msg)

def worker(item, config):

    data_sender = DataSender(config['rabbit_config'])
    data_publisher = DataPublisher(config['rabbit_config'])
    start_time = time.time()
    data = item['data']
    s_msg = json.dumps({
        "signature_name": "serving_default",
        "instances": data
    })
    # hanlde data filter
    source_topic_name = item['exchange']
    print(source_topic_name)
    response = data_sender.predict(s_msg)
    if type(response) is requests.exceptions.ConnectionError:
        send_dlq(config, data, response)
    else:
        try:
            res = response.content
            res = json.loads(s=res)

            if "CIFAR10" in source_topic_name:
                topic_name = 'CIFAR10'
            else:
                topic_name = 'CANCER'
    
            msg = json.dumps({
                "data": data,
                "predictions": res['predictions']
            })
            data_publisher.send_msg(msg)
            print('{} | {} | {} | {}ms'.format(multiprocessing.current_process(), topic_name, res['predictions'], round((time.time() - start_time) * 1000), 2))
        except Exception as e:
            send_dlq(config, data ,e) 





if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="[Data Processing] Main")
    parser.add_argument('--mq_ip', '-ip', default='70.70.10.39', type=str, help='rabbit mq ip')
    parser.add_argument('--mq_port', '-port', default='5672', type=str, help='rabbit mq port')
    parser.add_argument('--service_ip', '-sip', default='http://70.70.202.133', type=str, help='prediction server ip')
    parser.add_argument('--service_port', '-sport', default='3000', type=str, help='prediction server port')
    parser.add_argument('--topic', '-t', default='0', type=int, help='0 is cifar10, 1 is cancer')
    parser.add_argument('--thread_num', '-tn', default='3', type=int, help='num of data handler')
    parser.add_argument('--config_file', '-cf', default='config/data_processing_config.json', type=str, help='config file path')
    args = parser.parse_args()
    thread_num = args.thread_num
    with open(os.path.join(root, args.config_file), 'r') as cf:
        config = json.load(cf)
    config['rabbit_config']['ip'] = args.mq_ip
    config['rabbit_config']['port'] = args.mq_port
    config['rabbit_config']['sip'] = args.service_ip
    config['rabbit_config']['sport'] = args.service_port
    if args.topic is 0:
        config['rabbit_config']['c_topic'] = 'CIFAR10_DATA_SOURCE'
        config['rabbit_config']['topic'] = 'CIFAR10'
        config['rabbit_config']['model'] = 'cifar10'
        config['rabbit_config']['url'] = '/v1/models/cifar10:predict'
    else:
        config['rabbit_config']['c_topic'] = 'CANCER_DATA_SOURCE'
        config['rabbit_config']['topic'] = 'CANCER'
        config['rabbit_config']['model'] = 'cancer'
        config['rabbit_config']['url'] = '/v1/models/cancer:predict'

    print("Config " + str(config))
    p = Pool(thread_num)
    consumer = DataConsumer(config['rabbit_config'])
    consumer.start()
    que = QueueHandler()
    while True:
        que_size = que.current_size()
        if que_size > 0:
            print("Current que size:", que_size)        
            item = json.loads(que.get())
            p.apply(worker, args=(item, config, ))

