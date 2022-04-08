## Rabbitmq configuration
The server expects following queues and exchanges.
1. Exchange "vertx-rmq-redis-writer" bind to "vertx-rmq-redis-reader" queue using routing key "processed"
2. There exists queue called "redis-latest". The binding to resource group exchanges done during 
creation of adapter
