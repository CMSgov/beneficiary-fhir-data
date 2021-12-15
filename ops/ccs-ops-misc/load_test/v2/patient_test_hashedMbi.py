import csv
import datetime
import os
import sys
import urllib3
import common.config as config
import common.data as data
import common.errors as errors
import common.test_setup as setup
import locust.exception as locust_exception
from locust import HttpUser, task

server_public_key = setup.loadServerPublicKey()
setup.disable_no_cert_warnings(server_public_key, urllib3)

mbis = data.load_mbis()
client_cert = setup.getClientCert()
setup.set_locust_env(config.load())

class BFDUser(HttpUser):
    @task
    def patient(self):
        if len(mbis) == 0:
            errors.no_data_stop_test(self)

        hashed_mbi = mbis.pop()
        self.client.get(f'/v2/fhir/Patient?identifier=https%3A%2F%2Fbluebutton.cms.gov%2Fresources%2Fidentifier%2Fmbi-hash%7C%0A{hashed_mbi}',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/Patient search by hashed mbi')