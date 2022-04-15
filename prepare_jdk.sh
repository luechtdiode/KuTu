#!/usr/bin/env bash

export jremajor=17
export jreversion="jdk-17.0.2+8"
export jrefversion="17.0.2_8"
export macspec="jdk_x64_mac_hotspot"
export linspec="jdk_x64_linux_hotspot"
export msspec="jdk_x64_windows_hotspot"

echo "$OS"
echo "JAVA_HOME before install jdk${jremajor} ${JAVA_HOME}"
echo "JRE_HOME before install jdk${jremajor} ${JRE_HOME}"

function makeLink {
  osspec=$1
  filespec=$2
  echo "https://github.com/adoptium/temurin${jremajor}-binaries/releases/download/${jreversion}/OpenJDK${jremajor}U-${osspec}_${jrefversion}.${filespec}"
}

# https://mail.openjdk.java.net/pipermail/openjfx-dev/2018-September/022500.html

if [ ${OS} == "Win64" ]
then
    curl -L $(makeLink $msspec "zip") -o "jdk-${OS}.zip"
    unzip -u "jdk-${OS}.zip" -d .  >/dev/null 2>&1
fi

if [ ${OS} == "MSYS_NT-10.0" ]
then
    curl -L $(makeLink $msspec "zip") -o "jdk-${OS}.zip"
    unzip -u "jdk-${OS}.zip" -d .  >/dev/null 2>&1
fi

if [ ${OS} == "Linux" ]
then
    curl -L $(makeLink $linspec "tar.gz") -o "jdk-${OS}.tar.gz"
    tar -xzf "jdk-${OS}.tar.gz" -C .  >/dev/null 2>&1
fi

if [ ${OS} == "Darwin" ]
then
    echo "install jdk for ${OS}"
    link=$(makeLink $macspec "tar.gz")
    echo $link
    curl -L "$link" -o "jdk-${OS}.tar.gz"
    tar -xzf "jdk-${OS}.tar.gz" -C .  >/dev/null 2>&1
    echo "download jdk for ${OS} finished"
    sudo rm -rf "/Library/Java/JavaVirtualMachines/${jreversion}.jdk"
    sudo mv "${jreversion}/" "/Library/Java/JavaVirtualMachines/${jreversion}.jdk"
    sudo ln -s "/Library/Java/JavaVirtualMachines/${jreversion}.jdk/Contents/Home" "${jreversion}"
fi

export JAVA_HOME="${PWD}/${jreversion}"
export JRE_HOME="${PWD}/${jreversion}"
export PATH=${JAVA_HOME}/bin:$PATH
echo "JAVA_HOME=${JAVA_HOME}" >> ~/.mavenrc
echo "JAVA_HOME=${JAVA_HOME}" >> ~/.zshrc
echo "JAVA_HOME=${JAVA_HOME}" >> ~/.bash_profile
echo "JAVA_HOME=${JAVA_HOME}" >> ~/.bashrc

echo "JAVA_HOME after install jdk${jremajor} ${JAVA_HOME}"
echo "JRE_HOME after install jdk${jremajor} ${JRE_HOME}"

echo "javac version on path:"
javac -version
echo "mvn version on path:"
mvn -version

echo "install jdk for ${OS} finished"
