import pika
import json
from data_augmentor import base64_encoding

class DataPublisher:
    def __init__(self, config):
        self.credentials = pika.PlainCredentials(config['id'], config['passwd'])
        self.topic_name = config['topic']
        self.ip = config['ip']
        self.port = config['port']
        self.queue_name = config['queue_name']
        self.channel, self.connection, self.routing_key, = self.connect()

    def connect(self):
        connection = pika.BlockingConnection(pika.ConnectionParameters(host=self.ip, port=self.port, credentials=self.credentials))
        channel = connection.channel()
        channel.exchange_declare(exchange=self.topic_name, exchange_type='topic')
        return channel, connection, self.queue_name

    def send_msg(self, msg):
        try:
            self.channel.basic_publish(exchange=self.topic_name, routing_key=self.routing_key, body=msg)

        except Exception as e:
            print('', e)
            self.channel, self.connection, self.routing_key, = self.connect()
            self.send_msg(msg)

    def disconnect(self):
        print('disconnect')
        self.connection.close()

if __name__ == '__main__':
    config = {
                "ip": "70.70.10.39",
                "port": "5672",
                "queue_name": "park",
                "id": "park",
                "passwd": "1111"
            }
    file_path = "D://DATA//workspace//swa//AI_Prediction_Service//data_sample//cifar10//test//airplane//0000.jpg"
    img_encoded = base64_encoding(file_path)

    msg = json.dumps({
        "img": img_encoded
    })

    topic_name = 'CIFAR10_DATA_SOURCE'
    mqtt = DataPublisher(config, topic_name)
    for i in range(50):
        print(i)
        mqtt.send_msg(msg)