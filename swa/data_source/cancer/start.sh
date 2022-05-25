#!/bin/bash

echo $1 $2 $3 $4
if [ $3 -eq 0 ] ; then
	echo "CIFAR10 DATA Source Start"
else
	echo "CANCER DATA Source Start"
fi

python3 /workspace/AI_Prediction_Service/data_source/main.py -ip $1 -port $2 -t $3 -iv $4
