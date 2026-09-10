FROM jadehh/rknn-toolkit:1.7.3

# 输入环境变量，需要在执行镜像之前配置
# 格式： ENV 超参名称 变量类型:取值范围:默认值:输入规范要求
#TARGET_PLATFORM 支持 rk1806,rk1808,rk3399pro,rv1126,rv1109
ENV CONVERT_TARGET_PLATFORM=enum:{rk1806,rk1808,rk3399pro,rv1126,rv1109}:rv1126
ENV CONVERT_INPUT_MODEL_NAME=string::best.onnx
ENV CONVERT_OUTPUT_MODEL_NAME=string::best.rknn
ENV CONVERT_RKNN_QUANTIZED_DTYPE=enum:{asymmetric_quantized-u8,dynamic_fixed_point-i8,dynamic_fixed_point-i16}:asymmetric_quantized-u8
ENV CONVERT_RKNN_QUANTIZED_IMG_NUM=numeric:[1,):100
ENV CONVERT_REORDER_CHANNEL=enum:{RGB,BGR}:RGB
ENV CONVERT_RKNN_MEAN_VALUES=string::0_0_0:输入将按如下公式处理'y=(x-MEAN)/STD'，以'#'分割不同的输入，以'_'分割每一个输入的不同通道，如'0_1_2#3_4'
ENV CONVERT_RKNN_STD_VALUES=string::255_255_255:输入将按如下公式处理'y=(x-MEAN)/STD'，以'#'分割不同的输入，以'_'分割每一个输入的不同通道，如'0_1_2#3_4'

# 本镜像中未使用
ENV INPUT_IMAGE_CHANNEL=3
ENV INPUT_IMAGE_SIZE=640
ENV MODEL_CLASS_NUM=80


RUN mkdir -p /workspace
COPY docker/convert2rknn/start.py /workspace

WORKDIR /workspace
CMD ["python", "start.py"]