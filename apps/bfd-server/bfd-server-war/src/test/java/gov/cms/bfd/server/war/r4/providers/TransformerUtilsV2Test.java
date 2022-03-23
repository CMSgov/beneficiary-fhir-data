package gov.cms.bfd.server.war.r4.providers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import gov.cms.bfd.model.codebook.data.CcwCodebookVariable;
import gov.cms.bfd.sharedutils.exceptions.BadCodeMonkeyException;
import java.math.BigDecimal;
import java.math.BigInteger;
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
   * Ensures the fi_num is correctly mapped to an eob as an extension when the input
   * fiscalIntermediaryNumber is present.
   */
  @Test
  public void mapEobCommonGroupInpOutHHAHospiceSNFWhenFiNumberExistsExpectExtensionOnEob() {

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

    assertNotNull(eob.getExtension());
    assertFalse(eob.getExtension().isEmpty());
    Extension fiNumExtension =
        eob.getExtension().stream()
            .filter(e -> expectedDiscriminator.equals(e.getUrl()))
            .findFirst()
            .orElse(null);
    assertNotNull(fiNumExtension);
    assertEquals(fiNum, ((Coding) fiNumExtension.getValue()).getCode());
  }

  /**
   * Ensures the fi_num is not mapped to an eob as an extension when the input
   * fiscalIntermediaryNumber is not present.
   */
  @Test
  public void mapEobCommonGroupInpOutHHAHospiceSNFWhenNoFiNumberExpectNoFiNumExtension() {

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

    assertNotNull(eob.getExtension());
    assertFalse(eob.getExtension().isEmpty());
    Extension fiNumExtension =
        eob.getExtension().stream()
            .filter(e -> expectedDiscriminator.equals(e.getUrl()))
            .findFirst()
            .orElse(null);
    assertNull(fiNumExtension);
  }

  /** Verifies that createCoding can take a Character type value and create a Coding from it. */
  @Test
  public void createCodingWhenValueIsCharacterExpectCodingWithValue() {

    Character codingValue = 'a';
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    Coding coding =
        TransformerUtilsV2.createCoding(eob, CcwCodebookVariable.BENE_HOSPC_PRD_CNT, codingValue);

    assertEquals(codingValue.toString(), coding.getCode());
  }

  /** Verifies that createCoding can take a String type value and create a Coding from it. */
  @Test
  public void createCodingWhenValueIsStringExpectCodingWithValue() {

    String codingValue = "abc";
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    Coding coding =
        TransformerUtilsV2.createCoding(eob, CcwCodebookVariable.BENE_HOSPC_PRD_CNT, codingValue);

    assertEquals(codingValue, coding.getCode());
  }

  /**
   * Verifies that createCoding throws an exception when an unexpected typed coding is passed to it.
   */
  @Test
  public void createCodingWhenValueIsUnexpectedTypeExpectException() {

    BigInteger codingValue = BigInteger.ONE;
    ExplanationOfBenefit eob = new ExplanationOfBenefit();

    assertThrows(
        BadCodeMonkeyException.class,
        () -> {
          TransformerUtilsV2.createCoding(eob, CcwCodebookVariable.BENE_HOSPC_PRD_CNT, codingValue);
        });
  }
}
