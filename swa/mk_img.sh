#!/bin/bash
echo "Start Docker build and Make images"

m_fd=("data_source" "data_processing" "3rd_party_application")
s_fd=("cifar10" "cancer")
target=("Dockerfile" "start.sh")
repo="swa"

for mfd in ${m_fd[@]}
do
	for sfd in ${s_fd[@]}
	do
		d_path="./$mfd/$sfd/${target[0]}"
		s_path="./$mfd/$sfd/${target[1]}"
		d_dst="./${target[0]}"
		s_dst="./${target[1]}"
		echo cp $d_path $s_path
		cp $d_path $d_dst 
		cp $s_path $s_dst
		tag="$repo/$mfd/$sfd:v1.0"
		echo docker build ./ -t $tag
		docker build ./ -t $tag
	done
done
