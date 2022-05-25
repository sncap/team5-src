FROM python:3.7-slim

RUN pip install kfserving==0.4.1
ENV APP_HOME /app
WORKDIR $APP_HOME
ADD cancer_transformer.py /app/

ENTRYPOINT ["python", "cancer_transformer.py"]
