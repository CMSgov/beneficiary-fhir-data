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

class BFDUser(HttpUser):
    @task
    def explanation_of_benefit(self):
        if len(eob_ids) == 0:
            print("Ran out of data, stopping test...")
            raise locust_exception.StopUser()

        id = eob_ids.pop()
        self.client.get(f'/v2/fhir/ExplanationOfBenefit?patient={id}&_count=10&_format=application%2Ffhir%2Bjson',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/ExplanationOfBenefit')