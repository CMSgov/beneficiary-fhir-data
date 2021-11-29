import csv
import datetime
import os
import sys
import urllib3
import common.config as config
import common.test_setup as setup
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

class BFDUser(HttpUser):
    @task
    def explanation_of_benefit(self):
        id = eob_ids.pop()
        self.client.get(f'/v2/fhir/ExplanationOfBenefit?_count=10&patient={id}&_format=application%2Ffhir%2Bjson',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/ExplanationOfBenefit')