#!/bin/bash
echo "Start Docker images push"

docker images | grep swa
docker login
docker tag swa/3rd_party_application/cancer:v1.1 sncap/3rd_party_app_cancer:v1.2
docker push sncap/3rd_party_app_cancer:v1.2
docker tag swa/data_source/cancer:v1.1 sncap/data_source_cancer:v1.2
docker push sncap/data_source_cancer:v1.2
docker tag swa/data_source/cifar10:v1.1 sncap/data_source_cifar10:v1.2
docker push sncap/data_source_cifar10:v1.2
docker tag swa/3rd_party_application/cifar10:v1.1 sncap/3rd_party_app_cifar10:v1.2
docker push sncap/3rd_party_app_cifar10:v1.2
docker tag swa/data_processing/cancer:v1.1 sncap/data_processing_cancer:v1.2
docker push sncap/data_processing_cancer:v1.2
docker tag swa/data_processing/cifar10:v1.1 sncap/data_processing_cifar10:v1.2
docker push sncap/data_processing_cifar10:v1.2

echo "Finished push"

