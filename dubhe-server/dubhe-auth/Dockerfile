FROM openjdk:8-jdk-alpine

#docker run -e PARAMS="--server.port 9090"
ENV PARAMS=""
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone

COPY target/*-exec.jar /app.jar

#
ENTRYPOINT ["/bin/sh","-c","java -Dfile.encoding=utf8 -Djava.security.egd=file:/dev/./urandom -jar app.jar --spring.profiles.active=prod ${PARAMS}"]