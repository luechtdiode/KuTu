FROM openjdk:11-jre-slim

# Create kutu user and group
RUN echo "kutu:x:1497:1495:user for kutu:/home:/bin/false" >> /etc/passwd
RUN echo "kutu:!:1495:" >> /etc/group

WORKDIR /kutuapp
RUN chown kutu:kutu /kutuapp
RUN mkdir /home/kutuapp
RUN chown kutu:kutu /home/kutuapp
RUN chmod -R g+rwx /home/kutuapp

COPY --chown=kutu:kutu *.conf /kutuapp/
COPY --chown=kutu:kutu *-app.jar /kutuapp/app.jar

USER kutu

EXPOSE 5757

ENV X_DB_CONFIG_NAME "sqlite"
ENV X_POSTGRES_HOST ""
ENV X_POSTGRES_USER ""
ENV X_POSTGRES_PASSWORD ""

ENTRYPOINT [ "java", "-cp", ".:app.jar" \
    , "-Duser.home=/home" \
    , "-server" \
    , "-XX:+UseG1GC" \
    , "-XX:MaxGCPauseMillis=30" \
    , "-XX:G1HeapRegionSize=16m" \
    , "-XX:InitiatingHeapOccupancyPercent=75" \
    , "-XX:+ParallelRefProcEnabled" \
    , "-XX:+PerfDisableSharedMem" \
    , "-XX:+OptimizeStringConcat" \
    , "-XX:+HeapDumpOnOutOfMemoryError" \
    , "-Xms1000M" \
    , "-Xmx1000M" \
    , "-XX:MetaspaceSize=96M" \
    , "ch.seidel.kutu.KuTuServer"]
