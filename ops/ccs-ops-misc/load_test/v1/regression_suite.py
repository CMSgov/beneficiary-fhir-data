"""Regression test suite for V1 BFD Server endpoints.

The tests within this Locust test suite hit various endpoints that were
determined to be representative of typical V1 endpoint loads. When running
this test suite, all tests in this suite will be run in parallel, with
equal weighting being applied to each.
"""

from common.BFDUserBase import BFDUserBase
from common.url_path import create_url_path
from common.validation import SLA_V1_BASELINE
from locust import task

class BFDUser(BFDUserBase):
    DATA_REQUIRED = [ 'BENE_IDS', 'MBIS', 'CONTRACT_IDS' ]
    SLA_BASELINE = SLA_V1_BASELINE


    @task
    def coverage_test_id_count(self):
        def make_url(instance):
            return create_url_path('/v1/fhir/Coverage', {
                'beneficiary': instance.bene_ids.pop(),
                '_count': '10'
            })

        self.run_task(name='/v1/fhir/Coverage search by id / count=10', url_callback=make_url)


    @task
    def coverage_test_id_lastUpdated(self):
        def make_url(instance):
            return create_url_path('/v1/fhir/Coverage', {
                '_lastUpdated': f'gt{instance.last_updated}',
                'beneficiary': instance.bene_ids.pop(),
            })

        self.run_task(name='/v1/fhir/Coverage search by id / lastUpdated (2 weeks)', url_callback=make_url)

   
    @task
    def eob_test_id(self):
        def make_url(instance):
            return create_url_path('/v1/fhir/ExplanationOfBenefit', {
                'patient': instance.bene_ids.pop(),
                '_format': 'application/fhir+json'
            })

        self.run_task(name='/v1/fhir/ExplanationOfBenefit search by id', url_callback=make_url)


    @task
    def eob_test_id_count_typePDE(self):
        def make_url(instance):
            return create_url_path('/v1/fhir/ExplanationOfBenefit', {
                'patient': instance.bene_ids.pop(),
                '_format': 'json',
                '_count': '50',
                '_types': 'PDE'
            })

        self.run_task(name='/v1/fhir/ExplanationOfBenefit search by id / type = PDE / count = 50', url_callback=make_url)


    @task
    def eob_test_id_lastUpdated_count(self):
        def make_url(instance):
            return create_url_path('/v1/fhir/ExplanationOfBenefit', {
                'patient': instance.bene_ids.pop(),
                '_format': 'json',
                '_count': '100',
                '_lastUpdated': f'gt{instance.last_updated}'
            })

        self.run_task(name='/v1/fhir/ExplanationOfBenefit search by id / lastUpdated / count = 100', url_callback=make_url)

    
    @task
    def eob_test_id_lastUpdated_includeTaxNumbers(self):
        def make_url(instance):
            return create_url_path('/v1/fhir/ExplanationOfBenefit', {
                'patient': instance.bene_ids.pop(),
                '_format': 'json',
                '_lastUpdated': f'gt{instance.last_updated}',
                '_IncludeTaxNumbers': 'true'
            })

        self.run_task(name='/v1/fhir/ExplanationOfBenefit search by id / lastUpdated / includeTaxNumbers', url_callback=make_url)


    @task
    def eob_test_id_lastUpdated(self):
        def make_url(instance):
            return create_url_path('/v1/fhir/ExplanationOfBenefit', {
                'patient': instance.bene_ids.pop(),
                '_format': 'json',
                '_lastUpdated': f'gt{instance.last_updated}'
            })

        self.run_task(name='/v1/fhir/ExplanationOfBenefit search by id / lastUpdated', url_callback=make_url)

    @task
    def patient_test_id(self):
        def make_url(instance):
            return create_url_path(f'/v1/fhir/Patient/{instance.bene_ids.pop()}', {})

        self.run_task(name='/v1/fhir/Patient/id', url_callback=make_url)
    

    @task
    def patient_test_id_lastUpdated_includeMbi_includeAddress(self):
        def make_url(instance):
            return create_url_path('/v1/fhir/Patient', {
                '_id': instance.bene_ids.pop(),
                '_lastUpdated': f'gt{instance.last_updated}',
                '_IncludeIdentifiers': 'mbi',
                '_IncludeTaxNumbers': 'true'
            })

        self.run_task(name='/v1/fhir/Patient/id search by id / (2 weeks) / includeTaxNumbers / mbi', url_callback=make_url)

    @task
    def patient_test_coverageContract(self):
        def make_url(instance):
            contract_data = instance.contract_ids.pop()
            return create_url_path('/v1/fhir/Patient', {
                '_has:Coverage.extension': f'https://bluebutton.cms.gov/resources/variables/ptdcntrct01|{contract_data["id"]}',
                '_has:Coverage.rfrncyr': f'https://bluebutton.cms.gov/resources/variables/rfrnc_yr|{contract_data["year"]}',
                '_count': 25,
                '_format': 'json'
            })

        self.run_task(name='/v1/fhir/Patient search by coverage contract (all pages)', headers={"IncludeIdentifiers": "mbi"},
                url_callback=make_url)


    @task
    def patient_test_hashedMbi(self):
        def make_url(instance):
            return create_url_path(f'/v1/fhir/Patient/', {
                'identifier': f'https://bluebutton.cms.gov/resources/identifier/mbi-hash|{instance.mbis.pop()}',
                '_IncludeIdentifiers': 'mbi'
            })

        self.run_task(name='/v1/fhir/Patient search by hashed mbi / includeIdentifiers = mbi', url_callback=make_url)
