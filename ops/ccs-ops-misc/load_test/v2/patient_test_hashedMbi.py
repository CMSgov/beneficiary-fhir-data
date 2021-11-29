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

mbis = setup.generateAndLoadMbis()
client_cert = setup.getClientCert()

class BFDUser(HttpUser):
    @task
    def patient(self):
        hashed_mbi = mbis.pop()
        self.client.get(f'/v2/fhir/Patient?_mbiHash={hashed_mbi}',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/Patient')