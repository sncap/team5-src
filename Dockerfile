FROM tensorflow/tensorflow:2.1.0-py3

RUN pip install kfserving==0.4.1 numpy opencv-python
RUN apt-get update
RUN apt-get -y install libgl1-mesa-glx
ENV APP_HOME /app
WORKDIR $APP_HOME
ADD cifar10_transformer.py /app/

ENTRYPOINT ["python", "cifar10_transformer.py", "--predictor_host", "cifar10.myspace.example.com", "--model_name", "cifar10"]
