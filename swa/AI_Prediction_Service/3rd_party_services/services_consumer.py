import pika
import json
import argparse

class ServicesConsumer:
    def __init__(self, config):
        print('ServicesConsumer', config)
        self.is_connected = False
        self.credentials = pika.PlainCredentials(config['id'], config['passwd'])
        self.ip = config['ip']
        self.port = config['port']
        self.queue_name = config['queue_name']
        self.topic_name = config['topic']


    def callback(self, ch, method, properties, body):
        body = json.loads(body)
        data = body['data']
        prediction_result = body['predictions']
        print('method.exchange', method.exchange)
        print("[{} {}] {}".format(method.exchange, prediction_result, data))

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
    parser = argparse.ArgumentParser(description="[3rd CIFAR10] Main")
    parser.add_argument('--config_file', '-cf', default='../../config/3rd_party_services_config.json', type=str,
                        help='config file path')
    args = parser.parse_args()
    with open(args.config_file, 'r') as cf:
        config = json.load(cf)
        print("Config " + str(config))
    consumer = ServicesConsumer(config)
    consumer.connect()