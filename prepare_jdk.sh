#!/usr/bin/env bash

export jremajor=11
export jreversion="jdk-11.0.6+10"
export jrefversion="11.0.6_10"

echo "JAVA_HOME before install jdk${jremajor} ${JAVA_HOME}"
echo "JRE_HOME before install jdk${jremajor} ${JRE_HOME}"

# https://mail.openjdk.java.net/pipermail/openjfx-dev/2018-September/022500.html

if [ ${OS} == 'Win64' ]
then
    curl -L "https://github.com/AdoptOpenJDK/openjdk${jremajor}-binaries/releases/download/${jreversion}/OpenJDK${jremajor}U-jdk_x64_windows_hotspot_${jrefversion}.zip" -o "jdk-${OS}.zip"
    unzip -u "jdk-${OS}.zip" -d .  >/dev/null 2>&1
    curl -L http://download2.gluonhq.com/jpackager/11/jdk.packager-windows.zip -o "javapackager-${OS}.zip"
    unzip -u "javapackager-${OS}.zip" -d "${jreversion}/bin"  >/dev/null 2>&1
    cp "${jreversion}/bin/jdk.packager.jar" "${jreversion}/jmods"
fi

if [ ${OS} == 'MSYS_NT-10.0' ]
then
    curl -L "https://github.com/AdoptOpenJDK/openjdk${jremajor}-binaries/releases/download/${jreversion}/OpenJDK${jremajor}U-jdk_x64_windows_hotspot_${jrefversion}.zip" -o "jdk-${OS}.zip"
    unzip -u "jdk-${OS}.zip" -d .  >/dev/null 2>&1
    curl -L http://download2.gluonhq.com/jpackager/11/jdk.packager-windows.zip -o "javapackager-${OS}.zip"
    unzip -u "javapackager-${OS}.zip" -d "${jreversion}/bin"  >/dev/null 2>&1
    cp "${jreversion}/bin/jdk.packager.jar" "${jreversion}/jmods"
fi

if [ ${OS} == 'Linux' ]
then
    curl -L "https://github.com/AdoptOpenJDK/openjdk${jremajor}-binaries/releases/download/${jreversion}/OpenJDK${jremajor}U-jdk_x64_linux_hotspot_${jrefversion}.tar.gz" -o "jdk-${OS}.tar.gz"
    tar -xzf "jdk-${OS}.tar.gz" -C .  >/dev/null 2>&1
    curl -L http://download2.gluonhq.com/jpackager/11/jdk.packager-linux.zip -o "javapackager-${OS}.zip"
    unzip -u "javapackager-${OS}.zip" -d "${jreversion}/bin"  >/dev/null 2>&1
    cp "${jreversion}/bin/jdk.packager.jar" "${jreversion}/jmods"
fi

if [ ${OS} == 'Darwin' ]
then
    echo "install jdk for ${OS}"
    curl -L "https://github.com/AdoptOpenJDK/openjdk${jremajor}-binaries/releases/download/${jreversion}/OpenJDK13${jremajor}U-jdk_x64_mac_hotspot_${jrefversion}.tar.gz" -o "jdk-${OS}.tar.gz"
    tar -xzf "jdk-${OS}.tar.gz" -C .  >/dev/null 2>&1
    curl -L http://download2.gluonhq.com/jpackager/11/jdk.packager-osx.zip -o "javapackager-${OS}.zip"
    echo "download jdk for ${OS} finished"
    unzip -u "javapackager-${OS}.zip" -d "${jreversion}/Contents/Home/bin"  >/dev/null 2>&1
    cp "${jreversion}/Contents/Home/bin/jdk.packager.jar" "${jreversion}/Contents/Home/jmods"
    sudo rm -rf "/Library/Java/JavaVirtualMachines/${jreversion}.jdk"
    sudo mv "${jreversion}/*/" "/Library/Java/JavaVirtualMachines/${jreversion}.jdk"
    sudo ln -s "/Library/Java/JavaVirtualMachines/${jreversion}.jdk/Contents/Home" "jdk${jremajor}"
fi

export JAVA_HOME="${PWD}/${jreversion}"
export JRE_HOME="${PWD}/${jreversion}"
export PATH=${JAVA_HOME}/bin:$PATH
echo "JAVA_HOME=${JAVA_HOME}" >> ~/.mavenrc

echo "JAVA_HOME after install jdk${jremajor} ${JAVA_HOME}"
echo "JRE_HOME after install jdk${jremajor} ${JRE_HOME}"

echo "javac version on path:"
javac -version
echo "mvn version on path:"
mvn -version

echo "install jdk for ${OS} finished"
