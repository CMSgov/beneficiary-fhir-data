import boto3
import json
import random
import time
from datetime import datetime 
'''
Generator code for the food truck business. Randomly generate "purhcase" events and
place them into the food truck firehose. 

The foodtruck project is an example project with test data. 
'''

trucks = [
  {'truck': 'PHX1', 'state': 'AZ'},
  {'truck': 'PHX2', 'state': 'AZ'},
  {'truck': 'MESA1', 'state': 'AZ'},
  {'truck': 'LA1', 'state': 'CA'},
  {'truck': 'LA2', 'state': 'CA'},
  {'truck': 'SF1', 'state': 'CA'},  
  {'truck': 'SM1', 'state': 'CA'},  
  {'truck': 'SD1', 'state': 'CA'},  
]

firehose = boto3.client('firehose')

for i in range(100):
  truck = random.choice(range(0, len(trucks)))
  purchase = {
    'timestamp': datetime.utcnow().isoformat() + 'Z',
    'truck': trucks[truck]['truck'],
    'state': trucks[truck]['state'],
    'hamburgers': random.choice(range(0,3)),
    'hot_dogs': random.choice(range(0,3)),
    'curly_fries': random.choice(range(0,3)),
    'cokes': random.choice(range(0,2)),
    'shakes': random.choice(range(0,2))
  }
  firehose.put_record(DeliveryStreamName='bfd-insights-foodtruck-purchases', Record={'Data': json.dumps(purchase) + '\n'})
  print(json.dumps(purchase))
  
  time.sleep(random.choice([0.11,0.21,0.52,0.61,0.92]))




