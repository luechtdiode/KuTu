#!/usr/bin/env bash

echo "JAVA_HOME before install jdk11 ${JAVA_HOME}"
echo "JRE_HOME before install jdk11 ${JRE_HOME}"
mvn -version

rm -rf jdk
rm -rf jdk11
mkdir jdk

# https://mail.openjdk.java.net/pipermail/openjfx-dev/2018-September/022500.html

if [ ${OS} == 'Win64' ]
then
    curl -L https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.3%2B7_openj9-0.14.0/OpenJDK11U-jdk_x64_windows_openj9_11.0.3_7_openj9-0.14.0.zip -o "jdk-${OS}.zip"
    unzip -u "jdk-${OS}.zip" -d jdk
    curl -L http://download2.gluonhq.com/jpackager/11/jdk.packager-windows.zip -o "javapackager-${OS}.zip"
    mv jdk/*/ jdk11/
    unzip -u "javapackager-${OS}.zip" -d jdk11/bin
    cp "${JAVA_HOME}/bin/jdk.packager.jar" jdk11/jmods
    export JAVA_HOME="${PWD}/jdk11"
    export JRE_HOME="${PWD}/jdk11"
    export PATH=${JAVA_HOME}/bin:$PATH
fi

if [ ${OS} == 'MSYS_NT-10.0' ]
then
    curl -L https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.3%2B7_openj9-0.14.0/OpenJDK11U-jdk_x64_windows_openj9_11.0.3_7_openj9-0.14.0.zip -o "jdk-${OS}.zip"
    unzip -u "jdk-${OS}.zip" -d jdk
    curl -L http://download2.gluonhq.com/jpackager/11/jdk.packager-windows.zip -o "javapackager-${OS}.zip"
    mv jdk/*/ jdk11/
    unzip -u "javapackager-${OS}.zip" -d jdk11/bin
    cp "${JAVA_HOME}/bin/jdk.packager.jar" jdk11/jmods
    export JAVA_HOME="${PWD}/jdk11"
    export JRE_HOME="${PWD}/jdk11"
    export PATH=${JAVA_HOME}/bin:$PATH
fi

if [ ${OS} == 'Linux' ]
then
    curl -L https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.3%2B7_openj9-0.14.0/OpenJDK11U-jdk_x64_linux_openj9_11.0.3_7_openj9-0.14.0.tar.gz -0 "jdk-${OS}.tar.gz"
    tar -xzf "jdk-${OS}.tar.gz" -C jdk
    curl -L http://download2.gluonhq.com/jpackager/11/jdk.packager-linux.zip -o "javapackager-${OS}.zip"
    mv jdk/*/ jdk11/
    unzip -u "javapackager-${OS}.zip" -d jdk11/bin
    cp "${JAVA_HOME}/bin/jdk.packager.jar" jdk11/jmods
    export JAVA_HOME="${PWD}/jdk11"
    export JRE_HOME="${PWD}/jdk11"
    export PATH=${JAVA_HOME}/bin:$PATH
fi

if [ ${OS} == 'Darwin' ]
then
    echo "install jdk11 for ${OS}"
    curl -L https://github.com/AdoptOpenJDK/openjdk11-binaries/releases/download/jdk-11.0.3%2B7_openj9-0.14.0/OpenJDK11U-jdk_x64_mac_openj9_11.0.3_7_openj9-0.14.0.tar.gz -o "jdk-${OS}.tar.gz"
    tar -xzf "jdk-${OS}.tar.gz" -C jdk
    curl -L http://download2.gluonhq.com/jpackager/11/jdk.packager-osx.zip -o "javapackager-${OS}.zip"
    echo "download jdk11 for ${OS} finished"
    unzip -u "javapackager-${OS}.zip" -d jdk/Contents/Home/bin
    cp "${JAVA_HOME}/bin/jdk.packager.jar" jdk/Contents/Home/jmods
    sudo mv jdk/*/ /Library/Java/JavaVirtualMachines/jdk-11.jdk
    sudo ln -s /Library/Java/JavaVirtualMachines/jdk-11.jdk/Contents/Home jdk11
    export JAVA_HOME="${PWD}/jdk11"
    export JRE_HOME="${PWD}/jdk11"
    export PATH=${JAVA_HOME}/bin:$PATH
fi

ls jdk11/bin/*

echo "JAVA_HOME=${JAVA_HOME}" >> ~/.mavenrc

echo "JAVA_HOME after install jdk11 ${JAVA_HOME}"
echo "JRE_HOME after install jdk11 ${JRE_HOME}"

${JAVA_HOME}/bin/javac -version
javac -version
mvn -version

echo "install jdk11 for ${OS} finished"
