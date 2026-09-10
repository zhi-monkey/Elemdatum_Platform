#!/bin/bash

IMAGE_TAG=convert2rknn:rknn-toolkit1.7.3
docker rmi $IMAGE_TAG
docker build -f docker/convert2rknn-rknn-toolkit1.7.3.dockerfile -t $IMAGE_TAG .