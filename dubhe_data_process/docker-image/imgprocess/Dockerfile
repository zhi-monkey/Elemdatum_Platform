
# 使用 Ubuntu 18.04 LTS 作为基础镜像
FROM docker.8877777.xyz/ubuntu:18.04

# 设置环境变量，避免在配置过程中出现交互式提示
ENV DEBIAN_FRONTEND=noninteractive

# 更新包列表并安装必要的软件包
RUN apt-get update && apt-get install -y --no-install-recommends \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
    python3.7 \
    python3-pip \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# 设置 Python 3.7 为默认的 Python 版本
RUN update-alternatives --install /usr/bin/python3 python3 /usr/bin/python3.7

# 升级 pip
RUN python3 -m pip install --upgrade pip

# 安装额外的 Python 包
RUN python3 -m pip install opencv-python-headless numpy pytest-shutil redis albumentations

# 设置环境变量
ENV LANG=C.UTF-8

# 打印成功消息
RUN echo 'success!!!'



## README中要求的是3.7, 后续出问题注意一下
#FROM python:3.6.3
#
#
#MAINTAINER root
#
#RUN cp /etc/apt/sources.list /etc/apt/sources.list.bak
#
#ADD sources.list /etc/apt/
#
## 安装基础包
#
#RUN DEBIAN_FRONTEND=noninteractive apt-get update
#
#ENV LANG C.UTF-8
#
#RUN pip install --user -i https://mirrors.cloud.tencent.com/pypi/simple/ --upgrade pip
#
#RUN pip install --user -i https://mirrors.cloud.tencent.com/pypi/simple/  opencv-python-headless
#
#RUN pip install --user -i https://mirrors.cloud.tencent.com/pypi/simple/  numpy
#
#RUN pip install --user -i https://mirrors.cloud.tencent.com/pypi/simple/  pytest-shutil
#
#RUN pip install --user -i https://mirrors.cloud.tencent.com/pypi/simple/  redis
#
#RUN echo 'success!!!'