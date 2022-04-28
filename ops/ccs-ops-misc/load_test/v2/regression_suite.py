"""Regression test suite for V2 BFD Server endpoints.

The tests within this Locust test suite hit various endpoints that were
determined to be representative of typical V2 endpoint loads. When running
this test suite, all tests in this suite will be run in parallel, with
equal weighting being applied to each.
"""

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
cursor_list = data.load_cursors("v2")

class BFDUser(HttpUser):
    def on_start(self):
        """Run once when a BFDUser is initialized by Locust.

        This method copies the necessary test data (lists of
        MBIs, beneficiary IDs, and contract cursor URLs) as
        members of this particular BFDUser instance. We then
        shuffle these copied lists such that concurrent BFDUsers
        are not querying the same data at the same time.
        """

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
      """Returns the next beneficiary ID in this BFDUser's list of IDs.

      This method pops (that is, takes the topmost item) the next beneficiary
      ID in this instance's list of beneficiary IDs and returns it. If no
      more IDs are available, this method will stop the test run.
      """

      if len(self.bene_ids) == 0:
            errors.no_data_stop_test(self)

      return self.bene_ids.pop()

    def get_mbi(self) -> int:
      """Returns the next MBI in this BFDUser's list of MBIs.

      This method pops (that is, takes the topmost item) the next MBI
      in this instance's list of MBIs and returns it. If no
      more MBIs are available, this method will stop the test run.
      """

      if len(self.mbis) == 0:
            errors.no_data_stop_test(self)

      return self.mbis.pop()

    def get_cursor_path(self) -> str:
      """Returns the next cursor path in this BFDUser's list of URL paths.

      This method pops (that is, takes the topmost item) the next contract cursor
      URL path in this instance's list of contract paths and returns it. If no
      more paths are available, this method will stop the test run.
      """

      if len(self.cursor_list) == 0:
        errors.no_data_stop_test(self)

      return self.cursor_list.pop()

    def get(self, base_path: str, params: Dict[str, str] = {}, headers: Dict[str, str] = {}, name: str = ''):
      """Sends a GET request to the endpoint at base_path with the various query string parameters and headers specified.

      This method extends Locust's HttpUser::client.get() method to make creating the requests
      nicer. Specifically, the query string parameters are specified as a separate dictionary
      opposed to part of the path, the cert and verify arguments (which will never change)
      are already set, and Cache-Control headers are automatically set to ensure caching is
      disabled.
      """

      self.client.get(create_url_path(base_path, params),
                      cert=client_cert,
                      verify=server_public_key,
                      headers={**headers, 'Cache-Control': 'no-store, no-cache'},
                      name=name)

    @task
    def coverage_test_id_count(self):
        self.get('/v2/fhir/Coverage', params={'beneficiary': self.get_bene_id(), '_count': '10'},
                name='/v2/fhir/Coverage search by id / count=10')

    @task
    def coverage_test_id_lastUpdated(self):
        self.get('/v2/fhir/Coverage', params={'_lastUpdated': f'gt{last_updated}', 'beneficiary': self.get_bene_id()},
                name='/v2/fhir/Coverage search by id / lastUpdated (2 weeks)')

    @task
    def coverage_test_id(self):
        self.get('/v2/fhir/Coverage', params={'beneficiary': self.get_bene_id()},
                name='/v2/fhir/Coverage search by id')

    @task
    def eob_test_id_count(self):
        self.get('/v2/fhir/ExplanationOfBenefit', params={'patient': self.get_bene_id(), '_count': '10', '_format': 'application/fhir+json'},
                name='/v2/fhir/ExplanationOfBenefit search by id / count=10')

    @task
    def eob_test_id_includeTaxNumber(self):
        self.get('/v2/fhir/ExplanationOfBenefit', params={'_lastUpdated': f'gt{last_updated}', 'patient': self.get_bene_id(), '_IncludeTaxNumbers': 'true', '_format': 'application/fhir+json'},
                name='/v2/fhir/ExplanationOfBenefit search by id / lastUpdated / includeTaxNumbers = true')

    @task
    def eob_test_id(self):
        self.get('/v2/fhir/ExplanationOfBenefit', params={'patient': self.get_bene_id(), '_format': 'application/fhir+json'},
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
        self.get('/v2/fhir/Patient', params={'_id': self.get_bene_id(), '_format': 'application/fhir+json', '_IncludeIdentifiers': 'mbi', '_lastUpdated': f'gt{last_updated}'},
                name='/v2/fhir/Patient search by id / _IncludeIdentifiers=mbi / last updated (2 weeks)')

    @task
    def patient_test_id(self):
        self.get('/v2/fhir/Patient', params={'_id': self.get_bene_id(), '_format': 'application/fhir+json'},
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