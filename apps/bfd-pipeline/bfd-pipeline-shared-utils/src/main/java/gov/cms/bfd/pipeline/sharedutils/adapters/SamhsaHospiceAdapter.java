package gov.cms.bfd.pipeline.sharedutils.adapters;

import static java.util.Map.entry;

import gov.cms.bfd.model.rif.entities.HospiceClaim;
import gov.cms.bfd.model.rif.entities.HospiceClaimLine;
import gov.cms.bfd.model.rif.samhsa.HospiceTag;
import gov.cms.bfd.pipeline.sharedutils.SamhsaUtil;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;
import gov.cms.bfd.pipeline.sharedutils.model.TagDetails;
import gov.cms.bfd.sharedutils.TagCode;
import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Adapter to get a list of SAMHSA codes from a Hospice. */
public class SamhsaHospiceAdapter extends SamhsaAdapterBase<HospiceClaim, HospiceClaimLine> {
  /**
   * Constructor.
   *
   * @param claim The claim to process.
   */
  public SamhsaHospiceAdapter(HospiceClaim claim) {
    super(claim, claim.getLines());
    this.claim = claim;
    this.table = "hospice_claims";
    this.linesTable = "hospice_claim_lines";
  }

  /** Retrieves a list of SAMHSA fields. */
  @Override
  public List<SamhsaFields> getFields() {
    getCodes();
    return samhsaFields;
  }

  /** {@inheritDoc} */
  @Override
  Short getLineNum(HospiceClaimLine hospiceClaimLine) {
    return hospiceClaimLine.getLineNumber();
  }

  /** {@inheritDoc} */
  @Override
  Map<Supplier<Optional<String>>, String> getClaimMethods() {
    return Map.ofEntries(
        entry(claim::getDiagnosisPrincipalCode, PRNCPAL_DGNS_CD),
        entry(claim::getDiagnosis1Code, ICD_DGNS_CD_1),
        entry(claim::getDiagnosis2Code, ICD_DGNS_CD_2),
        entry(claim::getDiagnosis3Code, ICD_DGNS_CD_3),
        entry(claim::getDiagnosis4Code, ICD_DGNS_CD_4),
        entry(claim::getDiagnosis5Code, ICD_DGNS_CD_5),
        entry(claim::getDiagnosis6Code, ICD_DGNS_CD_6),
        entry(claim::getDiagnosis7Code, ICD_DGNS_CD_7),
        entry(claim::getDiagnosis8Code, ICD_DGNS_CD_8),
        entry(claim::getDiagnosis9Code, ICD_DGNS_CD_9),
        entry(claim::getDiagnosis10Code, ICD_DGNS_CD_10),
        entry(claim::getDiagnosis11Code, ICD_DGNS_CD_11),
        entry(claim::getDiagnosis12Code, ICD_DGNS_CD_12),
        entry(claim::getDiagnosis13Code, ICD_DGNS_CD_13),
        entry(claim::getDiagnosis14Code, ICD_DGNS_CD_14),
        entry(claim::getDiagnosis15Code, ICD_DGNS_CD_15),
        entry(claim::getDiagnosis16Code, ICD_DGNS_CD_16),
        entry(claim::getDiagnosis17Code, ICD_DGNS_CD_17),
        entry(claim::getDiagnosis18Code, ICD_DGNS_CD_18),
        entry(claim::getDiagnosis19Code, ICD_DGNS_CD_19),
        entry(claim::getDiagnosis20Code, ICD_DGNS_CD_20),
        entry(claim::getDiagnosis21Code, ICD_DGNS_CD_21),
        entry(claim::getDiagnosis22Code, ICD_DGNS_CD_22),
        entry(claim::getDiagnosis23Code, ICD_DGNS_CD_23),
        entry(claim::getDiagnosis24Code, ICD_DGNS_CD_24),
        entry(claim::getDiagnosis25Code, ICD_DGNS_CD_25),
        entry(claim::getDiagnosisExternal1Code, ICD_DGNS_E_CD_1),
        entry(claim::getDiagnosisExternal2Code, ICD_DGNS_E_CD_2),
        entry(claim::getDiagnosisExternal3Code, ICD_DGNS_E_CD_3),
        entry(claim::getDiagnosisExternal4Code, ICD_DGNS_E_CD_4),
        entry(claim::getDiagnosisExternal5Code, ICD_DGNS_E_CD_5),
        entry(claim::getDiagnosisExternal6Code, ICD_DGNS_E_CD_6),
        entry(claim::getDiagnosisExternal7Code, ICD_DGNS_E_CD_7),
        entry(claim::getDiagnosisExternal8Code, ICD_DGNS_E_CD_8),
        entry(claim::getDiagnosisExternal9Code, ICD_DGNS_E_CD_9),
        entry(claim::getDiagnosisExternal10Code, ICD_DGNS_E_CD_10),
        entry(claim::getDiagnosisExternal11Code, ICD_DGNS_E_CD_11),
        entry(claim::getDiagnosisExternal12Code, ICD_DGNS_E_CD_12),
        entry(claim::getDiagnosisExternalFirstCode, FST_DGNS_E_CD));
  }

  /** {@inheritDoc} */
  @Override
  Map<Supplier<Optional<String>>, String> getClaimLineMethods(HospiceClaimLine hospiceClaimLine) {
    return Map.ofEntries(entry(hospiceClaimLine::getHcpcsCode, HCPCS_CD));
  }

  /** {@inheritDoc} */
  @Override
  public boolean checkAndProcessClaim(EntityManager entityManager)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {

    Optional<List<TagDetails>> entries = buildDetails();
    if (entries.isPresent()) {
      List<HospiceTag> tags = new ArrayList<>();
      tags.add(
          HospiceTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode._42CFRPart2.toString())
              .details(entries.get())
              .build());
      tags.add(
          HospiceTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode.R.toString())
              .details(entries.get())
              .build());
      return SamhsaUtil.persistTags(Optional.of(tags), entityManager);
    }
    return false;
  }
}
