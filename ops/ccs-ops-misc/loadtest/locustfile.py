import csv
import os
import sys
from locust import HttpUser, task

try:
    ids_file = os.environ['BFD_LT_IDS_FILE']
    cert = os.environ['BFD_LT_CERT']
except KeyError as err:
    print(f"must provide env var: {err}")
    sys.exit(1)

try:
    with open(ids_file, 'r') as f:
        eob_ids = f.read().split('\n')

        # remove empty string at the end
        eob_ids.pop()

        patient_ids = eob_ids.copy()
except FileNotFoundError as err:
    print(f"key file not found: {err}")
    sys.exit(1)

class BFDUser(HttpUser):
    @task(3)
    def explanation_of_benefit(self):
        id = eob_ids.pop()
        self.client.get(f"/v1/fhir/ExplanationOfBenefit?patient={id}&_format=json",
                cert=cert,
                verify=False,
                name="/v1/fhir/ExplanationOfBenefit/?patient={id}&_format=json")
    
    @task(1)
    def patient(self):
        id = patient_ids.pop()
        self.client.get(f"/v2/fhir/Patient/{id}",
                cert=cert,
                verify=False,
                name="/v2/fhir/Patient/{id}")

