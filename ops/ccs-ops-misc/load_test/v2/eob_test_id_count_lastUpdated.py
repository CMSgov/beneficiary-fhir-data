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

eob_ids = data.load_bene_ids()
client_cert = setup.getClientCert()
setup.set_locust_env(config.load())
last_updated = data.get_last_updated()

class BFDUser(HttpUser):
    @task
    def explanation_of_benefit(self):
        if len(eob_ids) == 0:
            errors.no_data_stop_test(self)

        id = eob_ids.pop()
        self.client.get(f'/v2/fhir/ExplanationOfBenefit?patient={id}&_count=10&_format=application%2Ffhir%2Bjson&_lastUpdated=gt{last_updated}',
                cert=client_cert,
                verify=server_public_key,
                name='/v2/fhir/ExplanationOfBenefit search by id / count=10 / lastUpdated (2 weeks)')