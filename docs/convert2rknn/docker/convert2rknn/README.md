# Convert2RKNN

## 说明

本代码中包含构建针对Rockchip修改的yolov8的模型转换的镜像的文件，数据集目录结构参考Dockerfile标准-\*.pdf文件。

本镜像可用于对Rockchip修改的yolov8输出的模型文件进行量化，转化为可用于RV1126等平台的rknn模型文件。镜像以`/weight/output`中的onnx文件为输入，将转换的模型输出到`/weight/output_convert`中。

## 构建

可通过在根目录（包含`scripts/`, `docker/`等的目录）中调用`scripts/docker`目录中的脚本`build_convert2rknn_image.sh`构建转换镜像。

## 运行

可通过在根目录（包含`scripts/`, `docker/`等的目录）中调用`scripts/docker`目录中的脚本`run_covnert2rknn_image.sh`运行转换镜像。

镜像需要输入如下环境变量：

| 环境变量 | 必填/可选 | 说明 |
| -- | -- | -- |
| INPUT_MODEL_NAME | 可选 | 字符串，模型文件名，默认值为'best.onnx'，输入的文件名为相对于'/weight/output'的相对路径 |
| OUTPUT_MODEL_NAME | 可选 | 字符串，模型文件名，默认值为'best.rknn'，输入的文件名为相对于'/weight/output_convert'的相对路径 |
| RKNN_QUANTIZED_DTYPE | 可选 | 字符串枚举，表示量化方式, 默认值为‘asymmetric_quantized-u8'，可接受的值包括‘asymmetric_quantized-u8','dynamic_fixed_point-i8','dynamic_fixed_point-i16’ |
| RKNN_QUANTIZED_IMG_NUM | 可选 | 整数，大于0，表示量化时的batch_size，默认值为'100' |
| REORDER_CHANNEL | 可选 | 字符串，表示输入图像的通道顺序，默认值为'RGB'，可接受的值包括'RGB','BGR' |
| RKNN_MEAN_VALUES | 可选 | 字符串，表示模型输入的每个通道的均值，推理时会对输入按如下公式处理`y=(x-mean)/std`，默认值为'0_0_0'，输入的字符串以'#'分割不同的输入，以'_'分割每一个输入的不同通道，如‘0 1 2#3 4’；具体参考rknn-toolkit文档。 |
| RKNN_STD_VALUES | 可选 | 字符串，表示模型输入的每个通道的标准差，推理时会对输入按如下公式处理`y=(x-mean)/std`，默认值为'0_0_0'，输入的字符串以'#'分割不同的输入，以'_'分割每一个输入的不同通道，如‘0 1 2#3 4’；具体参考rknn-toolkit文档。 |

同时需要挂载如下目录：

| 镜像内目录 | 说明 |
| -- | -- |
| /weight | 模型文件目录，具体参考`Dockerfile标准-*.pdf` |
| /dataset/train/images | 数据集目录，具体参考`Dockerfile标准-*.pdf` |

## 打包

打包Convert2RKNN部分代码可以使用`bash scripts/docker/pack_convert2rknn.sh`
