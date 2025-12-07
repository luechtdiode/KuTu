#!/usr/bin/env bash

export OS=Win64 #$(uname -s)
export CODECOV_TOKEN='d03b4d79-1273-44ff-9206-69b56dbc4058'

. ./prepare_jdk.sh

#mvn clean install package
sbt -v "clean; test"
sbt -v "packageApp"

#bash <(curl -s https://codecov.io/bash)

rm -rf docker/libs
mkdir docker/libs
cp target/dependency/*.jar docker/libs/
cp target/*.jar docker/
cp target/package/libs/*.jar docker/libs/
cp target/package/*.jar docker/
rm docker/libs/javafx*.jar

docker build ./docker -t luechtdiode/kutuapp:test

# docker run -p 5757:5757 --name kutuapp_cont luechtdiode/kutuapp:test
