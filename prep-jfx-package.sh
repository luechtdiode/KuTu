#!/usr/bin/env bash

rm -rf target/package
mkdir target/package
cp -r target/dependency target/package/libs
cp target/*.jar target/package/
