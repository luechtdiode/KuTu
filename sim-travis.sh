#!/usr/bin/env bash

export OS=$(uname -s)

. ./prepare_jdk.sh

mvn clean install

cp target/*-app.jar docker/

docker build ./docker -t luechtiode/kutuapp:test