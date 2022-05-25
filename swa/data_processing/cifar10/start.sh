#!/bin/bash

echo $1 $2 $3 $4 $5 $6
if [ $5 -eq 0 ] ; then
	echo "CIFAR10 DATA Processing Start"
else
	echo "CANCER DATA Processing Start"
fi

python3 /workspace/AI_Prediction_Service/data_processing/main.py -ip $1 -port $2 -sip $3 -sport $4 -t $5 -tn $6
