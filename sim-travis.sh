#!/usr/bin/env bash

export OS=Win64 #$(uname -s)

. ./prepare_jdk.sh

mvn clean install


rm -rf docker/libs
mkdir docker/libs
cp target/dependency/*.jar docker/libs/
cp target/*.jar docker/
rm docker/libs/javafx*.jar
rm docker/*-app.jar

docker build ./docker -t luechtdiode/kutuapp:test

# docker run -p 5757:5757 --name kutuapp_cont luechtdiode/kutuapp:test
