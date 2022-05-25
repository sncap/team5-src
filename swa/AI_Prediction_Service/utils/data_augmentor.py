import base64
import numpy as np

def base64_encoding(file_path):
    with open(file_path, 'rb') as f:
        data = f.read()
        encoded = base64.b64encode(data)
        encoded = encoded.decode("utf-8")
        print('"'+encoded+'"')
    return encoded

def base64_decoding(encoded):
    decoded = np.fromstring(base64.b64decode(encoded), dtype=np.uint8)
    print(decoded)
    return decoded



if __name__ == '__main__':
    file_path = "D://DATA//workspace//swa//AI_Prediction_Service//data_sample//cifar10//test//airplane//0000.jpg"
    e = base64_encoding(file_path)
    d = base64_decoding(e)