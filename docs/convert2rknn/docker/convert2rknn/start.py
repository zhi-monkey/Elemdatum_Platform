from enum import Enum
import os
import urllib
import traceback
import time
import sys
import numpy as np
import cv2
from rknn.api import RKNN


# Environment Variables
# TARGET_PLATFORM=os.environ["TARGET_PLATFORM"] if "TARGET_PLATFORM" in os.environ and len(os.environ["TARGET_PLATFORM"]) > 0 and os.environ["TARGET_PLATFORM"]!=os.environ["DEFINE_TARGET_PLATFORM"] else (print(f"TARGET_PLATFORM use default value: {os.environ['DEFAULT_TARGET_PLATFORM']}"), os.environ["DEFAULT_TARGET_PLATFORM"])[1]
# INPUT_MODEL_NAME=os.environ["INPUT_MODEL_NAME"] if "INPUT_MODEL_NAME" in os.environ and len(os.environ["INPUT_MODEL_NAME"]) > 0 and os.environ["INPUT_MODEL_NAME"]!=os.environ["DEFINE_INPUT_MODEL_NAME"] else (print(f"INPUT_MODEL_NAME use default value: {os.environ['DEFAULT_INPUT_MODEL_NAME']}"), os.environ["DEFAULT_INPUT_MODEL_NAME"])[1]
# OUTPUT_MODEL_NAME=os.environ["OUTPUT_MODEL_NAME"] if "OUTPUT_MODEL_NAME" in os.environ and len(os.environ["OUTPUT_MODEL_NAME"]) > 0 and os.environ["OUTPUT_MODEL_NAME"]!=os.environ["DEFINE_OUTPUT_MODEL_NAME"] else (print(f"OUTPUT_MODEL_NAME use default value: {os.environ['DEFAULT_OUTPUT_MODEL_NAME']}"), os.environ["DEFAULT_OUTPUT_MODEL_NAME"])[1]
# RKNN_QUANTIZED_DTYPE=os.environ["RKNN_QUANTIZED_DTYPE"] if "RKNN_QUANTIZED_DTYPE" in os.environ and len(os.environ["RKNN_QUANTIZED_DTYPE"]) > 0 and os.environ["RKNN_QUANTIZED_DTYPE"]!=os.environ["DEFINE_RKNN_QUANTIZED_DTYPE"] else (print(f"RKNN_QUANTIZED_DTYPE use default value: {os.environ['DEFAULT_RKNN_QUANTIZED_DTYPE']}"), os.environ["DEFAULT_RKNN_QUANTIZED_DTYPE"])[1]
# RKNN_QUANTIZED_IMG_NUM=os.environ["RKNN_QUANTIZED_IMG_NUM"] if "RKNN_QUANTIZED_IMG_NUM" in os.environ and len(os.environ["RKNN_QUANTIZED_IMG_NUM"]) > 0 and os.environ["RKNN_QUANTIZED_IMG_NUM"]!=os.environ["DEFINE_RKNN_QUANTIZED_IMG_NUM"] else (print(f"RKNN_QUANTIZED_IMG_NUM use default value: {os.environ['DEFAULT_RKNN_QUANTIZED_IMG_NUM']}"), os.environ["DEFAULT_RKNN_QUANTIZED_IMG_NUM"])[1]
# REORDER_CHANNEL=os.environ["REORDER_CHANNEL"] if "REORDER_CHANNEL" in os.environ and len(os.environ["REORDER_CHANNEL"]) > 0 and os.environ["REORDER_CHANNEL"]!=os.environ["DEFINE_REORDER_CHANNEL"] else (print(f"REORDER_CHANNEL use default value: {os.environ['DEFAULT_REORDER_CHANNEL']}"), os.environ["DEFAULT_REORDER_CHANNEL"])[1]
# RKNN_MEAN_VALUES=os.environ["RKNN_MEAN_VALUES"] if "RKNN_MEAN_VALUES" in os.environ and len(os.environ["RKNN_MEAN_VALUES"]) > 0 and os.environ["RKNN_MEAN_VALUES"]!=os.environ["DEFINE_RKNN_MEAN_VALUES"] else (print(f"RKNN_MEAN_VALUES use default value: {os.environ['DEFAULT_RKNN_MEAN_VALUES']}"), os.environ["DEFAULT_RKNN_MEAN_VALUES"])[1]
# RKNN_STD_VALUES=os.environ["RKNN_STD_VALUES"] if "RKNN_STD_VALUES" in os.environ and len(os.environ["RKNN_STD_VALUES"]) > 0 and os.environ["RKNN_STD_VALUES"]!=os.environ["DEFINE_RKNN_STD_VALUES"] else (print(f"RKNN_STD_VALUES use default value: {os.environ['DEFAULT_RKNN_STD_VALUES']}"), os.environ["DEFAULT_RKNN_STD_VALUES"])[1]

