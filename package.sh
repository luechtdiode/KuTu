#!/usr/bin/env bash

rm -rf target/package
mkdir target/package
cp -r target/dependency target/package/libs
cp target/*.jar target/package/

$JAVA_HOME/bin/jpackage --input target/package/ \
  --name "TurnerWettkampf-App" \
  --main-jar KuTu-2.2.13.jar \
  --main-class ch.seidel.kutu.KuTuApp \
  --copyright "Interpolar" \
  --vendor "Interpolar" \
  --app-version 2.2.13 \
  --type dmg \
  --dest target \
  --java-options "--add-opens=java.base/java.lang=ALL-UNNAMED
                  --add-opens=java.base/java.lang.invoke=ALL-UNNAMED
                  --add-opens=java.base/java.net=ALL-UNNAMED
                  --add-opens=java.base/java.nio=ALL-UNNAMED
                  --add-opens=java.base/java.time=ALL-UNNAMED
                  --add-opens=java.base/java.util=ALL-UNNAMED
                  --add-opens=java.base/java.util.concurrent.atomic=ALL-UNNAMED
                  --add-opens=java.base/sun.nio.ch=ALL-UNNAMED
                  --add-opens=java.base/sun.util.calendar=ALL-UNNAMED"
