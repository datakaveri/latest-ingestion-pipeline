

Setup Guide
----

This document contains the installation and configuration processes of the external modules of each Verticle in IUDX Latest Ingestion Pipeline Server.



Latest Ingestion Pipeline server connects with various external dependencies namely :

 - `PostgreSQL` : used to store the query related to token invalidations, unique attributes etc.
 - `RabbitMQ` : used to publish and subscribe different types of messages or events.
 - `Redis` : used to store the latest/current value of every resource Item or resource group.

## Setting up PostgreSQL 
-  Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/postgres) to setup PostgreSQL

> PostgresQL database should be configured with a RBAC user having CRUD privileges

In order to connect to the appropriate Postgres database, required information such as databaseIP,databasePort etc. should be updated in the PostgresVerticle and DataBrokerVerticle modules available in [config-example.json](example-configs/config-example.json).


**PostgresVerticle**
```
{
    "id": "iudx.ingestion.pipeline.postgres.PostgresVerticle",
    "verticleInstances": "<num-of-verticle-instances>",
    "databaseIp": "<database-ip>",
    "databasePort": "<port-number>",
    "databaseName": "<database-name>",
    "databaseUserName": "<username-for-psql>",
    "databasePassword": "<password-for-psql>",
    "poolSize": "<pool-size>"
}
```

#### Schemas for PostgreSQL tables used by Latest ingestion pipeline

> Every postgreSQL table also uses function & triggers to insert/update values in `created_at` and `modified_at` columns.
1. Creating `update_modified` function
```sql
CREATE
OR REPLACE
   FUNCTION update_modified () RETURNS TRIGGER AS $$
BEGIN NEW.modified_at = now ();
RETURN NEW;
END;
$$ language 'plpgsql';
```

2. Creating `created_at` function
```sql
CREATE
OR REPLACE
   FUNCTION update_created () RETURNS TRIGGER AS $$
BEGIN NEW.created_at = now ();
RETURN NEW;
END;
$$ language 'plpgsql';

```
3. `unique_attributes` Table schema

```sql
CREATE TABLE IF NOT EXISTS unique_attributes
(
   _id uuid DEFAULT uuid_generate_v4 () NOT NULL,
   resource_id varchar NOT NULL,
   unique_attribute varchar NOT NULL,
   created_at timestamp without time zone NOT NULL,
   modified_at timestamp without time zone NOT NULL,
   CONSTRAINT unique_attrib_pk PRIMARY KEY (_id),
   CONSTRAINT resource_id_unique UNIQUE (resource_id)
);
```

4. applying functions to table via SQL triggers

```sql
CREATE TRIGGER update_ua_created BEFORE INSERT ON unique_attributes FOR EACH ROW EXECUTE PROCEDURE update_created ();
CREATE TRIGGER update_ua_modified BEFORE INSERT OR UPDATE ON unique_attributes FOR EACH ROW EXECUTE PROCEDURE update_modified();
```

## Setting up RabbitMQ

- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/databroker) to setup RMQ.


In order to connect to the appropriate RabbitMQ instance, required information such as dataBrokerIP,dataBrokerPort etc. should be updated in the DataBrokerVerticle module available in [config-example.json](example-configs/config-example.json).

 **RabbitMQ Verticle**
```
{
    "id": "iudx.ingestion.pipeline.rabbitmq.RabbitMQVerticle",
    "verticleInstances": <num-of-verticle-instances>,
    "dataBrokerIP": <rabbit mq ip>,
    "dataBrokerPort": <port-number>,
    "dataBrokerVhost": <vHost-name>,
    "dataBrokerUserName": <username-for-rmq>,
    "dataBrokerPassword": <password-for-rmq>,
    "dataBrokerManagementPort": <management-port-number>,
    "connectionTimeout": <time-in-milliseconds>,
    "requestedHeartbeat": <time-in-seconds>,
    "handshakeTimeout": <time-in-milliseconds>,
    "requestedChannelMax": <num-of-max-channels>,
    "networkRecoveryInterval": <time-in-milliseconds>,
    "automaticRecoveryEnabled": "true"
}
```

In addition to above configs, Latest ingestion pipeline server also expects few `exchanges` and `queues` to be present in the rabbit mq.

##### Exchanges 
- processed-messages

##### Queues
- redis-latest
- lip-processed-messages
- lip-unique-attributes

an appropriate binding should also be done between exchanges and queues.


## Setting up Redis
- Refer to the docker files available [here](https://github.com/datakaveri/iudx-deployment/blob/master/Docker-Swarm-deployment/single-node/redis)

In order to connect to the appropriate Redis instance, required information such as redisHost,redisPort etc. should be updated in the LatestVerticle module available in [config-example.json](example-configs/config-example.json).

**RedisVerticle**
```
{
   "id": "iudx.resource.server.database.latest.LatestVerticle",
    "verticleInstances": <num-of-verticle-instances>,
    "redisMode": <mode>,
    "redisUsername": <username-for-redis>,
    "redisPassword": <password-for-redis>,
    "redisMaxPoolSize": <pool-size>,
    "redisMaxPoolWaiting": <max-pool-waiting>,
    "redisMaxWaitingHandlers": <max-waiting-handlers>,
    "redisPoolRecycleTimeout": <recycle-timeout-in milliseconds>,
    "redisHost": "localhost",
    "redisPort": <port-number>,
}
