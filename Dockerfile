FROM amazoncorretto:23-headless AS hotspot

USER root
RUN mkdir -pv /opt/mcw/bin
COPY target/mcw.jar /opt/mcw/bin/mcw.jar

RUN java -jar /opt/mcw/bin/mcw.jar --version

FROM ghcr.io/mangadex-pub/containers-base/rockylinux:9 AS graal

USER root
RUN mkdir -pv /opt/mcw/bin
COPY target/mcw /opt/mcw/mcw

RUN /opt/mcw/mcw --version
