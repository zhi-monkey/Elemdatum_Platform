#!/bin/bash

IMAGE_TAG=convert2rknn:rknn-toolkit1.7.3
docker rmi registry.cn-hangzhou.aliyuncs.com/kaixjl/aiserver:$IMAGE_TAG
docker tag $IMAGE_TAG registry.cn-hangzhou.aliyuncs.com/kaixjl/aiserver:$IMAGE_TAG
docker push registry.cn-hangzhou.aliyuncs.com/kaixjl/aiserver:$IMAGE_TAG