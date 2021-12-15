import os
import sys
import urllib3
import common.config as config
import common.test_setup as setup
import common.errors as errors
import common.data as data
import common.errors as errors
import common.read_contract_cursors as cursors
import locust.exception as locust_exception
from locust.runners import MasterRunner, WorkerRunner
from locust import HttpUser, task, events

server_public_key = setup.loadServerPublicKey()
setup.disable_no_cert_warnings(server_public_key, urllib3)

client_cert = setup.getClientCert()
setup.set_locust_env(config.load())
cursor_list = data.load_cursors("v2")

class BFDUser(HttpUser):

    @task
    def patient(self):
        if len(cursor_list) == 0:
            errors.no_data_stop_test(self)

        cursor_url = cursor_list.pop()

        response = self.client.get(cursor_url,
                cert=client_cert,
                verify=server_public_key,
                headers={"IncludeIdentifiers": "mbi"},
                name='/v2/fhir/Patient search by coverage contract (all pages)')