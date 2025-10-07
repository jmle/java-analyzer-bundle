FROM registry.access.redhat.com/ubi9/ubi

RUN dnf module enable -y maven:3.8 \
 && dnf clean all \
 && dnf -y update \
 && dnf -y install maven-openjdk17 git \
 && dnf clean all

RUN mkdir -p unpack/index
RUN cd unpack/index \
 && curl -s -o nexus-maven-repository-index.gz https://repo.maven.apache.org/maven2/.index/nexus-maven-repository-index.gz

RUN cd ../.. \
 && mvn clean install

RUN java -jar ./target/export-index-1.0-SNAPSHOT.jar /unpack/index

RUN sort maven.default.index