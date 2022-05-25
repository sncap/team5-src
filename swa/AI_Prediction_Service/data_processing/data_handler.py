import threading
import time
from queue_handler import QueueHandler
from data_sender import DataSender
from data_publisher import DataPublisher
import json
from multiprocessing import Pool

class DataHandler():
    def __init__(self, config):
        self.id = id
        self.que = QueueHandler()
        self.senders, self.publishers = self.init_component(config)


    def init_component(self, config):
        publishers = {}
        senders = {}
        publishers['CIFAR10'] = DataPublisher(config['prediction_publisher_config'], "CIFAR10")
        publishers['CANCER'] = DataPublisher(config['prediction_publisher_config'], "CANCER")
        senders['CIFAR10_DATA_SOURCE'] = DataSender(config['cifar10_predict_server_config'])
        senders['CANCER_DATA_SOURCE'] =  DataSender(config['cancer_predict_server_config'])
        return senders, publishers

    def worker(self, item):
        print('worker', item)
        start_time = time.time()

        data = item['data']
        source_topic_name = item['exchange']
        ## DATA Filter
        item = json.dumps(item)
        response = self.senders[source_topic_name].predict(item)

        res = json.loads(response.read().decode())
        topic_name = res['model']
        prediction_result = res['prediction_result']
        msg = json.dumps({
            "data": data,
            "prediction_result": prediction_result
        })
        print('model name: ', topic_name)
        self.publishers[topic_name].send_msg(msg)
        print('{}-{}: {}ms'.format(self.id, topic_name, round((time.time() - start_time) * 1000), 2))

if __name__ == '__main__':
    dh = DataHandler('A')
    dh.run()