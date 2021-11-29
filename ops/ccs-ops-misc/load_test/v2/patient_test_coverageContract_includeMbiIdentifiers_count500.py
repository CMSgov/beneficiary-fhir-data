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

##TODO: get coverage contract ids?
id1 = "7CZ0001"
id2 = "7C0003"

client_cert = setup.getClientCert()


class BFDUser(HttpUser):
    @task
    def patient(self):
        id = eob_ids.pop()
        self.client.get('/v2/fhir/Patient'
        + f'?_has%3ACoverage.extension=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fvariables%2Fptdcntrct01%{id1}'
        + f'&_has%3ACoverage.rfrncyr=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fvariables%2Frfrnc_yr%{id2}'
        + '&_count=500'
        + '&_format=json'
        + '&_IncludeIdentifiers=mbi',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/Patient')