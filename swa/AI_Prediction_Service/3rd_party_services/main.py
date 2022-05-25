from services_consumer import ServicesConsumer
import os
import json
import argparse

root = os.path.dirname(os.path.abspath(os.path.dirname(__file__)))

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="[Data Processing] Main")
    parser.add_argument('--mq_ip', '-ip', default='70.70.10.39', type=str, help='rabbit mq ip')
    parser.add_argument('--mq_port', '-port', default='5672', type=str, help='rabbit mq port')
    parser.add_argument('--topic', '-t', default='0', type=int, help='0 is cifar10, 1 is cancer')
    parser.add_argument('--config_file', '-cf', default='config/3rd_party_services_config.json', type=str, help='config file path')
    args = parser.parse_args()
    with open(os.path.join(root, args.config_file), 'r') as cf:
        config = json.load(cf)
    config['rabbit_config']['ip'] = args.mq_ip
    config['rabbit_config']['port'] = args.mq_port
    if args.topic is 0:
        config['rabbit_config']['topic'] = 'CIFAR10'
    else:
        config['rabbit_config']['topic'] = 'CANCER'

    consumer = ServicesConsumer(config['rabbit_config'])
    consumer.connect()
