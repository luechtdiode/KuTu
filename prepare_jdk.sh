#!/usr/bin/env bash

# https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.1%2B12
# https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.1%2B12/OpenJDK21U-jdk_x64_windows_hotspot_21.0.1_12.zip
export jremajor=21
export jreminor=0.4
export jreupdate=7
export jreversion="jdk-${jremajor}.${jreminor}+${jreupdate}"
export jrefversion="${jremajor}.${jreminor}_${jreupdate}"
export macispec="jdk_x64_mac_hotspot"
export macaspec="jdk_aarch64_mac_hotspot"
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

if [ ${OS} == "Windows_NT" ]
then
    curl -L $(makeLink $msspec "zip") -o "jdk-${OS}.zip"
    unzip -u "jdk-${OS}.zip" -d .  >/dev/null 2>&1
fi

if [ ${OS} == "Linux" ]
then
    curl -L $(makeLink $linspec "tar.gz") -o "jdk-${OS}.tar.gz"
    tar -xzf "jdk-${OS}.tar.gz" -C .  >/dev/null 2>&1
fi

if [ ${OS} == "macOS" ]
then
  echo "install jdk for ${OS}"
  if [[ ! -z $(uname -a | grep x86_64) ]]
  then
    link=$(makeLink $macispec "tar.gz")
  fi
  if [[ ! -z $(uname -a | grep arm64) ]]
  then
    link=$(makeLink $macaspec "tar.gz")
  fi
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
# echo "JAVA_HOME=${JAVA_HOME}" >> ~/.zshrc
# echo "JAVA_HOME=${JAVA_HOME}" >> ~/.bash_profile
# echo "JAVA_HOME=${JAVA_HOME}" >> ~/.bashrc

echo "JAVA_HOME after install jdk${jremajor} ${JAVA_HOME}"
echo "JRE_HOME after install jdk${jremajor} ${JRE_HOME}"

echo "javac version on path:"
javac -version
echo "mvn version on path:"
mvn -version

echo "install jdk for ${OS} finished"
