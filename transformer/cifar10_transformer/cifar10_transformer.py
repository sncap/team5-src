import argparse
import logging
from typing import Dict

import kfserving
import numpy as np

import cv2
import tensorflow as tf

logging.basicConfig(level=kfserving.constants.KFSERVING_LOGLEVEL)

CLASS = ['airplane', 'automobile', 'bird', 'cat', 'deer', 'dog', 'frog', 'horse', 'ship', 'truck']

def image_transform(instance): 
    img = np.array(instance, dtype=np.uint8)
    img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
    img = img.reshape(-1,3).T.reshape(-1)
    img = tf.convert_to_tensor([img], np.uint8)
    img = tf.reshape(img, (-1, 3, 32, 32))
    img = tf.transpose(img, (0, 2, 3, 1))
    img = tf.cast(img, tf.float32)
    img = img / 255
    return img.numpy().tolist()
#     return img

def parsing_prediction(predictions):
    return CLASS[np.argmax(predictions)]


class ImageTransformer(kfserving.KFModel):
    def __init__(self, name: str, predictor_host: str):
        super().__init__(name)
        self.predictor_host = predictor_host 
        
    def preprocess(self, inputs: Dict) -> Dict:
        return {'instances':  image_transform(inputs['instances'])}
    
    def postprocess(self, inputs: Dict) -> Dict:
        return {'predictions': parsing_prediction(inputs['predictions'][0])}
    
if __name__ == "__main__":
    DEFAULT_MODEL_NAME = "cifar10"
    parser = argparse.ArgumentParser(parents=[kfserving.kfserver.parser])
    parser.add_argument('--model_name', default=DEFAULT_MODEL_NAME,
                        help='The name that the model is served under.')
    parser.add_argument('--predictor_host', help='The URL for the model predict function', required=True)
    args, _ = parser.parse_known_args()
    transformer = ImageTransformer(args.model_name, predictor_host=args.predictor_host)
    kfserver = kfserving.KFServer()
    kfserver.start(models=[transformer])
