package gov.cms.bfd.server.war.r4.providers.pac.common;

import gov.cms.bfd.model.rda.entities.RdaFissClaim;
import gov.cms.bfd.model.rda.entities.RdaFissPayer;
import java.util.Map;
import java.util.Optional;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;

/** Class for performing common FISS based transformation logic. */
public class FissTransformerV2 {

  /** The FISS specific sex mapping to use to map from RDA to FHIR. */
  private static final Map<String, Enumerations.AdministrativeGender> SEX_MAP =
      Map.of(
          "m", Enumerations.AdministrativeGender.MALE,
          "f", Enumerations.AdministrativeGender.FEMALE,
          "u", Enumerations.AdministrativeGender.UNKNOWN);

  /** Instantiates a new Fiss transformer v2. */
  private FissTransformerV2() {}

  /**
   * Creates a {@link Patient} object using the given {@link RdaFissClaim} information.
   *
   * @param claimGroup The {@link RdaFissClaim} information to use to build the {@link Patient}
   *     object.
   * @param sexExtensionEnabled whether the sex extension is enabled.
   * @return The constructed {@link Patient} object.
   */
  public static Resource getContainedPatient(RdaFissClaim claimGroup, boolean sexExtensionEnabled) {
    Optional<RdaFissPayer> benePayerOptional =
        claimGroup.getPayers().stream()
            .filter(p -> p.getPayerType() == RdaFissPayer.PayerType.BeneZ)
            .findFirst();

    Patient patient;

    if (benePayerOptional.isPresent()) {
      RdaFissPayer benePayer = benePayerOptional.get();

      patient =
          AbstractTransformerV2.getContainedPatient(
              claimGroup.getMbi(),
              new AbstractTransformerV2.PatientInfo(
                  benePayer.getBeneFirstName(),
                  benePayer.getBeneLastName(),
                  AbstractTransformerV2.ifNotNull(
                      benePayer.getBeneMidInit(), s -> s.charAt(0) + "."),
                  benePayer.getBeneDob(),
                  benePayer.getBeneSex(),
                  SEX_MAP,
                  "max 10 chars of first",
                  "middle initial",
                  "max 15 chars of last"),
              sexExtensionEnabled);
    } else {
      patient =
          AbstractTransformerV2.getContainedPatient(claimGroup.getMbi(), null, sexExtensionEnabled);
    }

    return patient;
  }
}