# INPUT_IMAGE_CHANNEL=os.environ["INPUT_IMAGE_CHANNEL"] if "INPUT_IMAGE_CHANNEL" in os.environ and len(os.environ["INPUT_IMAGE_CHANNEL"]) > 0 else "3"
# INPUT_IMAGE_SIZE=os.environ["INPUT_IMAGE_SIZE"] if "INPUT_IMAGE_SIZE" in os.environ and len(os.environ["INPUT_IMAGE_SIZE"]) > 0 else "640"
# MODEL_CLASS_NUM=os.environ["MODEL_CLASS_NUM"] if "MODEL_CLASS_NUM" in os.environ and len(os.environ["MODEL_CLASS_NUM"]) > 0 else "80"


DEFAULT_TARGET_PLATFORM="rv1126"
DEFAULT_INPUT_MODEL_NAME="best.onnx"
DEFAULT_OUTPUT_MODEL_NAME="best.rknn"
DEFAULT_RKNN_QUANTIZED_DTYPE="asymmetric_quantized-u8"
DEFAULT_RKNN_QUANTIZED_IMG_NUM="100"
DEFAULT_REORDER_CHANNEL="RGB"
DEFAULT_RKNN_MEAN_VALUES="0_0_0"
DEFAULT_RKNN_STD_VALUES="255_255_255"

TARGET_PLATFORM=os.environ["CONVERT_TARGET_PLATFORM"]
INPUT_MODEL_NAME=os.environ["CONVERT_INPUT_MODEL_NAME"]
OUTPUT_MODEL_NAME=os.environ["CONVERT_OUTPUT_MODEL_NAME"]
RKNN_QUANTIZED_DTYPE=os.environ["CONVERT_RKNN_QUANTIZED_DTYPE"]
RKNN_QUANTIZED_IMG_NUM=os.environ["CONVERT_RKNN_QUANTIZED_IMG_NUM"]
REORDER_CHANNEL=os.environ["CONVERT_REORDER_CHANNEL"]
RKNN_MEAN_VALUES=os.environ["CONVERT_RKNN_MEAN_VALUES"]
RKNN_STD_VALUES=os.environ["CONVERT_RKNN_STD_VALUES"]

# Check and convert
if len(TARGET_PLATFORM) == 0:
    print(f"TARGET_PLATFORM use default value: {DEFAULT_TARGET_PLATFORM}")
    TARGET_PLATFORM = DEFAULT_TARGET_PLATFORM
if TARGET_PLATFORM not in ("rk1806", "rk1808", "rk3399pro", "rv1126", "rv1109"):
    raise Exception(f"TARGET_PLATFORM should be one of 'rk1806', 'rk1808', 'rk3399pro', 'rv1126', 'rv1109', but got {TARGET_PLATFORM}.")

if INPUT_MODEL_NAME is None or len(INPUT_MODEL_NAME) == 0:
    print(f"INPUT_MODEL_NAME is empty. Search for onnx file in /weight/output automatically.")
    try:
        INPUT_MODEL_NAME = [it for it in os.listdir("/weight/output") if it.endswith(".onnx")][0]
    except:
        raise Exception(f"Cannot found onnx file in /weight/output, please ensure onnx model file exists.")

if OUTPUT_MODEL_NAME is None or len(OUTPUT_MODEL_NAME) == 0:
    print(f"OUTPUT_MODEL_NAME is empty. Use default value.")
    OUTPUT_MODEL_NAME = INPUT_MODEL_NAME[:-5] + ".rknn"

