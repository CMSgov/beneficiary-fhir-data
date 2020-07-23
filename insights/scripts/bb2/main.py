import boto3
import json
import random
import time
from datetime import datetime, timedelta
from apps import App, Humana, Evidation
import events
import csv

"""
Synthentic generator code for a days worth of data
"""

apps = [(Humana(), range(0,60)), (Evidation(), range(55,85))]

benes = []
with open('benes.csv') as bene_file:
  reader = csv.DictReader(bene_file)
  for row in reader: 
    benes.append(row)

firehose = boto3.client('firehose')
start_time = datetime.utcnow()

sessions = []
for app in apps:
  for bene in benes:
    if int(bene['random']) in app[1]:
      session_time = start_time + timedelta(seconds=random.uniform(10.0, 10*60*60))
      session_messages = []
      for message in app[0].generate_session(session_time, bene):
        session_messages.append(message)
      sessions.append({"time": session_time, "messages": session_messages})

sessions.sort(key = lambda s: s["time"])

for session in sessions:
  for message in session["messages"]:
    event = {
      "instance_id": "i-000000000000000",
      "image_id": "ami-00000000000000",
      "component": "bb2.web",
      "vpc": "bluebutton-prod-sim",
      "log_name": "audit.hhs_oauth_server.request_logging",
      "message": message,
    }
    print(json.dumps(event))
    firehose.put_record(DeliveryStreamName='bfd-insights-bb2-events', Record={'Data': json.dumps(event) + '\n'})

