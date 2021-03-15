#FROM cantara/alpine-openjdk-jdk8
FROM anapsix/alpine-java:8u201b09_server-jre_nashorn

MAINTAINER jzmiao@easipass.com

ADD nacosgateway.jar /server/nacosgateway.jar
ADD start.sh /server/start.sh
RUN [ "chmod", "777", "/server/start.sh" ]
RUN echo "Asia/Shanghai" > /etc/timezone
ENTRYPOINT ["/server/start.sh"]