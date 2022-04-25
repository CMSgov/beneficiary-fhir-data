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
eob_ids = data.load_bene_ids()
last_updated = data.get_last_updated()
cursor_list = data.load_cursors("v2")

class BFDUser(HttpUser):
    def on_start(self):
        copied_eob_ids = eob_ids.copy()
        random.shuffle(copied_eob_ids)

        copied_mbis = mbis.copy()
        random.shuffle(copied_mbis)

        copied_cursor_list = cursor_list.copy()
        random.shuffle(copied_cursor_list)

        self.eob_ids = copied_eob_ids
        self.mbis = copied_mbis
        self.cursor_list = copied_cursor_list

    def get_eob(self) -> int:
      if len(self.eob_ids) == 0:
            errors.no_data_stop_test(self)

      return self.eob_ids.pop()

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
        self.get('/v2/fhir/Coverage', params={'beneficiary': self.get_eob(), '_count': '10'},
                name='/v2/fhir/Coverage search by id / count=10')

    @task
    def coverage_test_id_lastUpdated(self):
        self.get('/v2/fhir/Coverage', params={'_lastUpdated': f'gt{last_updated}', 'beneficiary': self.get_eob()},
                name='/v2/fhir/Coverage search by id / lastUpdated (2 weeks)')

    @task
    def coverage_test_id(self):
        self.get('/v2/fhir/Coverage', params={'beneficiary': self.get_eob()},
                name='/v2/fhir/Coverage search by id')

    @task
    def eob_test_id_count(self):
        self.get('/v2/fhir/ExplanationOfBenefit', params={'patient': self.get_eob(), '_count': '10', '_format': 'application/fhir+json'},
                name='/v2/fhir/ExplanationOfBenefit search by id / count=10')

    @task
    def eob_test_id_includeTaxNumber(self):
        self.get('/v2/fhir/ExplanationOfBenefit', params={'_lastUpdated': f'gt{last_updated}', 'patient': self.get_eob(), '_IncludeTaxNumbers': 'true', '_format': 'application/fhir+json'},
                name='/v2/fhir/ExplanationOfBenefit search by id / lastUpdated / includeTaxNumbers = true')

    @task
    def eob_test_id(self):
        self.get('/v2/fhir/ExplanationOfBenefit', params={'patient': self.get_eob(), '_format': 'application/fhir+json'},
                name='/v2/fhir/ExplanationOfBenefit search by id')

    @task
    def patient_test_coverageContract(self):
        self.get(self.get_cursor_path(), headers={"IncludeIdentifiers": "mbi"},
                name='/v2/fhir/Patient search by coverage contract (all pages)')

    @task
    def patient_test_hashedMbi(self):
        self.get('/v2/fhir/Patient', params={'identifier': f'https://bluebutton.cms.gov/resources/identifier/mbi-hash|{self.get_mbi()}', '_IncludeIdentifiers': 'mbi'},
                name='/v2/fhir/Patient search by hashed mbi / _IncludeIdentifiers=mbi')

    @task
    def patient_test_id_lastUpdated(self):
        self.get('/v2/fhir/Patient', params={'_id': self.get_eob(), '_format': 'application/fhir+json', '_IncludeIdentifiers': 'mbi', '_lastUpdated': f'gt{last_updated}'},
                name='/v2/fhir/Patient search by id / _IncludeIdentifiers=mbi / last updated (2 weeks)')

    @task
    def patient_test_id(self):
        self.get('/v2/fhir/Patient', params={'_id': self.get_eob(), '_format': 'application/fhir+json'},
                name='/v2/fhir/Patient search by id')

'''
Adds a global failsafe check to ensure that if this test overwhelms the
database, we bail out and stop hitting the server.
'''
@events.init.add_listener
def on_locust_init(environment, **_kwargs):
    validation.setup_failsafe_event(environment, validation.SLA_V2_BASELINE)

'''
Adds a listener that will run when the test ends which checks the various
response time percentiles against the SLA for this endpoint.
'''
@events.test_stop.add_listener
def on_locust_quit(environment, **_kwargs):
    validation.check_sla_validation(environment, validation.SLA_V2_BASELINE)