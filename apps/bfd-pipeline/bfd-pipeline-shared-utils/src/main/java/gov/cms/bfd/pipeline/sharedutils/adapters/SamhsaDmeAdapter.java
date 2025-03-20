package gov.cms.bfd.pipeline.sharedutils.adapters;

import static java.util.Map.entry;

import gov.cms.bfd.model.rif.entities.DMEClaim;
import gov.cms.bfd.model.rif.entities.DMEClaimLine;
import gov.cms.bfd.model.rif.samhsa.DmeTag;
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

/** Adapter to get a list of SAMHSA codes from a DMEClaim. */
public class SamhsaDmeAdapter extends SamhsaAdapterBase<DMEClaim, DMEClaimLine> {

  /**
   * Constructor.
   *
   * @param claim The claim to process.
   */
  public SamhsaDmeAdapter(DMEClaim claim) {
    super(claim, claim.getLines());
    this.claim = claim;
    this.table = "dme_claims";
    this.linesTable = "dme_claim_lines";
  }

  /**
   * Retrieves a list of SAMHSA fields.
   *
   * @return {@link SamhsaFields}
   */
  @Override
  public List<SamhsaFields> getFields() {
    getCodes();
    return samhsaFields;
  }

  /** {@inheritDoc} */
  @Override
  public Map<Supplier<Optional<String>>, String> getClaimMethods() {
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
        entry(claim::getDiagnosis12Code, ICD_DGNS_CD_12));
  }

  /** {@inheritDoc} */
  @Override
  Short getLineNum(DMEClaimLine dmeClaimLine) {
    return dmeClaimLine.getLineNumber();
  }

  /** {@inheritDoc} */
  @Override
  public Map<Supplier<Optional<String>>, String> getClaimLineMethods(DMEClaimLine line) {
    return Map.ofEntries(
        entry(line::getDiagnosisCode, LINE_ICD_DGNS_CD), entry(line::getHcpcsCode, HCPCS_CD));
  }

  /** {@inheritDoc} */
  @Override
  public boolean checkAndProcessClaim(EntityManager entityManager)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {

    Optional<List<TagDetails>> entries = buildDetails();
    if (entries.isPresent()) {
      List<DmeTag> tags = new ArrayList<>();
      tags.add(
          DmeTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode._42CFRPart2.toString())
              .details(entries.get())
              .build());
      tags.add(
          DmeTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode.R.toString())
              .details(entries.get())
              .build());
      return SamhsaUtil.persistTags(Optional.of(tags), entityManager);
    }
    return false;
  }
}
