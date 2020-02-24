#!/usr/bin/env bash

export OS=Win64 #$(uname -s)

. ./prepare_jdk.sh

mvn clean install

cp target/*-app.jar docker/

docker build ./docker -t luechtdiode/kutuapp:test

# docker run -p 5757:5757 --name kutuapp_cont luechtdiode/kutuapp:test
