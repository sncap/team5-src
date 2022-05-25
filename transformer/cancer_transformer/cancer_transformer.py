import argparse
import logging
from typing import Dict

import kfserving


logging.basicConfig(level=kfserving.constants.KFSERVING_LOGLEVEL)

CLASS = ['negative', 'positive']

def parsing_prediction(predictions):
    return CLASS[int(predictions[0])]


class ImageTransformer(kfserving.KFModel):
    def __init__(self, name: str, predictor_host: str):
        super().__init__(name)
        self.predictor_host = predictor_host 
        
#     def preprocess(self, inputs: Dict) -> Dict:
#         return {'instances':  image_transform(inputs['instances'])}
    
    def postprocess(self, inputs: Dict) -> Dict:
        return {'predictions': parsing_prediction(inputs['predictions'])}
    
if __name__ == "__main__":
    DEFAULT_MODEL_NAME = "cancer"
    parser = argparse.ArgumentParser(parents=[kfserving.kfserver.parser])
    parser.add_argument('--model_name', default=DEFAULT_MODEL_NAME,
                        help='The name that the model is served under.')
    parser.add_argument('--predictor_host', help='The URL for the model predict function', required=True)
    args, _ = parser.parse_known_args()
    transformer = ImageTransformer(args.model_name, predictor_host=args.predictor_host)
    kfserver = kfserving.KFServer()
    kfserver.start(models=[transformer])

