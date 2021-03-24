#!/usr/bin/python3
# redis-client-writer.py

import asyncio
import json
from aio_pika import connect_robust, Message, IncomingMessage, AMQPException
import hashlib
import concurrent.futures
from rejson import Client, Path
import redis

global redis_client

def on_message(message: IncomingMessage):
    try:
        data_dict = json.loads(message.body.decode())
        # check if the packet={'key':[rg]<String>,'path_param':[_SHA1_attr/d]<String>,'data':[adapter packet]<JSON>} from the queue is empty
        if data_dict is None or not bool(data_dict):
            print('> data_dict is empty. data_dict---- '+str(data_dict))
            return 'failed'
        else: 
            # print('> redis_key: ' + data_dict['key'] + '\nredis_path_param: ' + data_dict['path_param'] + '\nadapter_data_packet: ' + data_dict['data'])
            # this needs to be tested
            exp_val = data_dict['data']['id'] + '_' + data_dict['data']['observationDateTime']
    except json.decoder.JSONDecodeError as json_error:
        print('> JsonDecodeError!!!!'+str(json_error))
        return 'failed'

    try:
        # check if redis already has existing data?
        chck_if_rg_exists = redis_client.jsonget(data_dict['key'],Path.rootPath())
        # (nil) - never seen packet -> None, Insert
        # {}, !exists
        # data, upsert
        # redis already has previous data
        if chck_if_rg_exists is not None:
            print('> ' + str(data_dict['key']) +  'exists.')
            print('> Upserting ' + str(data_dict['data']) +' at .'+ data_dict['path_param'])
            print('> Upsertion still in progress...')
            redis_client.jsonset(data_dict['key'], '.'+data_dict['key']+'.'+data_dict['path_param'], data_dict['data'])
            print('> Upsertion successful!')
            message.ack()
        # First time the ingestor receives a packet belonging to RG
        else:
            print('> RG='+ data_dict['key'] + ' is not present in Redis. Inserting RG with {} at root.')
            # create first entry in the redis server
            # origin = {rg: {SHA : {}}}
            origin = {data_dict['key']:{data_dict['path_param']:{}}} 
            redis_client.jsonset(data_dict['key'], Path.rootPath(), origin)
            print('> Insertion still in progress...')
            # insert data now
            # JSON.GET resource-group-key/redis-key .path_param (SHA1...)
            # JSON.GET resource-group .resource-group.SHA1.... {adapter_data}
            redis_client.jsonset(data_dict['key'], '.' + data_dict['key'] + '.' + data_dict['path_param'], data_dict['data'])
            print('> Insertion successful!')
            message.ack() 

    except redis.exceptions.ResponseError as r_error:
        print('> Response Error from Redis!!!! '+str(r_error))
        return 'failed'



async def main_loop(loop):
    global redis_client
    try:
        # Connect to Redis
        # retrieve config using docker configs
        redis_client = Client(host='0.0.0.0', port=28734, decode_responses=True)

        # Connect to RabbitMQ
        # Can use connection pool instead of two separate clients
        # rmq_sub_client = await connect_robust(host='0.0.0.0',port=29042,login='redis-user',
        #                                               password='uv)aqcY]qSvARi74', virtualhost='IUDX',loop=loop
        # )
        rmq_sub_client = await connect_robust(host='0.0.0.0',port=29042,login='adapter-redis',
                                                       password='adapter-redis@123', virtualhost='IUDX',loop=loop
        )
    except AMQPException as e:
        print('> Connection Failed!')
        e.printStackTrace()
        return

    # redis_q_name = "redis-ingestion-queue"
    redis_q_name = "vertx-rmq-redis-reader"
    routing_key = "#"
    async with rmq_sub_client:
        channel = await rmq_sub_client.channel()
        queue_redis = await channel.declare_queue(redis_q_name, durable=True, arguments={'x-max-length': 20000})

        result = await queue_redis.consume(on_message)
        
        # Surround with a try catch block?
        # remove print and use logs instead
        if result is not None:
            if result == 'success':
                print('> Insertion was successful.')
            elif result == 'failed':
                print('> Insertion failed!')
        else:
            print('> Packet not found [or] Queue is empty!')


if __name__ == '__main__':
    print('> Running v0.0.1 Redis Client writer.')
    
    # create an event loop and keep it alive forever
    loop = asyncio.get_event_loop()
    loop.create_task(main_loop(loop))
    loop.run_forever()
