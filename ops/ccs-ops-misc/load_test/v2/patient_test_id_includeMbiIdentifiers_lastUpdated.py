import csv
import datetime
import os
import sys
import urllib3
import common.config as config
import common.test_setup as setup
import locust.exception as locust_exception
from locust import HttpUser, task

server_public_key = setup.loadServerPublicKey()

'''
If there is no server cert, the warnings are disabled because thousands will appear in the logs and make it difficult
to see anything else.
'''
if not server_public_key:
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

eob_ids = setup.generateAndLoadIds()
client_cert = setup.getClientCert()
setup.set_locust_env(config.load())

'''
The lastUpdated field defaults to two weeks before when the script is run. The time delta can be modified.
'''
today = datetime.datetime.now()
delta = datetime.timedelta(weeks = 2)
prior_date = today - delta
last_updated = prior_date.strftime('%Y-%m-%d')

class BFDUser(HttpUser):
    @task
    def patient(self):
        if len(eob_ids) == 0:
            print("Ran out of data, stopping test...")
            raise locust_exception.StopUser()

        id = eob_ids.pop()
        self.client.get(f'/v2/fhir/Patient?_id={id}&_format=application%2Ffhir%2Bjson&_IncludeIdentifiers=mbi&_lastUpdated=gt{last_updated}',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/Patient search by id / _IncludeIdentifiers=mbi / last updated (2 weeks)')