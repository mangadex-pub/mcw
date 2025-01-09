FROM ghcr.io/mangadex-pub/jdk-maven:23-corretto AS hotspot-corretto

USER root
RUN mkdir -pv /opt/mcw
COPY target/mcw.jar /opt/mcw/mcw.jar

USER mangadex
RUN java -jar /opt/mcw/mcw.jar --version

FROM ghcr.io/mangadex-pub/containers-base/rockylinux:9 AS aot-glibc

USER root
RUN mkdir -pv /opt/mcw
COPY target/mcw /opt/mcw/mcw

USER mangadex
RUN /opt/mcw/mcw --version
