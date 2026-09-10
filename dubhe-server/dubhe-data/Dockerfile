FROM openjdk:8-jdk-alpine

#docker run -e PARAMS="--server.port 9090"
ENV PARAMS=""
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone

# 安装FFmpeg系统库（JavaCV需要）
# Alpine Linux中ffmpeg包已包含所有必要的依赖库（libavcodec, libavformat, libavutil等）
RUN apk add --no-cache \
    ffmpeg \
    && rm -rf /var/cache/apk/*

COPY target/*-exec.jar /app.jar

#
ENTRYPOINT ["/bin/sh","-c","java -Dfile.encoding=utf8 -Djava.security.egd=file:/dev/./urandom -jar app.jar --spring.profiles.active=prod ${PARAMS}"]