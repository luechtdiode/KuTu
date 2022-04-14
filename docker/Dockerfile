FROM eclipse-temurin:17-jre

# Create kutu user and group
RUN echo "kutu:x:1497:1495:user for kutu:/home:/bin/false" >> /etc/passwd
RUN echo "kutu:!:1495:" >> /etc/group

WORKDIR /kutuapp
RUN chown kutu:kutu /kutuapp
RUN mkdir /home/kutuapp
RUN chown kutu:kutu /home/kutuapp
RUN chmod -R g+rwx /home/kutuapp

COPY --chown=kutu:kutu libs /kutuapp/libs
COPY --chown=kutu:kutu *.conf /kutuapp/
COPY --chown=kutu:kutu *.jar /kutuapp/app.jar

USER kutu

EXPOSE 5757

ENV X_KUTU_SECRET ""
ENV X_DB_CONFIG_NAME "sqlite"
ENV X_POSTGRES_HOST ""
ENV X_POSTGRES_USER ""
ENV X_POSTGRES_PASSWORD ""

ENV X_SMTP_HOST "undefined"
ENV X_SMTP_PORT 0
ENV X_SMTP_USERNAME "undefined"
ENV X_SMTP_DOMAIN "undefined"
ENV X_SMTP_PASSWORD "undefined"

ENTRYPOINT [ "java", "-cp", ".:app.jar:libs/*" \
    , "-Duser.home=/home" \
    , "-server" \
    , "--add-opens=java.base/java.io=ALL-UNNAMED" \
    , "--add-opens=java.base/java.util=ALL-UNNAMED" \
    , "--add-opens java.base/java.lang=ALL-UNNAMED" \
    , "--add-opens java.base/java.lang.invoke=ALL-UNNAMED" \
    , "--add-opens=java.base/java.net=ALL-UNNAMED" \
    , "--add-opens=java.base/java.nio=ALL-UNNAMED" \
    , "--add-opens=java.base/java.time=ALL-UNNAMED" \
    , "--add-opens=java.base/java.util=ALL-UNNAMED" \
    , "--add-opens java.base/java.util.concurrent.atomic=ALL-UNNAMED" \
    , "--add-opens=java.base/sun.nio.ch=ALL-UNNAMED" \
    , "--add-opens=java.base/sun.util.calendar=ALL-UNNAMED" \
    , "-XX:+UseG1GC" \
    , "-XX:+UseContainerSupport" \
    , "-XX:MaxGCPauseMillis=30" \
    , "-XX:InitiatingHeapOccupancyPercent=70" \
    , "-XX:MaxRAMPercentage=80" \
    , "-XX:+ParallelRefProcEnabled" \
    , "-XX:+PerfDisableSharedMem" \
    , "-XX:+OptimizeStringConcat" \
    , "-XX:+HeapDumpOnOutOfMemoryError" \
    , "-XX:MetaspaceSize=96M" \
    , "ch.seidel.kutu.KuTuServer"]
