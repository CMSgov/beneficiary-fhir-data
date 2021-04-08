package gov.cms.bfd.server.war.r4.providers;

import ca.uhn.fhir.context.FhirContext;
import com.codahale.metrics.MetricRegistry;
import gov.cms.bfd.model.rif.InpatientClaim;
import gov.cms.bfd.model.rif.OutpatientClaim;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.adapter.AdapterFilter;
import gov.cms.bfd.server.war.commons.adapter.InpatientClaimAdaptor;
import gov.cms.bfd.server.war.commons.adapter.OutpatientClaimAdaptor;
import gov.cms.bfd.server.war.commons.adapter.PartDEventAdaptor;
import java.util.Arrays;
import java.util.List;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.junit.Test;

public final class AdaptorPrototypeTest {

  public <T> T generateClaim(Class<T> clazz) throws FHIRException {
    List<Object> parsedRecords =
        ServerTestUtils.parseData(Arrays.asList(StaticRifResourceGroup.SAMPLE_A.getResources()));

    T claim =
        parsedRecords.stream()
            .filter(r -> clazz.isInstance(r))
            .map(r -> clazz.cast(r))
            .findFirst()
            .get();

    return claim;
  }

  private static final FhirContext fhirContext = FhirContext.forR4();

  @Test
  public void serialize_outpatient() throws FHIRException {

    ExplanationOfBenefit eob =
        ExplainationOfBenefitTransformerV2.transform(
            new MetricRegistry(), new OutpatientClaimAdaptor(generateClaim(OutpatientClaim.class)));

    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }

  @Test
  public void serialize_inpatient() throws FHIRException {

    ExplanationOfBenefit eob =
        ExplainationOfBenefitTransformerV2.transform(
            new MetricRegistry(), new InpatientClaimAdaptor(generateClaim(InpatientClaim.class)));

    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }

  @Test
  public void serialize_pde() throws FHIRException {

    ExplanationOfBenefit eob =
        ExplainationOfBenefitTransformerV2.transform(
            new MetricRegistry(), new PartDEventAdaptor(generateClaim(PartDEvent.class)));

    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }

  @Test
  public void serialize_filtered_outpatient() throws FHIRException {

    ExplanationOfBenefit eob =
        ExplainationOfBenefitTransformerV2.transform(
            new MetricRegistry(),
            AdapterFilter.create(
                new OutpatientClaimAdaptor(generateClaim(OutpatientClaim.class)),
                "getNearLineRecordIdCode",
                "getClaimGroupId",
                "getClaimTypeCode"));

    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }

  @Test
  public void serialize_filtered_inpatient() throws FHIRException {

    ExplanationOfBenefit eob =
        ExplainationOfBenefitTransformerV2.transform(
            new MetricRegistry(),
            AdapterFilter.create(
                new InpatientClaimAdaptor(generateClaim(InpatientClaim.class)),
                "getPaymentAmount"));

    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }

  @Test
  public void serialize_filtered_outpatient_line() throws FHIRException {

    ExplanationOfBenefit eob =
        ExplainationOfBenefitTransformerV2.transform(
            new MetricRegistry(),
            AdapterFilter.create(
                new OutpatientClaimAdaptor(generateClaim(OutpatientClaim.class)),
                "getNationalDrugCode"));

    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }

  @Test
  public void serialize_filtered_pde_line() throws FHIRException {

    ExplanationOfBenefit eob =
        ExplainationOfBenefitTransformerV2.transform(
            new MetricRegistry(),
            AdapterFilter.create(
                new PartDEventAdaptor(generateClaim(PartDEvent.class)), "getNationalDrugCode"));

    System.out.println(fhirContext.newJsonParser().encodeResourceToString(eob));
  }
}
