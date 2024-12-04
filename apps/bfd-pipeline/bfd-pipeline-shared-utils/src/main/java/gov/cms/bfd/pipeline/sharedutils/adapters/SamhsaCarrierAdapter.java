package gov.cms.bfd.pipeline.sharedutils.adapters;

import static java.util.Map.entry;

import gov.cms.bfd.model.rif.entities.CarrierClaim;
import gov.cms.bfd.model.rif.entities.CarrierClaimLine;
import gov.cms.bfd.model.rif.samhsa.CarrierTag;
import gov.cms.bfd.pipeline.sharedutils.SamhsaUtil;
import gov.cms.bfd.pipeline.sharedutils.model.SamhsaFields;
import gov.cms.bfd.pipeline.sharedutils.model.TagCode;
import gov.cms.bfd.pipeline.sharedutils.model.TagDetails;
import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/** Adapter to get a list of SAMHSA codes from a CarrierClaim. */
public class SamhsaCarrierAdapter extends SamhsaAdapterBase<CarrierClaim, CarrierClaimLine> {

  /**
   * Constructor.
   *
   * @param claim The claim to process.
   */
  public SamhsaCarrierAdapter(CarrierClaim claim) {
    super(claim, claim.getLines());
    this.claim = claim;
    this.table = "carrier_claims";
    this.linesTable = "carrier_claim_lines";
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
        entry(claim::getDiagnosisPrincipalCode, "prncpal_dgns_cd"),
        entry(claim::getDiagnosis1Code, "icd_dgns_cd1"),
        entry(claim::getDiagnosis2Code, "icd_dgns_cd2"),
        entry(claim::getDiagnosis3Code, "icd_dgns_cd3"),
        entry(claim::getDiagnosis4Code, "icd_dgns_cd4"),
        entry(claim::getDiagnosis5Code, "icd_dgns_cd5"),
        entry(claim::getDiagnosis6Code, "icd_dgns_cd6"),
        entry(claim::getDiagnosis7Code, "icd_dgns_cd7"),
        entry(claim::getDiagnosis8Code, "icd_dgns_cd8"),
        entry(claim::getDiagnosis9Code, "icd_dgns_cd9"),
        entry(claim::getDiagnosis10Code, "icd_dgns_cd10"),
        entry(claim::getDiagnosis11Code, "icd_dgns_cd11"),
        entry(claim::getDiagnosis12Code, "icd_dgns_cd12"));
  }

  /** {@inheritDoc} */
  @Override
  Short getLineNum(CarrierClaimLine carrierClaimLine) {
    return carrierClaimLine.getLineNumber();
  }

  /** {@inheritDoc} */
  @Override
  public Map<Supplier<Optional<String>>, String> getClaimLineMethods(CarrierClaimLine line) {
    return Map.ofEntries(
        entry(line::getDiagnosisCode, "line_icd_dgns_cd"), entry(line::getHcpcsCode, "hcpcs_cd"));
  }

  /** {@inheritDoc} */
  @Override
  public boolean checkAndProcessClaim(EntityManager entityManager)
      throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {

    Optional<List<TagDetails>> entries = buildDetails();
    if (entries.isPresent()) {
      List<CarrierTag> tags = new ArrayList<>();
      tags.add(
          CarrierTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode._42CFRPart2.toString())
              .details(entries.get())
              .build());
      tags.add(
          CarrierTag.builder()
              .claim(claim.getClaimId())
              .code(TagCode.R.toString())
              .details(entries.get())
              .build());
      return SamhsaUtil.persistTags(Optional.of(tags), entityManager);
    }
    return false;
  }
}
