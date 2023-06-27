# iudx-latest-ingestion-pipeline

The latest ingestion pipeline is designed to ingest data asynchronously into Redis Database. This pipeline would enable the IUDX Resource Server to serve latest data for IUDX specified resources that are available in the Database.

<p align="center">
<img src="docs/lip_server_overview.png">
</p>


## Prerequisites

### External dependencies installation

The Latest ingestion pipeline connects to various external dependencies namely
 - Postgres SQL
 - RabbitMQ
 - Redis

Find the installations of the above along with the configurations to modify the database url, port and associated credentials in the appropriate sections [here](SETUP.md)

## Get started

### Make configuration
1. Clone this repo and change directory:
   ```sh 
   git clone https://github.com/datakaveri/latest-ingestion-pipeline.git && cd latest-ingestion-pipeline/vertx/
   ```
2. Make a config file based on the template in `example-configs/config-dev.json` for non-clustered vertx and  `example-configs/config-depl.json` for clustered vertx.
   - Modify the databroker url and associated credentials in the appropriate sections
   - Populate secrets directory with following structure in the present directory:
      ```sh
      secrets/
      ├── all-verticles-configs/ (directory)
      │   ├── config-depl.json (needed for clustered vertx all verticles  in one container)
      │   ├── config-dev.json (needed for non-clustered vertx all verticles in one container/maven based setup)
      │  
      └── one-verticle-configs/ (directory, needed for clustered vertx in multi-container)
      ``` 
3. Populate `.lip.env` environment file based on template in `example-configs/example-evironment-file(.lip.env)` in the present directory
#### Note
1. DO NOT ADD actual config with credentials to `example-configs/` directory (even in your local git clone!). 
2. If you would like to add your own config with different name than config-dev.json and config-depl.json, place in the `secrets/all-verticles-configs/` and follow the note sections of docker based and maven based setup.
3. Update all appropriate configs in `example-configs/` ONLY when there is addition of new config parameter options.
### Docker based
1. Install docker and docker compose (one time setup)
2. Build the images 
   ```sh
    ./docker/build.sh
    ```
3. There are following two ways of setting/deploying the file server using docker compose:
   1. Non-Clustered setup with all verticles running in a single container: 
      - This needs no hazelcast, zookeeper, the deployment can be done on non-swarm too and suitable for development environment.
      - This makes use of iudx/lip-dev:latest image and config-dev.json present at `secrets/all-verticles-configs/config-dev.json`
         ```sh 
         # Command to bring up the non-clustered latest-ingestion-pipeline container
         docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
         # Command to bring down the non-clustered latest-ingestion-pipeline container
         docker compose -f docker-compose.yml -f docker-compose.dev.yml down
         ```
   2. Clustered setup with all verticles running in a single container: 
      - This needs following things:
         - Docker swarm and overlay network having a name 'overlay-net'. Refer [here](https://github.com/datakaveri/iudx-deployment/tree/master/docs/swarm-setup.md)
         - Zookeeper running in 'overlay-net' named overlay network. Refer [here](https://github.com/datakaveri/iudx-deployment/tree/master/single-node/zookeeper)
      - This makes use of iudx/lip-depl:latest image and config-depl.json present at `secrets/all-verticles-configs/config-depl.json`
         ```sh 
         # Command to bring up the clustered one latest-ingestion-pipeline container
         docker compose -f docker-compose.yml -f docker-compose.depl.yml up -d
         # Command to bring down the clustered one latest-ingestion-pipeline container
         docker compose -f docker-compose.yml -f docker-compose.depl.yml down
         ```
#### Note   
1. If you want to try out or do temporary things, such as 
   - use different config file than the standard two
   - Binding the ports of clustered latest-ingestion-pipeline container to host, etc.<br>
   Please use [this](readme/multiple-compose-files.md) technique of overriding/merging compose files i.e. using non-git versioned docker-compose.temp.yml file and do not modify the git-versioned files.
2. Modify the git versioned compose files ONLY when the configuration is needed by all (or its for CI - can preferably name it as docker-compose.ci.yml) and commit and push to the repo.


### Maven based
1. Install jdk 11 and maven
2. Use the maven exec plugin based starter to start the server 
   ```sh 
   mvn clean compile exec:java@ingestion-pipeline
   ```
#### Note
1. Maven based setup by default uses `secrets/all-verticles-configs/config-dev.json` and is non-clustered setup of verticles. Also it cannot take values from `.lip.env` file and so the default values apply.
2. If you want to use a different named config called `config-x.json`, need to place it at `secrets/all-verticles-configs/config-x.json` and use following command to bring it up:
   ```sh
   mvn clean compile  exec:java@ingestion-pipeline -Dconfig-dev.file=config-x.json
   ```