if len(RKNN_QUANTIZED_DTYPE) == 0:
    print(f"RKNN_QUANTIZED_DTYPE use default value: {DEFAULT_RKNN_QUANTIZED_DTYPE}")
    RKNN_QUANTIZED_DTYPE = DEFAULT_RKNN_QUANTIZED_DTYPE
if RKNN_QUANTIZED_DTYPE not in ("asymmetric_quantized-u8", "dynamic_fixed_point-i8", "dynamic_fixed_point-i16"):
    raise Exception(f"RKNN_QUANTIZED_DTYPE should be one of 'asymmetric_quantized-u8', 'dynamic_fixed_point-i8', 'dynamic_fixed_point-i16', but got {RKNN_QUANTIZED_DTYPE}.")

if len(RKNN_QUANTIZED_IMG_NUM) == 0:
    print(f"RKNN_QUANTIZED_IMG_NUM use default value: {DEFAULT_RKNN_QUANTIZED_IMG_NUM}")
    RKNN_QUANTIZED_IMG_NUM = DEFAULT_RKNN_QUANTIZED_IMG_NUM
RKNN_QUANTIZED_IMG_NUM = int(RKNN_QUANTIZED_IMG_NUM)
if not RKNN_QUANTIZED_IMG_NUM > 0:
    print(f"RKNN_QUANTIZED_IMG_NUM should be a number greater than 0, but got {RKNN_QUANTIZED_IMG_NUM}.")

if len(REORDER_CHANNEL) == 0:
    print(f"REORDER_CHANNEL use default value: {DEFAULT_REORDER_CHANNEL}")
    REORDER_CHANNEL = DEFAULT_REORDER_CHANNEL
if REORDER_CHANNEL not in ("RGB", "BGR"):
    raise Exception(f"REORDER_CHANNEL should be one of 'RGB', 'BGR', but got {REORDER_CHANNEL}.")
REORDER_CHANNEL = {"RGB": '0 1 2', "BGR": '2 1 0'}[REORDER_CHANNEL]

if len(RKNN_MEAN_VALUES) == 0:
    print(f"RKNN_MEAN_VALUES use default value: {DEFAULT_RKNN_MEAN_VALUES}")
    RKNN_MEAN_VALUES = DEFAULT_RKNN_MEAN_VALUES
try:
    RKNN_MEAN_VALUES = [ [ float(it2) for it2 in it.split("_") ] for it in RKNN_MEAN_VALUES.split("#") ]
except:
    raise Exception(f"Fail to parse RKNN_MEAN_VALUES. Got {RKNN_MEAN_VALUES}")

if len(RKNN_STD_VALUES) == 0:
    print(f"RKNN_STD_VALUES use default value: {DEFAULT_RKNN_STD_VALUES}")
    RKNN_STD_VALUES = DEFAULT_RKNN_STD_VALUES
try:
    RKNN_STD_VALUES = [ [ float(it2) for it2 in it.split("_") ] for it in RKNN_STD_VALUES.split("#") ]
except:
    raise Exception(f"Fail to parse RKNN_STD_VALUES. Got {RKNN_STD_VALUES}")


# Config
ONNX_MODEL = f'/weight/output/{INPUT_MODEL_NAME}'
RKNN_MODEL = f'/weight/output_convert/{OUTPUT_MODEL_NAME}'
MEAN_VALUES = RKNN_MEAN_VALUES
STD_VALUES = RKNN_STD_VALUES
RKNN_CONFIG = dict(
    mean_values=MEAN_VALUES,
    std_values=STD_VALUES,
    reorder_channel=REORDER_CHANNEL, # '0 1 2', '2 1 0'
    target_platform=[TARGET_PLATFORM],
    quantized_dtype=RKNN_QUANTIZED_DTYPE, # asymmetric_quantized-u8, dynamic_fixed_point-8, dynamic_fixed_point-16
    batch_size=RKNN_QUANTIZED_IMG_NUM, # default is 100
    quantized_algorithm="normal", # normal，mmse, kl_divergence
    optimization_level=3, # 0-3, default is 3
    quantize_input_node=True, # default is False
    # merge_dequant_layer_and_output_node=False, # default is False
    mmse_epoch=3, # default is 3
)
RKNN_DATASET = "/tmp/quantization_dataset.txt"
# MODEL_NAME="CrowdHuman"
PRE_COMPILE=True


