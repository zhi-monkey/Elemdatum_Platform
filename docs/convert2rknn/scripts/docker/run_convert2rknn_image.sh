#!/bin/bash

IMAGE_TAG=convert2rknn:rknn-toolkit1.7.3
CONTAINER_TAG=convert2rknn
docker rm -f $CONTAINER_TAG
docker run --rm -it \
    -v ./dataset/coco8:/dataset \
    -v ./weight/detect:/weight \
    -e CONVERT_TARGET_PLATFORM=rv1126 \
    -e CONVERT_INPUT_MODEL_NAME=best.onnx \
    -e CONVERT_OUTPUT_MODEL_NAME=best.rknn \
    -e CONVERT_RKNN_QUANTIZED_DTYPE=asymmetric_quantized-u8 \
    -e CONVERT_RKNN_QUANTIZED_IMG_NUM=100 \
    -e CONVERT_REORDER_CHANNEL=RGB \
    -e CONVERT_RKNN_MEAN_VALUES=0_0_0 \
    -e CONVERT_RKNN_STD_VALUES=255_255_255 \
    --name $CONTAINER_TAG $IMAGE_TAG $1
# docker exec -it $CONTAINER_TAG bash