#!/usr/bin/python3

import asyncio
import websockets
import json
from rejson import Client, Path
attr_dict={}
async def receive(attr_dict):
    redis_path_param_default='_d'
    uri = "ws://tasks.logstash:3232"
    async with websockets.connect(uri,ping_interval=None) as websocket:
        data = await websocket.recv()
        print(f"> {data}")
        
        # Parse this message and extract the resource-group
        res_data=json.loads(data)
        res_id=res_data['id'].replace('/','__')
        print('> res_id-- '+res_id)
        rg=res_id.split('__')[3]
        print('> rg-- '+rg)
        rejson_key=rg
        print('> attr_dict-- '+str(attr_dict))

        # Check if the resource-group is present in the HashMap and it's attribute value
        if rg in attr_dict.keys():
            print('> true')
            attribute=attr_dict[rg]
            redis_path_param=res_id+'_'+res_data[attribute]
        else:
            redis_path_param=res_id+redis_path_param_default    
        
        # use the rejson module to insert the data into Redis db
        rj=Client(host='redis-server',port=6379,decode_responses=True)
        print('> redis_path_param-- '+redis_path_param)
        redis_path_param=redis_path_param.replace('.','$')
        redis_path_param=redis_path_param.replace('-','_')
        #rj.jsonset(rejson_key,Path.rootPath(),json.dumps({}))        
        rj.jsonset(rejson_key,Path('.'+redis_path_param),res_data)        


def load_hasmap():
    
    #load the hashmap using a config file
    with open('attribute_list.json') as attr_json_f:
        attr_dict=json.load(attr_json_f)
    print(attr_dict)
    

#if '__name__' == '__main__':
print('Redis client is running')
load_hasmap()
asyncio.get_event_loop().run_until_complete(receive(attr_dict))
asyncio.get_event_loop().run_forever()
