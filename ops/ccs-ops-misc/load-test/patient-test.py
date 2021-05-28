import os
import sys
import urllib3
from locust import HttpUser, task

'''
If there is a public key to verify the BFD Server's responses
then it can be passed in with an environment variable. Otherwise,
the error from the self-signed cert is ignored. The warnings are also
disabled because thousands will appear in the logs and make it difficult
to see anything else.
'''
try:
    server_public_key = os.environ['SERVER_PUBLIC_KEY']
except KeyError:
    server_public_key = False
    urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

'''
A file of beneficiary IDs to use in requests and a
client cert to authenticate with BFD are both required.
'''
try:
    ids_file = os.environ['IDS_FILE']
    client_cert = os.environ['CLIENT_CERT']
except KeyError as err:
    print(f'must provide env var: {err}')
    sys.exit(1)

try:
    with open(ids_file, 'r') as f:
        bene_ids = f.read().split('\n')

        # remove empty string at the end
        bene_ids.pop()

except FileNotFoundError as err:
    print(f'key file not found: {err}')
    sys.exit(1)

class BFDUser(HttpUser):
    @task
    def patient_by_id(self):
        id = bene_ids.pop()
        self.client.get(f'/v1/fhir/Patient/{id}',
                cert=client_cert,
                verify=server_public_key,
                name='/v1/fhir/Patient/{id}')

