#!/usr/bin/env bash

export OS=$(uname -s)

. ./prepare_jdk.sh

mvn clean install