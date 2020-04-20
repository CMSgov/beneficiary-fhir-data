import boto3
import json
import random
import time
from datetime import datetime 

stores = [
  {'store': 'PHX1', 'state': 'AZ'},
  {'store': 'PHX2', 'state': 'AZ'},
  {'store': 'MESA1', 'state': 'AZ'},
  {'store': 'LA1', 'state': 'CA'},
  {'store': 'LA2', 'state': 'CA'},
  {'store': 'SF1', 'state': 'CA'},  
  {'store': 'SM1', 'state': 'CA'},  
  {'store': 'SD1', 'state': 'CA'},  
]

for i in range(100):
  store = random.choice(range(0, len(stores)))
  purchase = {
    'timestamp': datetime.utcnow().isoformat(),
    'store': stores[store]['store'],
    'state': stores[store]['state'],
    'hamburger': random.choice(range(0,3)),
    'hot_dog': random.choice(range(0,3)),
    'curly_fries': random.choice(range(0,3)),
    'coke': random.choice(range(0,2)),
    'shake': random.choice(range(0,2))
  }

  print(json.dumps(purchase))
  time.sleep(random.choice([0.11,0.21,0.52,0.61,0.92]))




