
from python:3.7.8-buster
MAINTAINER kyoungwon

ENV http_proxy 'http://70.10.15.10:8080'
ENV https_proxy 'http://70.10.15.10:8080'
RUN apt-get update
RUN python -m pip install --upgrade pip
RUN pip install pika
RUN pip install numpy
COPY ./AI_Prediction_Service /workspace/AI_Prediction_Service
COPY ./start.sh /data/start.sh 
VOLUME ["/DATA"]
WORKDIR /workspace/AI_Prediction_Service

CMD /data/start.sh 34.145.3.171 5672 0 
