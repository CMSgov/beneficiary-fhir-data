"""
This module contains all the sythentic event classes
"""
import json
import uuid
import random
from datetime import datetime, timedelta

def my_medicare_login(bene, app, start_time: datetime):
    # Example: {"env": "prod", "time": "2020-05-11 03:59:17", "level": "INFO", "name": "audit.hhs_oauth_server.request_logging", "message": {"start_time": 1589169557.025516, "end_time": 1589169557.062385, "request_uuid": "c8d93bf8-933b-11ea-8b95-1235012118b1", "path": "/mymedicare/login", "response_code": 302, "size": "", "location": "https://account.mymedicare.gov/?scope=openid%20profile&client_id=bluebutton&state=78252271507550671369463107&redirect_uri=https%3A//api.bluebutton.cms.gov/mymedicare/sls-callback", "app_name": "", "app_id": "", "dev_id": "", "dev_name": "", "access_token_hash": "", "user": "None", "ip_addr": "73.26.177.126"}}
    result = {
      "access_token_hash": "",
      "app_id": app.app_id(),
      "app_name":  app.app_name(),
      "dev_id":  app.dev_id(),
      "dev_name":  app.dev_name(),
      "end_time":  (start_time + timedelta(seconds=0.001)).timestamp(),
      "ip_addr":  ".".join(str(random.randint(0, 255)) for _ in range(4)),
      "location": "https://account.mymedicare.gov/?scope=openid%20profile&client_id=bluebutton&state=78252271507550671369463107&redirect_uri=https%3A//api.bluebutton.cms.gov/mymedicare/sls-callback",
      "path":  "/mymedicare/login",
      "request_uuid": str(uuid.uuid4()),
      "response_code":  302,
      "size":  "", 
      "start_time":  start_time.timestamp(),
      "user":  bene['sls_id'],
      "fhir_id": bene['fhir_id']
    }
    return json.dumps(result)

def token_response(bene, app, start_time: datetime, token_hash):
    result = {
      "access_token_hash": token_hash,
      "app_id": app.app_id(),
      "app_name":  app.app_name(),
      "dev_id":  app.dev_id(),
      "dev_name":  app.dev_name(),
      "end_time":  (start_time + timedelta(seconds=0.001)).timestamp(),
      "ip_addr":  ".".join(str(random.randint(0, 255)) for _ in range(4)),
      "location": "",
      "path":  "/v1/o/token/",
      "request_uuid": str(uuid.uuid4()),
      "response_code":  200,
      "size":  267, 
      "start_time":  start_time.timestamp(),
      "user":  bene['sls_id'],
      "fhir_id": bene['fhir_id']
    }
    return json.dumps(result)
  

def eob_response(bene, app, start_time, access_token_hash): 
    eob_sizes = [60101, 371753, 371582, 371463, 371388, 371156,398040,371999,925754,599323,921721,1089874,1136965,795386,377776,371234,420537,371734,549063,565998,620802,523836,677427,653]
    result = {
      "access_token_hash": access_token_hash,
      "app_id": app.app_id(),
      "app_name":  app.app_name(),
      "dev_id":  app.dev_id(),
      "dev_name":  app.dev_name(),
      "end_time":  (start_time + timedelta(seconds=0.013)).timestamp(),
      "ip_addr":  "35.224.154.231", 
      "location": "",
      "path":  "/v1/fhir/ExplanationOfBenefit/",
      "request_uuid":  str(uuid.uuid4()),
      "response_code":  200,
      "size":  random.choice(eob_sizes), 
      "start_time":  start_time.timestamp(),
      "user":  bene['sls_id'],
      "fhir_id": bene['fhir_id']
    }
    return json.dumps(result)

def patient_response(bene, app, start_time, access_token_hash):
    result = {
      "access_token_hash": access_token_hash,
      "app_id":app.app_id(),
      "app_name":  app.app_name(),
      "dev_id":  app.dev_id(),
      "dev_name":  app.dev_name(),
      "end_time":  (start_time + timedelta(seconds=0.004)).timestamp(), 
      "ip_addr":  "35.224.154.231", 
      "location": "",
      "path":  "/v1/fhir/Patient/" + bene['fhir_id'],
      "request_uuid":  str(uuid.uuid4()),
      "response_code":  200,
      "size":  3185, 
      "start_time":  start_time.timestamp(),
      "user":  bene['sls_id'],
      "fhir_id": bene['fhir_id']
    }
    return json.dumps(result)

def coverage_response(bene, app, start_time, access_token_hash):
    result = {
      "access_token_hash": access_token_hash,
      "app_id":app.app_id(),
      "app_name":  app.app_name(),
      "dev_id":  app.dev_id(),
      "dev_name":  app.dev_name(),
      "end_time":  (start_time + timedelta(seconds=0.004)).timestamp(), 
      "ip_addr":  "35.224.154.231", 
      "location": "",
      "path":  "/v1/fhir/Coverage/",
      "request_uuid":  str(uuid.uuid4()),
      "response_code":  200,
      "size":  22219, 
      "start_time":  start_time.timestamp(),
      "user":  bene['sls_id'],
      "fhir_id": bene['fhir_id']
    }
    return json.dumps(result)

def user_info_response(bene, app, start_time, access_token_hash):
    result = {
      "access_token_hash": access_token_hash,
      "app_id":app.app_id(),
      "app_name":  app.app_name(),
      "dev_id":  app.dev_id(),
      "dev_name":  app.dev_name(),
      "end_time":  (start_time + timedelta(seconds=0.002)).timestamp(), 
      "ip_addr":  "35.224.154.231", 
      "location": "",
      "path":  "/v1/connect/userinfo",
      "request_uuid":  str(uuid.uuid4()),
      "response_code":  200,
      "size":  142, 
      "start_time":  start_time.timestamp(),
      "user":  bene['sls_id'],
      "fhir_id": bene['fhir_id']
    }
    return json.dumps(result)

