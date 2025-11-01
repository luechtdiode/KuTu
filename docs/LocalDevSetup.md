# How to setup local development environment

## Prerequisites

* JDK 25
* Working Maven (3.9+) installation,
* Git-Client
* IDE (IntelliJ, vscode, eclipse, ...)
* Node (v12.22+)
    ```bash
    curl -fsSL https://fnm.vercel.app/install | bash
    source /home/roland/.bashrc
    fnm install v12.22
    node -v
    ```

* NPM (6.14+)
    ```bash
    sudo apt install npm
    ```
* Docker for Desktop (optional)

## Step by step instructions

### Clone this repo into the workspace

```bash
git clone https://github.com/luechtdiode/KuTu.git
cd KuTu
```

### Prepare jdk (optional)

This script installs the required JDK 21

```bash
# Set the os-variable. Should be one of [Darwin, Win64, Linux]
OS=<your OS>
./prepare_jdk.sh
```

### Build the client
```bash
cd newclient/resultcatcher
npm install -g @angular/cli
npm install -g cordova @ionic/cli
npm i
ionic build --prod --engine=browser
cd -
```

### Start client development
One can run the client against the test-server `https://test-kutuapp.sharevic.net`
or against a local running server-instance `http://localhost:5757`.
You can specify the remote-address in the file `newclient/resultcatcher/src/proxy.conf.json`.

```bash
cd newclient/resultcatcher
ionic serve
cd -
```

### Copy the client-artefacts into the backend (optional)

```bash
rm -rf ./src/main/resources/app
mkdir ./src/main/resources/app
cp -R -f ./newclient/resultcatcher/www/* ./src/main/resources/app
```

### Build the backend

```bash
mvn clean install
```

### Develop/Debug with the backend

There are two IntelliJ runconfigurations prepared to use:
* `.run/KuTuApp-Client.run.xml`
* `.run/KuTuApp-Server.run.xml`

Other IDE's could be used as well, but then, you have to configure yourself.

The important thing is, that each instance gets its own `kutuapp.conf`-file.
This guarantees that they don't share the same database.

See the preconfigured files in
* `.client/kutuapp.conf`
* `.server/kutuapp.conf`

### Build the distribution

```bash
mvn package
```

#### Java-Client

The java-client is built using jpackage. It's in the `target/$OS` directory after `mvn package`.

#### Docker-Image

The docker-image is build by the local Docker for desktop Installation running
the following command (after `mvn clean install`):

```bash
rm -rf docker/libs
mkdir docker/libs
cp target/dependency/*.jar docker/libs/
cp target/*.jar docker/
rm docker/libs/javafx*.jar

docker build ./docker -t luechtdiode/kutuapp:test
```

Then you can start the docker-container:

```bash
docker run -p 5757:5757 --name kutuapp_cont luechtdiode/kutuapp:test
```

### Simulate github-action ci-pipeline (java-part)

The script `sim-cibuild.sh` aims the core-buildprocess, with the following exceptions:
* without client-build (npm/ionic). The js-binaries are already in the source of the backend-app integrated.
* without push to any registry.
* build binary-package just for the host-os.

```bash
./sim-cibuild.sh
```
