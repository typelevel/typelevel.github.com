FROM ruby:2.5

ENV SBT_VERSION 1.3.10

ENV LC_ALL C.UTF-8

RUN \
apt-get update && \
apt-get install openjdk-11-jdk-headless -y

RUN \
curl -L -o sbt.deb https://dl.bintray.com/sbt/debian/sbt-$SBT_VERSION.deb && \
dpkg -i sbt.deb && \
rm sbt.deb && \
apt-get install sbt -y && \
sbt sbtVersion

