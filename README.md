![IUDX](./images/iudx.png)
# Latest Ingestion Pipeline

The new latest ingestion pipeline is designed to ingest data asynchronously into Redis Database. This pipeline would enable the [IUDX Resource Server](https://github.com/datakaveri/iudx-resource-server)  to serve latest data for IUDX specified resources that are available in the Database.

## Design Implementation for IUDX release v2:

The implementation for retrieving latest data for IUDX resources in IUDX v2 gurantees performant retrieval of latest data from ElasticSearch Database which otherwise involved large computational overhead of sorting directly on the database (IUDX v1). This was not a desirable trait as the dataset became increasingly huge, sorting incurred huge latency in response.

The implementation consists of 3 main components-
1. Rabbit MQ
2. Logstash
3. ElasticSearch

Logstash pushes the data subscribed through RabbitMQ (hereafter referred as RMQ) data broker using RabbitMQ plugin into a separate ElaticSearch (hereafter referred as ES) Index- *Latest*. The Latest Index contains one document per IUDX resource ID. The data is always upserted, i.e. if the resource is not present in the index, it is inserted or otherwise updated. The *document-id* for ES has been chosen to be the SHA-1 value of the resource-id to enable upsertion. Therefore, when the Latest API is triggered in the Resource Server, there is only one document available in the Latest Index which can be easily retrieved using the ES *REST GET API*. No "search" is required thus ensuring faster retrieval of data. To ensure data consistency, we used *version* feature in ES. The version for each individual document was chosen to be the TimeStamp at which the data was published, thereby the document is never updated if it's not recent data. This also ensures that any out-of-order data packets arriving at RMQ does not lead to inconsistent data. 

### Motivation for new design

It was then observed that the index size for *Latest* is growing abnormally because of frequent updates. Since the data from all the IUDX resources were smushed into one index, the update rate shot upto hundreds of thousands per second due to which the index size was growing abnormally. This was attributed to the underlying implementation of ES. It was a classic trade off between indexing and querying speed. Although a manual flush or reducing the refresh rate (which was already at 1 second) would have aided. 

To get rid of this manual process of flushing whenever the index size was growing tremendously, we decided to move to a new caching mechanism using [Redis](https://redis.io).

We tried a few design and pro-con'ed which suited the best for our use case. Eventually, we decided to write our own ingestion pipeline.

## RMQ Logstash-websocket Redis Plugin

The implementation consists of 5 main components-

1. RabbitMQ
2. Logstash
3. Websocket server
4. Python Client
5. Redis

### Drawbacks

- The Redis input plugin for Logstash did not support any other data structures except list which is why a simple Websocket plugin posed a suitable candidate to pull the data from Logstash and ingest into Redis.
- The system although very simple in both design and implementation introduced huge latency problems. The end-to-end ingestion duration for just one packet from RMQ to Redis Database took around 30 seconds which was not desired.This was due to the fact that there were multiple hops between individual modules [RMQ -> Logstash -> Websocket Server -> Python Client -> Redis Database]. 
- It was synchronous.

This led to moving into a asynchronous event loop based system which is discussed below.

## Asynchronous using Aio_pika

The implementation consists of 3 main components-

1. RabbitMQ
2. Redis Client written in Python using Aio_pika <insert link>
3. Redis

## Asynchronous using Vertx

The implementation consists of 3 main components-

1. RabbitMQ
2. Redis Client written in Java using Vertx <insert link> and JReJSON <insert link>
3. Redis

A detailed explanation of the implementation can be found [here]("./vertx/README.md").

## Reactive Pattern using aio_pika 

The design is similar to the async Python + Aio_pika but the implemenation it follows the reactive pattern where we isolate and parallelize the IO and CPU bound computations. 

## Contributing
We follow Git Merge based workflow 
1. Fork this repo
2. Create a new feature branch in your fork. Multiple features must have a hyphen separated name, or refer to a milestone name as mentioned in Github -> Projects 
3. Commit to your fork and raise a Pull Request with upstream
4. If you find any issues with the implementation please raise an issue on Issues in GitHub.

## License
[MIT](./LICENSE.txt)






