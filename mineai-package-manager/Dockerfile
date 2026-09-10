# 使用 Ubuntu 20.04 作为基础镜像
FROM docker.8877777.xyz/ubuntu:20.04

# 设置环境变量
ENV PARAMS=""

RUN apt-get update && \
    apt-get install -y openjdk-8-jdk firejail firejail-profiles unzip zip tar python3=3.8.* && \
    rm -rf /var/lib/apt/lists/*

# 链接python3到python
RUN ln -s /usr/bin/python3 /usr/bin/python

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo 'Asia/Shanghai' > /etc/timezone

# 将应用程序的 JAR 文件复制到容器中
COPY target/*.jar /app.jar

# 设置容器启动时的命令
ENTRYPOINT ["/bin/sh", "-c", "java -Dfile.encoding=utf8 -Djava.security.egd=file:/dev/./urandom -jar app.jar ${PARAMS}"]