# Preparation
files = os.listdir("/dataset/train/images")
files = [ os.path.join("/dataset/train/images", it) for it in files ]
files = files[:1000]
with open(RKNN_DATASET, "w") as f:
    f.write("\n".join(files))
os.system("mkdir -p /weight/output_convert/")


def show_outputs(outputs):
    output = outputs[0][0]
    output_sorted = sorted(output, reverse=True)
    top5_str = 'chrodhuman\n-----TOP 5-----\n'
    for i in range(5):
        value = output_sorted[i]
        index = np.where(output == value)
        for j in range(len(index)):
            if (i + j) >= 5:
                break
            if value > 0:
                topi = '{}: {}\n'.format(index[j], value)
            else:
                topi = '-1: 0.0\n'
            top5_str += topi
    print(top5_str)


if __name__ == '__main__':

    print("=======")
    print(f"RKNN_CONFIG: {RKNN_CONFIG}")
    print(f"ONNX_MODEL: {ONNX_MODEL}")
    print(f"RKNN_MODEL: {RKNN_MODEL}")
    print("=======")

    # Create RKNN object
    rknn = RKNN()

    if not os.path.exists(ONNX_MODEL):
        print("No onnx file exists.")
        exit(1)
    
    # pre-process config
    print('--> config model')
    rknn.config(**RKNN_CONFIG)
    print('done')

    # Load onnx model
    print('--> Loading model')
    ret = rknn.load_onnx(model=ONNX_MODEL)
    if ret != 0:
        print('Load onnx failed!')
        exit(ret)
    print('done')

    # Build model
    print('--> Building model')
    ret = rknn.build(do_quantization=True, dataset=RKNN_DATASET, pre_compile=PRE_COMPILE)
    if ret != 0:
        print('Build crowdhuman failed!')
        exit(ret)

    # print('--> Hybrid quantize model step 1')
    # ret = rknn.hybrid_quantization_step1(dataset=RKNN_DATASET)
    # if ret != 0:
    #     print('Hybrid quantize step 2 crowdhuman failed!')
    #     exit(ret)

    # print('--> Analyze model accuracy')
    # with open(RKNN_DATASET, "r") as f:
    #     input_path = f.read().splitlines()[0]
    # with open(f"{RKNN_DATASET}.1.txt", 'w') as f:
    #     f.write(input_path)
    # ret = rknn.accuracy_analysis(f"{RKNN_DATASET}.1.txt")
    # if ret != 0:
    #     print('Analyze crowdhuman accuracy failed!')
    #     exit(ret)

    # print('--> Hybrid quantize model step 2')
    # ret = rknn.hybrid_quantization_step2(model_input=f"{MODEL_NAME}.json",
    #                                      data_input=f"{MODEL_NAME}.data",
    #                                      model_quantization_cfg=f"{MODEL_NAME}.quantization.cfg",
    #                                      dataset=RKNN_DATASET,
    #                                      pre_compile=True)
    # if ret != 0:
    #     print('Hybrid quantize step 2 crowdhuman failed!')
    #     exit(ret)

    # Export rknn model
    print('--> Export RKNN model')
    ret = rknn.export_rknn(RKNN_MODEL)
    if ret != 0:
        print('Export chrodhuman.rknn failed!')
        exit(ret)
    print('done')

    # if not PRE_COMPILE:
    #     # Set inputs
    #     img = cv2.imread('./model/test1.bmp')
    #     img = cv2.resize(img, (640, 640))
    #     img = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)

    #     # init runtime environment
    #     print('--> Init runtime environment')
    #     ret = rknn.init_runtime()
    #     if ret != 0:
    #         print('Init runtime environment failed')
    #         exit(ret)
    #     print('done')

    #     # Inference
    #     print('--> Running model')
    #     outputs = rknn.inference(inputs=[img])
    #     x = outputs[0]
    #     output = np.exp(x)/np.sum(np.exp(x))
    #     outputs = [output]
    #     show_outputs(outputs)
    #     print('done')

    rknn.release()

