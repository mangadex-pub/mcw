ARG JAVA_VERSION="23"
FROM amazoncorretto:${JAVA_VERSION}-headless AS hotspot

USER root
RUN mkdir -pv /opt/mcw/bin
COPY --chown=root:root --chmod=0644 target/mcw.jar /opt/mcw/bin/mcw.jar

RUN dnf update -y && \
    dnf install -y bind-utils shadow-utils util-linux && \
    dnf clean all && \
    rm -rf \
      /tmp/* \
      /var/cache \
      /var/log/* \
      /var/tmp/* && \
    dnf --version

RUN groupadd -r -g 1001 mcw && \
    useradd -M -r -u 1001 -g 1001 mcw
USER mcw
WORKDIR /tmp

RUN stat --format="%u:%g=%a" /opt/mcw/bin/mcw.jar | grep -E '^0:0=644$' >/dev/null && \
    java -jar /opt/mcw/bin/mcw.jar --version
ENTRYPOINT ["java", "-jar", "/opt/mcw/bin/mcw.jar"]

FROM ghcr.io/mangadex-pub/containers-base/rockylinux:9 AS graal

USER root
RUN mkdir -pv /opt/mcw/bin
COPY --chown=root:root --chmod=0755 target/mcw /opt/mcw/bin/mcw
ENV PATH="/opt/mcw/bin:$PATH"

RUN dnf update -y && \
    dnf install -y bind-utils shadow-utils util-linux && \
    dnf clean all && \
    rm -rf \
      /tmp/* \
      /var/cache \
      /var/log/* \
      /var/tmp/* && \
    dnf --version

RUN groupadd -r -g 1001 mcw && \
    useradd -M -r -u 1001 -g 1001 mcw
USER mcw
WORKDIR /tmp

RUN stat --format="%u:%g=%a" /opt/mcw/bin/mcw | grep -E '^0:0=755$' >/dev/null && \
    mcw --version

ENTRYPOINT ["mcw"]
