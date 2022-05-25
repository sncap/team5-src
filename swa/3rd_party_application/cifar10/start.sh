#!/bin/bash

echo $1 $2 $3 
if [ $3 -eq 0 ] ; then
	echo "CIFAR10 3rd Party Application Start"
else
	echo "CANCER 3rd Party Application Startt"
fi

python3 /workspace/AI_Prediction_Service/3rd_party_services/main.py -ip $1 -port $2 -t $3
