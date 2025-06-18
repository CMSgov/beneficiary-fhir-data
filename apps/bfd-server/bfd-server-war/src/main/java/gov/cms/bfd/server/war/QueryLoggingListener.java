package gov.cms.bfd.server.war;

import gov.cms.bfd.server.sharedutils.BfdMDC;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** This {@link QueryExecutionListener} records query performance data in {@link BfdMDC}. */
public final class QueryLoggingListener implements QueryExecutionListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(QueryLoggingListener.class);

  /** Used to compute various MDC keys. */
  private static final String MDC_KEY_PREFIX = "database_query";

  /** {@inheritDoc} */
  @Override
  public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    /*
     * Note: Somewhat surprisingly -- and fortuitously -- this event gets fired on
     * whichever thread called into JPA and/or the DataSource. This means that the
     * MDC entries added here will be attached to the HTTP response threads (when
     * relevant), and thus included in the access log.
     */

    if (queryInfoList.isEmpty()) return;

    /*
     * Most of the time, we don't want to include the full SQL queries as they add a tremendous
     * amount of bloat to the logs. But, sometimes we do...
     */
    boolean logFullQuery = false;
    if (LOGGER.isTraceEnabled()) {
      logFullQuery = true;
    }

    String mdcKeyPrefix;
    if (queryInfoList.size() == 1) {
      QueryType queryType = QueryType.computeQueryType(queryInfoList.get(0));
      mdcKeyPrefix = queryType.getQueryTypeId();

      if (queryType == QueryType.UNKNOWN) {
        logFullQuery = true;
      }
      if (execInfo.getElapsedTime() >= 1000) {
        logFullQuery = true;
      }

      if (logFullQuery)
        BfdMDC.put(
            BfdMDC.computeMDCKey(MDC_KEY_PREFIX, mdcKeyPrefix, "query"),
            queryInfoList.get(0).getQuery());
    } else {
      mdcKeyPrefix = "group";
      logFullQuery = true;

      StringBuilder queryIds = new StringBuilder();
      if (queryInfoList.size() > 1) queryIds.append('[');
      for (QueryInfo queryInfo : queryInfoList) {
        queryIds.append(QueryType.computeQueryType(queryInfo).getQueryTypeId());
        queryIds.append(',');
      }
      if (queryIds.charAt(queryIds.length() - 1) == ',')
        queryIds.deleteCharAt(queryIds.length() - 1);
      if (queryInfoList.size() > 1) queryIds.append(']');
      BfdMDC.put(BfdMDC.computeMDCKey(MDC_KEY_PREFIX, mdcKeyPrefix, "ids"), queryIds.toString());

      StringBuilder queries = new StringBuilder();
      if (queryInfoList.size() > 1) queries.append('[');
      for (QueryInfo queryInfo : queryInfoList) {
        queries.append('[');
        queries.append(queryInfo.getQuery());
        queries.append("],");
      }
      if (queries.charAt(queries.length() - 1) == ',') queries.deleteCharAt(queries.length() - 1);
      if (queryInfoList.size() > 1) queries.append(']');
      BfdMDC.put(BfdMDC.computeMDCKey(MDC_KEY_PREFIX, mdcKeyPrefix, "queries"), queries.toString());
    }
    BfdMDC.put(
        BfdMDC.computeMDCKey(MDC_KEY_PREFIX, mdcKeyPrefix, "size"),
        String.valueOf(queryInfoList.size()));

    BfdMDC.put(
        BfdMDC.computeMDCKey(MDC_KEY_PREFIX, mdcKeyPrefix, "duration_milliseconds"),
        String.valueOf(execInfo.getElapsedTime()));
    BfdMDC.put(
        BfdMDC.computeMDCKey(MDC_KEY_PREFIX, mdcKeyPrefix, "success"),
        String.valueOf(execInfo.isSuccess()));
    BfdMDC.put(
        BfdMDC.computeMDCKey(MDC_KEY_PREFIX, mdcKeyPrefix, "type"),
        execInfo.getStatementType().name());
    BfdMDC.put(
        BfdMDC.computeMDCKey(MDC_KEY_PREFIX, mdcKeyPrefix, "batch"),
        String.valueOf(execInfo.isBatch()));
    BfdMDC.put(
        BfdMDC.computeMDCKey(MDC_KEY_PREFIX, mdcKeyPrefix, "batch_size"),
        String.valueOf(execInfo.getBatchSize()));
    BfdMDC.put(
        BfdMDC.computeMDCKey(MDC_KEY_PREFIX, mdcKeyPrefix, "datasource_name"),
        execInfo.getDataSourceName());
  }

  /** {@inheritDoc} */
  @Override
  public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
    // Nothing to do here.
  }

  /** Enumerates the various query types. */
  static enum QueryType {
    /** Represents the query for beneficiary by id (no hicn/mbi returned). */
    BENE_BY_ID_OMIT_IDENTIFIERS(
        "bene_by_id_omit_hicns_and_mbis",
        (s ->
            s.contains(" from ccw.beneficiaries ")
                && s.contains("bene_id=")
                && !s.contains(" join ")
                && !s.contains("bene_crnt_hic_num="))),
    /** Represents the query for beneficiary by id. */
    BENE_BY_ID_INCLUDE_IDENTIFIERS(
        "bene_by_id_include_hicns_and_mbis",
        (s ->
            s.contains(" from ccw.beneficiaries ")
                && s.contains("bene_id=")
                && s.contains(" join ")
                && !s.contains("bene_crnt_hic_num="))),
    /** Represents the query for beneficiary by mbi (via bene history). */
    BENE_BY_MBI_HISTORY(
        "bene_by_mbi_mbis_from_beneficiarieshistory",
        (s -> s.contains(" from ccw.beneficiaries_history ") && s.contains("mbi_hash="))),
    /** Represents the query for beneficiary by hicn (via bene history). */
    BENE_BY_HICN_HISTORY(
        "bene_by_hicn_hicns_from_beneficiarieshistory",
        (s -> s.contains(" from ccw.beneficiaries_history ") && s.contains("bene_crnt_hic_num="))),
    /** Represents the query for beneficiary by hicn or id (no hicn/mbi returned). */
    BENE_BY_HICN_OR_ID_OMIT_IDENTIFIERS(
        "bene_by_hicn_bene_by_hicn_or_id_omit_hicns_and_mbis",
        (s ->
            s.contains(" from ccw.beneficiaries ")
                && !s.contains(" join ")
                && s.contains("bene_crnt_hic_num="))),
    /** Represents the query for beneficiary by hicn or id. */
    BENE_BY_HICN_OR_ID_INCLUDE_IDENTIFIERS(
        "bene_by_hicn_bene_by_hicn_or_id_include_hicns_and_mbis",
        (s ->
            s.contains(" from ccw.beneficiaries ")
                && s.contains(" join ")
                && s.contains("bene_crnt_hic_num="))),
    /** Represents the query for beneficiary by coverage contract. */
    BENE_BY_COVERAGE(
        "bene_by_coverage",
        (s ->
            s.contains(" from ccw.beneficiaries ")
                && s.contains("where beneficiar0_.ptd_cntrct_"))),
    /** Represents the query for EOB by bene id (carrier). */
    EOBS_BY_BENE_ID_CARRIER(
        "eobs_by_bene_id_carrier", (s -> s.contains(" from ccw.carrier_claims "))),
    /** Represents the query for EOB by bene id (DME). */
    EOBS_BY_BENE_ID_DME("eobs_by_bene_id_dme", (s -> s.contains(" from ccw.dme_claims "))),
    /** Represents the query for EOB by bene id (HHA). */
    EOBS_BY_BENE_ID_HHA("eobs_by_bene_id_hha", (s -> s.contains(" from ccw.hha_claims "))),
    /** Represents the query for EOB by bene id (hospice). */
    EOBS_BY_BENE_ID_HOSPICE(
        "eobs_by_bene_id_hospice", (s -> s.contains(" from ccw.hospice_claims "))),
    /** Represents the query for EOB by bene id (inpatient). */
    EOBS_BY_BENE_ID_INPATIENT(
        "eobs_by_bene_id_inpatient", (s -> s.contains(" from ccw.inpatient_claims "))),
    /** Represents the query for EOB by bene id (outpatient). */
    EOBS_BY_BENE_ID_OUTPATIENT(
        "eobs_by_bene_id_outpatient", (s -> s.contains(" from ccw.outpatient_claims "))),
    /** Represents the query for EOB by bene id (partD). */
    EOBS_BY_BENE_ID_PDE("eobs_by_bene_id_pde", (s -> s.contains(" from ccw.partd_events "))),
    /** Represents the query for EOB by bene id (SNF). */
    EOBS_BY_BENE_ID_SNF("eobs_by_bene_id_snf", (s -> s.contains(" from ccw.snf_claims "))),
    /** Represents the query for partially adjudicated claims (fiss). */
    FISS_CLAIMS("partially_adjudicated_fiss", s -> s.contains("from rda.fiss_claims")),
    /** Represents the query for fiss diagnosis codes (fiss). */
    FISS_DIAGNOSIS_CODES("fiss_diagnosis_codes", s -> s.contains("from rda.fiss_diagnosis_codes")),
    /** Represents the query for FISS audit trails. */
    FISS_AUDIT_TRAILS("fiss_audit_trails", s -> s.contains("from rda.fiss_audit_trails")),
    /** Represents the query for FISS payers. */
    FISS_PAYERS("fiss_payers", s -> s.contains("from rda.fiss_payers")),
    /** Represents the query for FISS procedure codes. */
    FISS_PROC_CODES("fiss_proc_codes", s -> s.contains("from rda.fiss_proc_codes")),
    /** Represents the query for FISS revenue lines. */
    FISS_REVENUE_LINES("fiss_revenue_lines", s -> s.contains("from rda.fiss_revenue_lines")),
    /** Represents the query for partially adjudicated claims (mcs). */
    MCS_CLAIMS("partially_adjudicated_mcs", s -> s.contains("from rda.mcs_claims")),
    /** Represents the query for mcs diagnosis codes (mcs). */
    MCS_DIAGNOSIS_CODES("mcs_diagnosis_codes", s -> s.contains("from rda.mcs_diagnosis_codes")),
    /** Represents the query for MCS adjustments. */
    MCS_ADJUSTMENTS("mcs_adjustments", s -> s.contains("from rda.mcs_adjustments")),
    /** Represents the query for MCS audits. */
    MCS_AUDITS("mcs_audits", s -> s.contains("from rda.mcs_audits")),
    /** Represents the query for MCS details. */
    MCS_DETAILS("mcs_details", s -> s.contains("from rda.mcs_details")),
    /** Represents the query for MCS locations. */
    MCS_LOCATIONS("mcs_locations", s -> s.contains("from rda.mcs_locations")),
    /** Represents the query for mbi cache lookup. */
    MBI_CACHE("mbi_cache_lookup", s -> s.contains("from rda.mbi_cache")),
    /** Represents the query for loaded batches. */
    LOADED_BATCH("loaded_batch", (s -> s.contains(" from ccw.loaded_batches "))),
    /** Represents the query for loaded files. */
    LOADED_FILE("loaded_file", (s -> s.contains(" from ccw.loaded_files "))),
    /** Represents a query from ccw.fda_data. */
    FDA_DATA("fda_data", s -> s.contains(" from ccw.fda_data ")),
    /** Represents a query from rda.mcs_tags. */
    RDA_MCS_TAGS("rda_mcs_tags", s -> s.contains(" from rda.mcs_tags ")),
    /** Represents a query from rda.fiss_tags. */
    RDA_FISS_TAGS("rda_fiss_tags", s -> s.contains(" from rda.fiss_tags ")),
    /** Represents a query from ccw.dme_tags. */
    DME_TAGS("dme_tags", s -> s.contains(" from ccw.dme_tags ")),
    /** Represents a query from ccw.hha_tags. */
    HHA_TAGS("hha_tags", s -> s.contains(" from ccw.hha_tags ")),
    /** Represents a query from ccw.carrier_tags. */
    CARRIER_TAGS("carrier_tags", s -> s.contains(" from ccw.carrier_tags ")),
    /** Represents a query from ccw.hospice_tags. */
    HOSPICE_TAGS("hospice_tags", s -> s.contains(" from ccw.hospice_tags ")),
    /** Represents a query from ccw.inpatient_tags. */
    INPATIENT_TAGS("inpatient_tags", s -> s.contains(" from ccw.inpatient_tags ")),
    /** Represents a query from ccw.outpatient_tags. */
    OUTPATIENT_TAGS("outpatient_tags", s -> s.contains(" from ccw.outpatient_tags ")),
    /** Represents a query from ccw.snf_tags. */
    SNF_TAGS("snf_tags", s -> s.contains(" from ccw.snf_tags ")),
    /** Represents a query from ccw.npi_fda_meta. */
    NPI_FDA_META("npi_fda_meta", s -> s.contains(" from ccw.npi_fda_meta ")),

    /**
     * Represents the query for checking if a beneficiary exists given the partD contract id and
     * year month.
     */
    BENE_EXISTS_BY_YEAR_MONTH_PARTD_CONTRACT_ID(
        "bene_exists_by_year_month_part_d_contract_id",
        (s ->
            s.contains(" from ccw.beneficiary_monthly ")
                && s.contains("year_month=")
                && s.contains("partd_contract_number_id="))),

    /** Represents query that invokes the check_claims_mask function. */
    CHECK_CLAIMS_MASK("check_claims_mask", (s -> s.contains("check_claims_mask"))),
    /**
     * Represents Hibernate PostgreSQL dialect's probing for sequences in the associated database.
     * This query is automatically executed at least once when establishing a database connection.
     */
    HIBERNATE_INFORMATION_SCHEMA_SEQUENCES(
        "hibernate_information_schema_sequences",
        (s -> s.contains(" from information_schema.sequences"))),
    /** Represents query that invokes the find_beneficiary function. */
    FIND_BENEFICIARY_FUNCTION("find_beneficiary", (s -> s.contains("find_beneficiary"))),
    /** Represents a query from npi_data. */
    NPI_DATA("npi_data", (s -> s.contains("from ccw.npi_data"))),
    /** Represents an unknown query (one not explicitly defined in this list). */
    UNKNOWN("unknown", null);

    /** A unique identifier for this {@link QueryType}, suitable for use in logs and such. */
    private final String id;

    /**
     * The {@link Predicate} that should return <code>true</code> if a given {@link
     * QueryInfo#getQuery()} represents this {@link QueryType}.
     */
    private final Predicate<String> queryTextRegex;

    /**
     * Constructs a new QueryType.
     *
     * @param id the value to use for {@link #getQueryTypeId()}
     * @param queryTextRegex the {@link Predicate} that should return <code>true</code> if a given
     *     {@link QueryInfo#getQuery()} represents this {@link QueryType}
     */
    private QueryType(String id, Predicate<String> queryTextRegex) {
      this.id = id;
      this.queryTextRegex = queryTextRegex;
    }

    /**
     * Gets the {@link #id}.
     *
     * @return a unique identifier for this {@link QueryType}, suitable for use in logs and such
     */
    public String getQueryTypeId() {
      return id;
    }

    /**
     * Computes the query type based on the {@link QueryInfo}.
     *
     * @param queryInfo the {@link QueryInfo} to compute a {@link QueryType} for
     * @return the {@link QueryType} that matches the specified {@link QueryInfo}, or {@link
     *     #UNKNOWN} if no match could be determined
     */
    public static QueryType computeQueryType(QueryInfo queryInfo) {
      List<QueryType> matchingQueryTypes = new LinkedList<>();
      for (QueryType queryType : values()) {
        if (queryType.queryTextRegex == null) continue;

        if (queryType.queryTextRegex.test(queryInfo.getQuery())) matchingQueryTypes.add(queryType);
      }

      if (matchingQueryTypes.size() == 1) return matchingQueryTypes.get(0);
      else if (matchingQueryTypes.size() > 1)
        LOGGER.warn(
            "Too many matching query types '{}' for query: {}",
            matchingQueryTypes,
            queryInfo.getQuery());
      else LOGGER.warn("No matching query type for query: {}", queryInfo.getQuery());

      return UNKNOWN;
    }
  }
}
