import pika
import sys
import json
import argparse
from queue_handler import QueueHandler
import threading
class DataConsumer(threading.Thread):
    def __init__(self, config):
        threading.Thread.__init__(self)
        print('DataConsumer', config)
        self.is_connected = False
        self.credentials = pika.PlainCredentials(config['id'], config['passwd'])
        self.que = QueueHandler()
        self.ip = config['ip']
        self.port = config['port']
        self.queue_name = config['queue_name']
        self.topic_name = config['c_topic']
        print('Topic Name: ', self.topic_name)



    def callback(self, ch, method, properties, body):
        body = json.loads(body)
        self.que.put(json.dumps({
                        "exchange": method.exchange,
                        "data": body["data"]
                    }))

    def run(self):
        self.connect()

    def connect(self):
        if self.is_connected:
            self.disconnect()

        connection = pika.BlockingConnection(pika.ConnectionParameters(host=self.ip, port=self.port, credentials=self.credentials))
        channel = connection.channel()
        channel.exchange_declare(exchange=self.topic_name, exchange_type='topic')
        result = channel.queue_declare('', exclusive=True)
        queue_name = result.method.queue
        channel.queue_bind(exchange=self.topic_name, queue=queue_name, routing_key='park')
        channel.basic_consume(queue=queue_name, on_message_callback=self.callback, auto_ack=True)
        channel.start_consuming()


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description="[Data Processing] Main")
    parser.add_argument('--config_file', '-cf', default='../config/local_data_processing_config.json', type=str,
                        help='config file path')
    parser.add_argument('--thread_num', '-tn', default='3', type=int, help='num of data handler')
    # parser.add_argument('--topic_name', '-tname', default='CIFAR10_DATA_SOURCE', type=str, help='num of data handler')
    args = parser.parse_args()
    thread_num = args.thread_num
    with open(args.config_file, 'r') as cf:
        config = json.load(cf)
        print("Config " + str(config))

    consumer = DataConsumer(config)
    consumer.connect()