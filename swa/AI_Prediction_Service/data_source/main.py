import os
import sys
root = os.path.dirname(os.path.abspath(os.path.dirname(__file__)))
print(root)

add_path_list = ['3rd_party_services', 'config', 'data_processing', 'data_sample', 'data_source', 'server', 'utils']

for add_path in add_path_list:
    if not root in sys.path:
        sys.path.append(root)

    if not add_path in sys.path:
        sys.path.append(os.path.join(root, add_path))

from data_publisher import DataPublisher
import json
from data_augmentor import base64_encoding
import argparse
import logging
import random
import time
import cv2
import numpy as np

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger("DATA SOURCE GENERATOR")

def is_iteration_validated(key):
    try:
        key = int(key)
    except:
        logger.error('using number only for the iteration')
        return False
    if not (0 < key <= 100000):
        logger.error('range(0 < iteration < 100,000) only for the iteration')
        return False
    return True

def get_msg(config, topic):
    if topic is 'CIFAR10_DATA_SOURCE':
        folder_list = ['airplane', 'automobile', 'bird', 'cat', 'deer', 'dog', 'frog', 'horse', 'ship', 'truck']
        index = random.randint(0, 9)
        file_path = os.path.join(os.path.join(root, config['cifar10_file_path']), folder_list[index])
        file_list = os.listdir(file_path)
        index = random.randint(0, len(file_list)-1)
        data_source_path = os.path.join(file_path, file_list[index])
        img = cv2.imread(data_source_path)
        img = np.expand_dims(img, axis=0)
        msg = json.dumps({
            "data": img.tolist()
        })
        print(msg[:100])
        return msg
    else:
        file_path = os.path.join(root, config['cancer_file_path'])
        with open(file_path, 'r') as f:
            data = f.readlines()
            index = random.randint(1, len(data) - 1)
        p_data = data[index].strip()
        p_data = p_data[p_data.index(',')+1:]
        p_data = p_data[p_data.index(',') + 1:]
        p_data_array = p_data.split(',')
        x = np.array(p_data_array)
        y = x.astype(np.float)
        msg = json.dumps({
            "data": [y.tolist()]
        })
        print(msg)
        return msg


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="[Data Source] Main")

    parser.add_argument('--mq_ip', '-ip', default='70.70.10.39', type=str, help='rabbit mq ip')
    parser.add_argument('--mq_port', '-port', default='5672', type=str, help='rabbit mq port')
    parser.add_argument('--topic', '-t', default='0', type=int, help='0 is cifar10, 1 is cancer')
    parser.add_argument('--interval', '-iv', default='50', type=int, help='ms')
    parser.add_argument('--config_file', '-cf', default='config/data_source_config.json', type=str, help='config file path')
    args = parser.parse_args()
    with open(os.path.join(root, args.config_file), 'r') as cf:
        config = json.load(cf)
    config['rabbit_config']['ip'] = args.mq_ip
    config['rabbit_config']['port'] = args.mq_port
    interval = args.interval / 1000
    if args.topic is 0:
        topic = 'CIFAR10_DATA_SOURCE'
    else:
        topic = 'CANCER_DATA_SOURCE'
    print('topic', topic)
    data_publisher = DataPublisher(config['rabbit_config'], topic)

    while True:
        time.sleep(interval)
        msg = get_msg(config['common_config'], topic)
        data_publisher.send_msg(msg)
    '''    
    dps.append(DataPublisher(config['rabbit_config'], 'CANCER_DATA_SOURCE'))
    while True:
        print('--------------------------------------------------------------')
        print('Topic name?(1. CIFAR10_DATA_SOURCE, 2. CANCER_DATA_SOURCE)')
        source = input()

        if source is None:
            continue
            
        if int(source) is 1:
            topic_name = 'CIFAR10_DATA_SOURCE'
        else:
            topic_name = 'CANCER_DATA_SOURCE'
        print('--------------------------------------------------------------')

        print('--------------------------------------------------------------')
        print('How many send the message?')
        print('--------------------------------------------------------------')
        iteration = input()
        if is_iteration_validated(iteration):
            for i in range(int(iteration)):
                if topic_name is 'CIFAR10_DATA_SOURCE':
                    msg = get_cifar10_msg(config['common_config'])
                    dps[0].send_msg(msg)
                else:
                    msg = get_cancer_msg(config['common_config'])
                    dps[1].send_msg(msg)
    '''
