#!/usr/bin/env bash

#rm -rf jdk
#rm -rf jdk11

# https://mail.openjdk.java.net/pipermail/openjfx-dev/2018-September/022500.html

if [ ${OS} == 'win64' ]
then
#    curl -L https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.1%2B13/OpenJDK11U-jdk_x64_windows_openj9_jdk-11.0.1_13_openj9-0.11.0_11.0.1_13.zip -o "jdk-win64.zip"
    curl -L http://download2.gluonhq.com/jpackager/11/jdk.packager-windows.zip -o "javapackager-win64.zip"
    curl -L https://github.com/wixtoolset/wix3/releases/download/wix3111rtm/wix311.exe -o "wix311.exe"
#    wix311.exe /install /quiet /norestart
fi

if [ ${OS} == 'linux' ]
then
#    curl -L https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.1%2B13/OpenJDK11U-jdk_x64_linux_openj9_jdk-11.0.1_13_openj9-0.11.0_11.0.1_13.tar.gz -0 "jdk-linux.zip"
    curl -L http://download2.gluonhq.com/jpackager/11/jdk.packager-linux.zip -o "javapackager-linux.zip"
fi

if [ ${OS} == 'Darwin' ]
then
#    curl -L https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.1%2B13/OpenJDK11U-jdk_x64_mac_openj9_jdk-11.0.1_13_openj9-0.11.0_11.0.1_13.tar.gz -o "jdk-Darwin.zip"
    curl -L http://download2.gluonhq.com/jpackager/11/jdk.packager-osx.zip -o "javapackager-Darwin.zip"
fi

#unzip -u "jdk-${OS}.zip" -d jdk
#
#mv jdk/*/ jdk11/
#
#unzip -u "javapackager-${OS}.zip" -d "jdk11/bin"
unzip -u "javapackager-${OS}.zip" -d "${JAVA_HOME}/bin"

cp "${JAVA_HOME}/bin/jdk.packager.jar" "${JAVA_HOME}/jmods"

#export JAVA_HOME="${PWD}/jdk11"
#
#export PATH=${JAVA_HOME}/bin:$PATH
#
#echo "${JAVA_HOME}"
