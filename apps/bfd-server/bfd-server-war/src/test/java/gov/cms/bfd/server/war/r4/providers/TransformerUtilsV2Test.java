package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Optional;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Extension;
import org.junit.jupiter.api.Test;

/** Tests the utility methods within the {@link TransformerUtilsV2}. */
public class TransformerUtilsV2Test {

  /**
   * Ensures the revenue status code is correctly mapped to an item's revenue as an extension when
   * the input statusCode is present.
   */
  @Test
  public void mapEobCommonItemRevenueStatusCodeWhenStatusCodeExistsExpectExtensionOnItem() {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    ExplanationOfBenefit.ItemComponent item = new ExplanationOfBenefit.ItemComponent();
    eob.addItem(item);

    Optional<String> statusCode = Optional.of("1");
    String expectedExtensionUrl =
        "https://bluebutton.cms.gov/resources/variables/rev_cntr_stus_ind_cd";

    TransformerUtilsV2.mapEobCommonItemRevenueStatusCode(item, eob, statusCode);

    assertNotNull(item);
    assertNotNull(item.getRevenue());
    assertNotNull(item.getRevenue().getExtension());
    assertEquals(1, item.getRevenue().getExtension().size());
    Extension ext = item.getRevenue().getExtensionByUrl(expectedExtensionUrl);
    assertNotNull(ext);
    assertEquals(expectedExtensionUrl, ext.getUrl());
    assertTrue(ext.getValue() instanceof Coding);
    assertEquals(statusCode.get(), ((Coding) ext.getValue()).getCode());
  }

  /**
   * Verifies the item revenue status code is not mapped to an extension when the revenue status
   * code field is not present (empty optional).
   */
  @Test
  public void mapEobCommonItemRevenueStatusCodeWhenStatusCodeDoesNotExistExpectNoExtensionOnItem() {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    ExplanationOfBenefit.ItemComponent item = new ExplanationOfBenefit.ItemComponent();
    eob.addItem(item);
    CodeableConcept revenue = new CodeableConcept();
    item.setRevenue(revenue);

    Optional<String> statusCode = Optional.empty();

    TransformerUtilsV2.mapEobCommonItemRevenueStatusCode(item, eob, statusCode);

    assertNotNull(item);
    assertNotNull(item.getRevenue());
    assertNotNull(item);
    assertNotNull(item.getRevenue());
    assertNotNull(item.getRevenue().getExtension());
    assertEquals(0, item.getRevenue().getExtension().size());
  }

  /** Verifies an exception is thrown when the item is passed in as null. */
  @Test
  public void mapEobCommonItemRevenueStatusCodeWhenNullItemExpectException() {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    Optional<String> statusCode = Optional.of("1");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          TransformerUtilsV2.mapEobCommonItemRevenueStatusCode(null, eob, statusCode);
        });
  }

  /**
   * Verifies an exception is thrown when the eob is passed in as null.
   *
   * <p>Ideally a null eob would not cause issues since it's just used for debugging, but downstream
   * requires it to exist for now
   */
  @Test
  public void mapEobCommonItemRevenueStatusCodeWhenNullEobExpectException() {
    ExplanationOfBenefit eob = new ExplanationOfBenefit();
    ExplanationOfBenefit.ItemComponent item = new ExplanationOfBenefit.ItemComponent();
    eob.addItem(item);

    Optional<String> statusCode = Optional.of("1");

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          TransformerUtilsV2.mapEobCommonItemRevenueStatusCode(item, null, statusCode);
        });
  }

  /**
   * Verifies that when the fiscalIntermediaryNumber is present on the claim, it is properly mapped
   * to the supporting info as a value and coding.
   */
  @Test
  public void mapEobCommonGroupInpOutHHAHospiceSNFWhenFiNumberPresentExpectSupportingInfoMapping() {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    String fiNum = "12534";
    String expectedDiscriminator = "https://bluebutton.cms.gov/resources/variables/fi_num";

    TransformerUtilsV2.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        Optional.empty(),
        ' ',
        ' ',
        Optional.empty(),
        "",
        ' ',
        Optional.empty(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        Optional.of(fiNum),
        Optional.empty());

    assertNotNull(eob);
    assertNotNull(eob.getSupportingInfo());
    assertFalse(eob.getSupportingInfo().isEmpty());
    // Find the supporting info that has the fi num data
    ExplanationOfBenefit.SupportingInformationComponent supportingInfo =
        eob.getSupportingInfo().stream()
            .filter(
                si ->
                    si.getCode().getCoding().stream()
                        .anyMatch(c -> expectedDiscriminator.equals(c.getSystem())))
            .findFirst()
            .orElse(null);
    assertNotNull(supportingInfo, "Found no supporting info which had FI_NUM coding within.");
    assertFalse(supportingInfo.isEmpty());
    Optional<Coding> fiNumCoding =
        supportingInfo.getCategory().getCoding().stream()
            .filter(c -> expectedDiscriminator.equals(c.getCode()))
            .findFirst();
    assertTrue(
        fiNumCoding.isPresent(),
        "Missing expected supporting info category coding for FI_NUM (fiscalIntermediaryNumber)");
    Optional<Coding> infoCoding =
        supportingInfo.getCategory().getCoding().stream()
            .filter(c -> "info".equals(c.getCode()))
            .findFirst();
    assertTrue(
        infoCoding.isPresent(),
        "Missing expected supporting info category coding for info (claim info category for FI_NUM)");

    // Check Code exists with correct discriminator and value
    Coding code =
        supportingInfo.getCode().getCoding().stream()
            .filter(c -> expectedDiscriminator.equals(c.getSystem()))
            .findFirst()
            .orElse(null);
    assertNotNull(code, "Missing expected eob.coding ");
    assertEquals(fiNum, supportingInfo.getCode().getCoding().get(0).getCode());
  }

  /**
   * Verifies that when the fiscalIntermediaryNumber is not present on the claim, it is not mapped
   * to the supporting info as a value and coding related to FI_NUM.
   */
  @Test
  public void
      mapEobCommonGroupInpOutHHAHospiceSNFWhenFiNumberNotPresentExpectNoSupportingInfoMapping() {

    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    String expectedDiscriminator = "https://bluebutton.cms.gov/resources/variables/fi_num";

    TransformerUtilsV2.mapEobCommonGroupInpOutHHAHospiceSNF(
        eob,
        Optional.empty(),
        ' ',
        ' ',
        Optional.empty(),
        "",
        ' ',
        Optional.empty(),
        BigDecimal.ZERO,
        BigDecimal.ZERO,
        Optional.empty(),
        Optional.empty());

    assertNotNull(eob);
    assertNotNull(eob.getSupportingInfo());
    assertFalse(eob.getSupportingInfo().isEmpty());
    // Should find no supporting info which has fi_num related coding
    ExplanationOfBenefit.SupportingInformationComponent supportingInfo =
        eob.getSupportingInfo().stream()
            .filter(
                si ->
                    si.getCode().getCoding().stream()
                        .anyMatch(c -> expectedDiscriminator.equals(c.getSystem())))
            .findFirst()
            .orElse(null);
    assertNull(supportingInfo);
  }
}
