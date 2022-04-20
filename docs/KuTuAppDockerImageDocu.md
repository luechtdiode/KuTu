# How to use the Docker-Image of KuTuApp Server

The Dockerimage of KuTuApp can be downloaded by
```bash
docker pull luechtdiode/kutuapp
```

## Resource-Requirements
 
* 1Gi+ Memory
* 1Gi+ Disk-Space
* 0.2 CPU Cores

## Simple usage

The server listens on port 5757. You have to map this port for accessing
it outside of the container by `-p 5757:5757` for example.

Use this with the 'out of the box' docker-container by
```bash
docker run -p 5757:5757 --name kutuapp_cont luechtdiode/kutuapp
```
Now, you can browse to [http://localhost:5757/](http://localhost:5757/).

## Health-/Readyness-Check

At the moment, only the `/metrics` route could be used to check HTTP-Status 200.
There will be dedicated endpoints to check healthy and readyness in the future.

## Environment

For more production-like purposes, there are more options that can be configured.
Most of them can be set as `env`-variable starting the container.

| ENV Variable-Name | Default-Value | Description |
|-------------------|---------------|-------------|
| X_KUTU_SECRET | ""                | Optional. Keeps the private key stable. If not set, each time, a new volume is mouted, a new private secret will be generated. In this case the already stored access-keys `.at.<server>` in the users `kutuapp/data/<competition>`-folder becomes invalid |
| X_DB_CONFIG_NAME | "sqlite"       | Optional. Name of the database-configuration. The configuration has to be defined in the file `kutuapp.conf` which you can map via Config-Map or Volume-Mount to the path `/home/kutuapp`. There is one predefined configuration for PostgreSQL named `kutudb_pg`. |
| X_POSTGRES_HOST | ""              | Required, if You use the predefined PostgreSQL Configuration `X_DB_CONFIG_NAME=kutudb_pg`. Should be the hostname of the database-instance. |
| X_POSTGRES_USER | ""              | Required, if You use the predefined PostgreSQL Configuration `X_DB_CONFIG_NAME=kutudb_pg`. Should be the username to connect to the database-instance. |
| X_POSTGRES_PASSWORD | ""          | Required, if You use the predefined PostgreSQL Configuration `X_DB_CONFIG_NAME=kutudb_pg`. Should be the password to connect to the database-instance. |
| X_SMTP_HOST | "undefined"         | Optional. Hostname of the smtp-host. If defined, the EMail-Features are activated. |
| X_SMTP_PORT | 0                   | Optional. Port of the smtp-server. |
| X_SMTP_DOMAIN | "undefined"       | Optional. Domain name of the EMail-Provider. |
| X_SMTP_USERNAME | "undefined"     | Optional. Username to connect to the smtp-server. |
| X_SMTP_PASSWORD |"undefined"      | Optional. Password to connect to the smtp-server. |

### Storage / Volume mounts

Some competition-related Artefacts like competition-logo or scorelist-definitions can be uploaded as file to the Server.
With the prepackaged database-driver, the SQLite database is stored in the file-system of the container.
Also the private key to sign the Access-Tokens is stored in the file-system.
If no volume is mounted to the container, a `empty-dir` volume is used.
This leads to data-loss on every redeployment of the container.

**To keep this storage stable, you have to mount a volume to the container**.

| Mountpoint    | Purpose |
|---------------|---------|
| /kutuapp      | Location where readonly access is required. You can place here a custom `kutuapp.conf`. |
| /home/kutuapp | Location where read/write access is required, so that database and competition-files can be stored. |

### External Database

Out of the box, a SQLite Database is used. This scales well in small environments.
However, if a dedicated Database should be used, there are the following options:

1. Use the predefined PostgreSQL-Configuration.
2. Use a complete customized Database-Configuration.

Be aware, that in each scenario the expected Database-name is `kutuapp` and
there should be a schema named `kutu`.

#### Use the predefined PostgreSQL-Configuration

Set the Environment-Variable `X_DB_CONFIG_NAME=kutudb_pg` and define the
Host, User and Password Environment-Variable, described in the Environment-Section (see above).

#### Use a complete customized Database-Configuration

Place your own `kutuapp.conf` in the container, mounted at `/kutuapp/kutuapp.conf`.
In this configuration, you have to define a custom configuration-section with a custom name.
This configuration will be used, if the Environment-Variable `X_DB_CONFIG_NAME` points to that custom configuraiton-name.

The configuration-section should follow the scala-slick 3.1 connection-config specification 
*see [https://scala-slick.org/doc/3.1.0/database.html](https://scala-slick.org/doc/3.1.0/database.html)*. 
If other drivers are used than HikariCP, Postgres or SQLite, a additional custom Dockerimage should be build, where the required jar-files should be appended to the classpath, defined in the ENTRYPOINT's start-command.

### Monitoring-Feature

If the server is up and running, one can query the `/metrics`-route to get current metrics of the KuTuApp, usually scraped by Prometheus.

## Examples

### Docker

Starts the Container interactive (-it) with the directory `.server` mounted as volume at home/kutuapp.
You can stop and remove the Instance by `CTRL + C`.
In this example, the X_KUTU_SECRET environment-variable is explicitly defined.
You can access the server by [http://localhost:5757](http://localhost:5757).

```bash
docker run --rm -it \
  -p 5757:5757 \
  -v /$(pwd)/.server:/home/kutuapp \
  -e X_KUTU_SECRET=topsecretprivatekey \
  --name kutuapp_cont \
  luechtdiode/kutuapp
```

### Dockercompose

*see [../dockercompose/docker-compose.yaml](../dockercompose/docker-compose.yaml)*

Start in dockercompose -folder:
```bash
docker-compose up d
```

Stop and remove:
```bash
docker-compose down
```

### Kubernetes

*see [https://github.com/luechtdiode/mk8-argo/tree/master/kutuapp](https://github.com/luechtdiode/mk8-argo/tree/master/kutuapp)*