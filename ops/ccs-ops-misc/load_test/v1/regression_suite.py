import random
from typing import Dict
import urllib3
import common.config as config
import common.data as data
import common.errors as errors
import common.test_setup as setup
from common.url_path import create_url_path
import common.validation as validation
from locust import HttpUser, task, events, tag

client_cert = setup.getClientCert()
server_public_key = setup.loadServerPublicKey()
setup.disable_no_cert_warnings(server_public_key, urllib3)
setup.set_locust_env(config.load())

mbis = data.load_mbis()
bene_ids = data.load_bene_ids()
last_updated = data.get_last_updated()
cursor_list = data.load_cursors("v1")

class BFDUser(HttpUser):
    def on_start(self):
        copied_bene_ids = bene_ids.copy()
        random.shuffle(copied_bene_ids)

        copied_mbis = mbis.copy()
        random.shuffle(copied_mbis)

        copied_cursor_list = cursor_list.copy()
        random.shuffle(copied_cursor_list)

        self.bene_ids = copied_bene_ids
        self.mbis = copied_mbis
        self.cursor_list = copied_cursor_list

    def get_bene_id(self) -> int:
      if len(self.bene_ids) == 0:
            errors.no_data_stop_test(self)

      return self.bene_ids.pop()

    def get_mbi(self) -> int:
      if len(self.mbis) == 0:
            errors.no_data_stop_test(self)

      return self.mbis.pop()

    def get_cursor_path(self) -> str:
      if len(self.cursor_list) == 0:
        errors.no_data_stop_test(self)

      return self.cursor_list.pop()

    def get(self, base_path: str, params: Dict[str, str] = {}, headers: Dict[str, str] = {}, name: str = ''):
      self.client.get(create_url_path(base_path, params),
                      cert=client_cert,
                      verify=server_public_key,
                      headers={**headers, 'Cache-Control': 'no-store, no-cache'},
                      name=name)

    @task
    def coverage_test_id_count(self):
        self.get(f'/v1/fhir/Coverage', params={'beneficiary': f'{self.get_bene_id()}', '_count':'10'},
                name='/v1/fhir/Coverage search by id / count=10')

    @task
    def coverage_test_id_lastUpdated(self):
        self.get(f'/v1/fhir/Coverage', params={'beneficiary': f'{self.get_bene_id()}', '_lastUpdated': f'gt{last_updated}'},
                name='/v1/fhir/Coverage search by id / lastUpdated (2 weeks)')
    
    @task
    def eob_test_id(self):
        self.get(f'/v1/fhir/ExplanationOfBenefit', params={'patient': f'{self.get_bene_id()}', '_format': 'json'},
                name='/v1/fhir/ExplanationOfBenefit search by id')

    @task
    def eob_test_id_count_typePDE(self):
        self.get(f'/v1/fhir/ExplanationOfBenefit', params={'patient': f'{self.get_bene_id()}', '_format': 'json', '_count': '50', '_types': 'PDE'},
                name='/v1/fhir/ExplanationOfBenefit search by id / type = PDE / count = 50')

    @task
    def eob_test_id_lastUpdated_count(self):
        self.get(f'/v1/fhir/ExplanationOfBenefit', params={'patient': f'{self.get_bene_id()}', '_format': 'json', '_count': '100', '_lastUpdated': f'gt{last_updated}',},
                name='/v1/fhir/ExplanationOfBenefit search by id / lastUpdated / count = 100')

    @task
    def eob_test_id_lastUpdated_includeTaxNumbers(self):
        self.get(f'/v1/fhir/ExplanationOfBenefit', params={'patient': f'{self.get_bene_id()}', '_format': 'json', '_lastUpdated': f'gt{last_updated}', '_IncludeTaxNumbers': 'true'},
                name='/v1/fhir/ExplanationOfBenefit search by id / lastUpdated / includeTaxNumbers = true')

    @task
    def eob_test_id_lastUpdated(self):
        self.get(f'/v1/fhir/ExplanationOfBenefit', params={'patient': f'{self.get_bene_id()}', '_format': 'json', '_lastUpdated': f'gt{last_updated}'},
                name='/v1/fhir/ExplanationOfBenefit search by id / lastUpdated')

    @task
    def patient_test_id(self):
        self.get(f'/v1/fhir/Patient/{self.get_bene_id()}',
                name='/v1/fhir/Patient/id')

    @task
    def patient_test_id_lastUpdated_includeMbi_includeAddress(self):
        self.get(f'/v1/fhir/Patient', params={'_id': f'{self.get_bene_id()}', '_lastUpdated': f'gt{last_updated}', '_IncludeIdentifiers': 'mbi', '_IncludeTaxNumbers': 'true'},
                name='/v1/fhir/Patient/id search by id / lastUpdated (2 weeks) / includeTaxNumbers = true / includeIdentifiers = mbi')

    @task
    def patient_test_coverageContract(self):
        self.get(self.get_cursor_path(), headers={"IncludeIdentifiers": "mbi"},
                name='/v1/fhir/Patient search by coverage contract (all pages)')

    @task
    def patient_test_hashedMbi(self):
        self.get(f'/v1/fhir/Patient', params={'identifier': f'https://bluebutton.cms.gov/resources/identifier/mbi-hash|{self.get_mbi()}', '_IncludeIdentifiers': 'mbi'},
                name='/v1/fhir/Patient search by hashed mbi / includeIdentifiers = mbi')

'''
Adds a global failsafe check to ensure that if this test overwhelms the
database, we bail out and stop hitting the server.
'''
@events.init.add_listener
def on_locust_init(environment, **_kwargs):
    validation.setup_failsafe_event(environment, validation.SLA_V1_BASELINE)

'''
Adds a listener that will run when the test ends which checks the various
response time percentiles against the SLA for this endpoint.
'''
@events.test_stop.add_listener
def on_locust_quit(environment, **_kwargs):
    validation.check_sla_validation(environment, validation.SLA_V1_BASELINE)