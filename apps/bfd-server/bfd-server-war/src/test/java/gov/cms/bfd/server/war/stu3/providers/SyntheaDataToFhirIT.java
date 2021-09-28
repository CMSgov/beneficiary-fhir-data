package gov.cms.bfd.server.war.stu3.providers;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import com.justdavis.karl.misc.exceptions.BadCodeMonkeyException;
import gov.cms.bfd.model.rif.Beneficiary;
import gov.cms.bfd.model.rif.CarrierClaim;
import gov.cms.bfd.model.rif.PartDEvent;
import gov.cms.bfd.model.rif.samples.StaticRifResourceGroup;
import gov.cms.bfd.server.war.ServerTestUtils;
import gov.cms.bfd.server.war.commons.MedicareSegment;
import gov.cms.bfd.sharedutils.SharedTestUtils;
import gov.cms.bfd.sharedutils.SlowTests;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.hibernate.internal.SessionFactoryRegistry;
import org.hl7.fhir.dstu3.model.Bundle;
import org.hl7.fhir.dstu3.model.Coverage;
import org.hl7.fhir.dstu3.model.ExplanationOfBenefit;
import org.hl7.fhir.dstu3.model.Patient;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Integration tests that verify that the data in the {@link StaticRifResourceGroup#SYNTHEA} set can
 * be transformed to FHIR as expected.
 */
public final class SyntheaDataToFhirIT {
  /**
   * Verifies that all of the data in the {@link StaticRifResourceGroup#SYNTHEA} set can be queried
   * across all resource types without error.
   *
   * @throws FHIRException (indicates test failure)
   */
  @Test
  @Category(SlowTests.class)
  public void checkThatSyntheaDataDoesntGoBoom() throws FHIRException {
    List<Object> loadedRecords =
        ServerTestUtils.loadData(Arrays.asList(StaticRifResourceGroup.SYNTHEA.getResources()));
    IGenericClient fhirClient = ServerTestUtils.createFhirClient();
    fhirClient.setEncoding(EncodingEnum.JSON);
    ExtraParamsInterceptor extraParamsInterceptor = new ExtraParamsInterceptor();
    extraParamsInterceptor.setIncludeIdentifiers("true");
    fhirClient.registerInterceptor(extraParamsInterceptor);

    // Save all of the responses for some extra checks at the end.
    List<Patient> patients = new ArrayList<>();
    List<Coverage> coverages = new ArrayList<>();
    List<ExplanationOfBenefit> eobs = new ArrayList<>();

    // Loop over each source bene, and check all of the resource types for it.
    List<Beneficiary> beneficiaries =
        loadedRecords.stream()
            .filter(r -> r instanceof Beneficiary)
            .map(r -> (Beneficiary) r)
            .collect(Collectors.toList());
    for (Beneficiary beneficiary : beneficiaries) {
      try {
        // Check the Patient resource conversion.
        Patient patient =
            fhirClient
                .read()
                .resource(Patient.class)
                .withId(beneficiary.getBeneficiaryId())
                .execute();
        Assert.assertNotNull(patient);
        String patientId = patient.getIdElement().getIdPart();
        writeToFile(patient, String.format("patient-%s.json", patientId));
        patients.add(patient);

        // Check the Coverage resource conversion.
        Bundle coverageBundle =
            fhirClient
                .search()
                .forResource(Coverage.class)
                .where(Coverage.BENEFICIARY.hasId(TransformerUtils.buildPatientId(beneficiary)))
                .returnBundle(Bundle.class)
                .execute();
        Assert.assertNotNull(coverageBundle);
        writeToFile(coverageBundle, String.format("coverages-%s.json", patientId));
        Assert.assertEquals(MedicareSegment.values().length, coverageBundle.getTotal());
        coverages.addAll(
            coverageBundle.getEntry().stream()
                .map(r -> ((Coverage) r.getResource()))
                .collect(Collectors.toList()));

        // Check the ExplanationOfBenefit resource conversion.
        Bundle eobBundle =
            fhirClient
                .search()
                .forResource(ExplanationOfBenefit.class)
                .where(
                    ExplanationOfBenefit.PATIENT.hasId(
                        TransformerUtils.buildPatientId(beneficiary)))
                .returnBundle(Bundle.class)
                .execute();
        Assert.assertNotNull(eobBundle);
        writeToFile(eobBundle, String.format("eobs-%s.json", patientId));
        Assert.assertEquals(
            loadedRecords.stream()
                .filter(r -> filterToClaimsForBeneficiary(beneficiary, r))
                .count(),
            eobBundle.getTotal());
        eobs.addAll(
            eobBundle.getEntry().stream()
                .map(r -> ((ExplanationOfBenefit) r.getResource()))
                .collect(Collectors.toList()));
      } catch (InternalErrorException e) {
        throw new InternalErrorException(
            String.format(
                "Server error rendering FHIR for beneficiary '%s'.",
                beneficiary.getBeneficiaryId()),
            e);
      }
    }

    /*
     * Verify that at least _some_ of the EOBs have some things that are expected, but won't be
     * there 100% of the time.
     *
     * Note that CI runs without Synthea mapping files, this results in Synthea output with missing
     * diagnoses and procedures so we are unable to check for their presence.
     *
     * Synthea exported providers are not loaded so we are unable to check for display names or
     * other details.
     */
    Assert.assertTrue(
        "No line item services found.",
        eobs.stream()
            .flatMap(eob -> eob.getItem().stream())
            .anyMatch(item -> item.getService() != null));
  }

  /**
   * @param beneficiary the {@link Beneficiary} to check for
   * @param rifRecord the {@link CarrierClaim}, {@link PartDEvent}, etc. to check
   * @return <code>true</code> if the specified RIF record is a claim/event for the specified {@link
   *     Beneficiary}, <code>false</code> otherwise
   */
  private static boolean filterToClaimsForBeneficiary(Beneficiary beneficiary, Object rifRecord) {
    // First, verify that it's a claim at all.
    Optional<ClaimType> claimType = ClaimType.findClaimTypeForEntity(rifRecord);
    if (!claimType.isPresent()) return false;

    // Next, check to see if the claim's bene matches.
    Member beneIdMember = claimType.get().getEntityBeneficiaryIdAttribute().getJavaMember();
    if (beneIdMember instanceof Field) {
      // We're gonna' use reflection here to grab the bene ID from the claim, which is a mess.
      Field beneIdField = (Field) beneIdMember;
      try {
        Object beneIdRaw = beneIdField.get(rifRecord);
        if (!(beneIdRaw instanceof String))
          // All bene IDs are currently Strings.
          throw new BadCodeMonkeyException();

        String beneId = (String) beneIdRaw;

        // FINALLY, we have the bene ID, so now we can check to see if it matches.
        return beneficiary.getBeneficiaryId().equals(beneId);
      } catch (IllegalArgumentException e) {
        throw new BadCodeMonkeyException(e);
      } catch (IllegalAccessException e) {
        throw new BadCodeMonkeyException(e);
      }
    } else {
      // All of the ClaimType attribs should be Fields, not Methods.
      throw new BadCodeMonkeyException();
    }
  }

  /**
   * @param fhirResource the FHIR resource to write out to a file
   * @param fileName the name of the file to write the resource out to
   */
  private static void writeToFile(IBaseResource fhirResource, String fileName) {
    Path appsTarget = SharedTestUtils.getBuildRootTargetDirectory();
    Path outputDir =
        appsTarget.resolve("bfd-server-war").resolve("synthea-synthetic-data-rendered-as-fhir");
    try {
      Files.createDirectories(outputDir);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    IParser hapiParser = FhirContext.forDstu3().newJsonParser();
    hapiParser.setPrettyPrint(true);
    String fhirResourceJson = hapiParser.encodeResourceToString(fhirResource);
    try (PrintStream out =
        new PrintStream(new FileOutputStream(outputDir.resolve(fileName).toFile()))) {
      out.print(fhirResourceJson);
    } catch (FileNotFoundException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** Ensures that {@link ServerTestUtils#cleanDatabaseServer()} is called after each test case. */
  @After
  public void cleanDatabaseServerAfterEachTestCase() {
    ServerTestUtils.cleanDatabaseServer();
    // FIXME temporary workaround to free up ram
    SessionFactoryRegistry.INSTANCE.clearRegistrations();
  }
}
