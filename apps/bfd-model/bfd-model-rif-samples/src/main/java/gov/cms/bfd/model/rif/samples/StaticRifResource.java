package gov.cms.bfd.model.rif.samples;

import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileType;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

/** Enumerates the sample RIF resources available on the classpath. */
public enum StaticRifResource {
  /** Sample A Beneficiary static rif. */
  SAMPLE_A_BENES(
      resourceUrl("rif-static-samples/sample-a-beneficiaries.txt"), RifFileType.BENEFICIARY, 1),

  /** Sample A for BB2 Beneficiary static rif. */
  SAMPLE_A4BB2_BENES(
      resourceUrl("rif-static-samples/sample-a4bb2-beneficiaries.txt"), RifFileType.BENEFICIARY, 1),

  /** Sample A Beneficiary without a reference year static rif. */
  SAMPLE_A_BENES_WITHOUT_REFERENCE_YEAR(
      resourceUrl("rif-static-samples/sample-a-beneficiaries-without-reference-year.txt"),
      RifFileType.BENEFICIARY,
      1),

  /** Sample A Beneficiary with a backslash in the data static rif. */
  SAMPLE_A_BENES_WITH_BACKSLASH(
      resourceUrl("rif-static-samples/sample-a-beneficiaries-with-backslash.txt"),
      RifFileType.BENEFICIARY,
      1),

  /** Sample A Beneficiary History static rif. */
  SAMPLE_A_BENEFICIARY_HISTORY(
      resourceUrl("rif-static-samples/sample-a-beneficiaryhistory.txt"),
      RifFileType.BENEFICIARY_HISTORY,
      5),

  /** Sample A for BB2 Beneficiary History static rif. */
  SAMPLE_A4BB2_BENEFICIARY_HISTORY(
      resourceUrl("rif-static-samples/sample-a4bb2-beneficiaryhistory.txt"),
      RifFileType.BENEFICIARY_HISTORY,
      5),

  /** Sample A Carrier static rif. */
  SAMPLE_A_CARRIER(resourceUrl("rif-static-samples/sample-a-bcarrier.txt"), RifFileType.CARRIER, 1),

  /** Sample A for BB2 Carrier static rif. */
  SAMPLE_A4BB2_CARRIER(
      resourceUrl("rif-static-samples/sample-a4bb2-bcarrier.txt"), RifFileType.CARRIER, 1),

  /** Sample A Carrier static rif. */
  SAMPLE_A_CARRIER_MULTIPLE_LINES(
      resourceUrl("rif-static-samples/sample-a-bcarrier-multiple-lines.txt"),
      RifFileType.CARRIER,
      7),

  /** Sample A Inpatient static rif. */
  SAMPLE_A_INPATIENT(
      resourceUrl("rif-static-samples/sample-a-inpatient.txt"), RifFileType.INPATIENT, 1),

  /** Sample A for BB2 Inpatient static rif. */
  SAMPLE_A4BB2_INPATIENT(
      resourceUrl("rif-static-samples/sample-a4bb2-inpatient.txt"), RifFileType.INPATIENT, 1),

  /** Sample A Inpatient four character drg code static rif. */
  SAMPLE_A_INPATIENT_FOUR_CHARACTER_DRG_CODE(
      resourceUrl("rif-static-samples/sample-a-inpatient-with-four-character-drg-code.txt"),
      RifFileType.INPATIENT,
      1),

  /** Sample A Outpatient static rif. */
  SAMPLE_A_OUTPATIENT(
      resourceUrl("rif-static-samples/sample-a-outpatient.txt"), RifFileType.OUTPATIENT, 1),

  /** Sample A for BB2 Outpatient static rif. */
  SAMPLE_A4BB2_OUTPATIENT(
      resourceUrl("rif-static-samples/sample-a4bb2-outpatient.txt"), RifFileType.OUTPATIENT, 1),

  /** Sample A SNF static rif. */
  SAMPLE_A_SNF(resourceUrl("rif-static-samples/sample-a-snf.txt"), RifFileType.SNF, 1),

  /** Sample A for BB2 SNF static rif. */
  SAMPLE_A4BB2_SNF(resourceUrl("rif-static-samples/sample-a4bb2-snf.txt"), RifFileType.SNF, 1),

  /** Sample A SNF Four Character DRG Code static rif. */
  SAMPLE_A_SNF_FOUR_CHARACTER_DRG_CODE(
      resourceUrl("rif-static-samples/sample-a-snf-with-four-character-drg-code.txt"),
      RifFileType.SNF,
      1),

  /** Sample A Hospice static rif. */
  SAMPLE_A_HOSPICE(resourceUrl("rif-static-samples/sample-a-hospice.txt"), RifFileType.HOSPICE, 1),

  /** Sample A for BB2 Hospice static rif. */
  SAMPLE_A4BB2_HOSPICE(
      resourceUrl("rif-static-samples/sample-a4bb2-hospice.txt"), RifFileType.HOSPICE, 1),

  /** Sample A HHA static rif. */
  SAMPLE_A_HHA(resourceUrl("rif-static-samples/sample-a-hha.txt"), RifFileType.HHA, 1),

  /** Sample A for BB2 HHA static rif. */
  SAMPLE_A4BB2_HHA(resourceUrl("rif-static-samples/sample-a4bb2-hha.txt"), RifFileType.HHA, 1),

  /** Sample A DME static rif. */
  SAMPLE_A_DME(resourceUrl("rif-static-samples/sample-a-dme.txt"), RifFileType.DME, 1),

  /** Sample A for BB2 DME static rif. */
  SAMPLE_A4BB2_DME(resourceUrl("rif-static-samples/sample-a4bb2-dme.txt"), RifFileType.DME, 1),

  /** Sample A PDE static rif. */
  SAMPLE_A_PDE(resourceUrl("rif-static-samples/sample-a-pde.txt"), RifFileType.PDE, 1),

  /** Sample A for BB2 PDE static rif. */
  SAMPLE_A4BB2_PDE(resourceUrl("rif-static-samples/sample-a4bb2-pde.txt"), RifFileType.PDE, 1),

  /** Sample A Multiple Rows Same Beneficiary static rif. */
  SAMPLE_A_MULTIPLE_ROWS_SAME_BENE(
      resourceUrl("rif-static-samples/sample-a-multiple-entries-same-bene.txt"),
      RifFileType.BENEFICIARY,
      6),

  /** Sample U Benficiary static rif. */
  SAMPLE_U_BENES(
      resourceUrl("rif-static-samples/sample-u-beneficiaries.txt"), RifFileType.BENEFICIARY, 1),

  /** Sample U Benficiary Changed With 8 months of history static rif. */
  SAMPLE_U_BENES_CHANGED_WITH_8_MONTHS(
      resourceUrl("rif-static-samples/sample-u-with-8-months.txt"), RifFileType.BENEFICIARY, 1),

  /** Sample U Benficiary Changed With 9 months of history static rif. */
  SAMPLE_U_BENES_CHANGED_WITH_9_MONTHS(
      resourceUrl("rif-static-samples/sample-u-with-9-months.txt"), RifFileType.BENEFICIARY, 1),

  /** Sample U Benficiary Unchanged static rif. */
  SAMPLE_U_BENES_UNCHANGED(
      resourceUrl("rif-static-samples/sample-u-unchanged-beneficiaries.txt"),
      RifFileType.BENEFICIARY,
      1),

  /** Sample U Carrier static rif. */
  SAMPLE_U_CARRIER(resourceUrl("rif-static-samples/sample-u-bcarrier.txt"), RifFileType.CARRIER, 1),

  /**
   * The ({@code SAMPLE_SYNTHEA_*}) set of test fixture files were generated using <a href=
   * "https://github.com/synthetichealth/synthea/wiki/Getting-Started">Synthea</a>. To recreate
   * these files, perform the following steps after installing Synthea as per the developer
   * instructions in the linked site above:
   *
   * <ol>
   *   <li>Generate the files: {@code ./run_synthea -s 1010 -cs 0 -r 20210520 -e 20210520 -p 20
   *       --exporter.fhir.export=false --exporter.bfd.export=true --exporter.years_of_history=10
   *       --generate.only_alive_patients=true -a 70-80}
   *   <li>Minimize the files: {@code ./gradlew rifMinimize}
   *   <li>Copy the files to the bfd resource dir: {@code cp output/bfd_min/*
   *       $BFD_HOME/apps/bfd-model/bfd-model-rif-samples/src/main/resources/rif-synthea}
   * </ol>
   */
  SAMPLE_SYNTHEA_BENES2011(
      resourceUrl("rif-synthea/beneficiary_2011.csv"), RifFileType.BENEFICIARY, -1),
  /** Sample Synthea Benes 2012 Resource. */
  SAMPLE_SYNTHEA_BENES2012(
      resourceUrl("rif-synthea/beneficiary_2012.csv"), RifFileType.BENEFICIARY, -1),
  /** Sample Synthea Benes 2013 Resource. */
  SAMPLE_SYNTHEA_BENES2013(
      resourceUrl("rif-synthea/beneficiary_2013.csv"), RifFileType.BENEFICIARY, -1),
  /** Sample Synthea Benes 2014 Resource. */
  SAMPLE_SYNTHEA_BENES2014(
      resourceUrl("rif-synthea/beneficiary_2014.csv"), RifFileType.BENEFICIARY, -1),
  /** Sample Synthea Benes 2015 Resource. */
  SAMPLE_SYNTHEA_BENES2015(
      resourceUrl("rif-synthea/beneficiary_2015.csv"), RifFileType.BENEFICIARY, -1),
  /** Sample Synthea Benes 2016 Resource. */
  SAMPLE_SYNTHEA_BENES2016(
      resourceUrl("rif-synthea/beneficiary_2016.csv"), RifFileType.BENEFICIARY, -1),
  /** Sample Synthea Benes 2017 Resource. */
  SAMPLE_SYNTHEA_BENES2017(
      resourceUrl("rif-synthea/beneficiary_2017.csv"), RifFileType.BENEFICIARY, -1),
  /** Sample Synthea Benes 2018 Resource. */
  SAMPLE_SYNTHEA_BENES2018(
      resourceUrl("rif-synthea/beneficiary_2018.csv"), RifFileType.BENEFICIARY, -1),
  /** Sample Synthea Benes 2019 Resource. */
  SAMPLE_SYNTHEA_BENES2019(
      resourceUrl("rif-synthea/beneficiary_2019.csv"), RifFileType.BENEFICIARY, -1),
  /** Sample Synthea Benes 2020 Resource. */
  SAMPLE_SYNTHEA_BENES2020(
      resourceUrl("rif-synthea/beneficiary_2020.csv"), RifFileType.BENEFICIARY, -1),
  /** Sample Synthea Benes 2021 Resource. */
  SAMPLE_SYNTHEA_BENES2021(
      resourceUrl("rif-synthea/beneficiary_2021.csv"), RifFileType.BENEFICIARY, -1),
  /** Sample Synthea Carrier Resource. */
  SAMPLE_SYNTHEA_CARRIER(resourceUrl("rif-synthea/carrier.csv"), RifFileType.CARRIER, -1),
  /** Sample Synthea Inpatient Resource. */
  SAMPLE_SYNTHEA_INPATIENT(resourceUrl("rif-synthea/inpatient.csv"), RifFileType.INPATIENT, -1),
  /** Sample Synthea Outpatient Resource. */
  SAMPLE_SYNTHEA_OUTPATIENT(resourceUrl("rif-synthea/outpatient.csv"), RifFileType.OUTPATIENT, -1),
  /** Sample Synthea SNF Resource. */
  SAMPLE_SYNTHEA_SNF(resourceUrl("rif-synthea/snf.csv"), RifFileType.SNF, -1),
  /** Sample Synthea Hospice Resource. */
  SAMPLE_SYNTHEA_HOSPICE(resourceUrl("rif-synthea/hospice.csv"), RifFileType.HOSPICE, -1),
  /** Sample Synthea HHAt Resource. */
  SAMPLE_SYNTHEA_HHA(resourceUrl("rif-synthea/hha.csv"), RifFileType.HHA, -1),
  /** Sample Synthea DME Resource. */
  SAMPLE_SYNTHEA_DME(resourceUrl("rif-synthea/dme.csv"), RifFileType.DME, -1),
  /** Sample Synthea PDE Resource. */
  SAMPLE_SYNTHEA_PDE(resourceUrl("rif-synthea/pde.csv"), RifFileType.PDE, -1),
  /** Sample Synthea Benehistory Resource. */
  SAMPLE_SYNTHEA_BENEHISTORY(
      resourceUrl("rif-synthea/beneficiary_history.csv"), RifFileType.BENEFICIARY_HISTORY, -1),

  /** Synthetic Hicn Multiple Benes S3 data. */
  SAMPLE_HICN_MULT_BENES(
      resourceUrl("rif-static-samples/sample-hicn-mult-bene-beneficiaries.txt"),
      RifFileType.BENEFICIARY,
      10),
  /** Synthetic Hicn Multiple Beneficiary History S3 data. */
  SAMPLE_HICN_MULT_BENES_BENEFICIARY_HISTORY(
      resourceUrl("rif-static-samples/sample-hicn-mult-bene-beneficiaryhistory.txt"),
      RifFileType.BENEFICIARY_HISTORY,
      7),

  /** Sample A Carrier static rif. */
  SAMPLE_A_CARRIER_SAMHSA(
      resourceUrl("rif-static-samples/samhsa/sample-a-carrier.txt"), RifFileType.CARRIER, 1),

  /** Sample A DME static rif. */
  SAMPLE_A_DME_SAMHSA(
      resourceUrl("rif-static-samples/samhsa/sample-a-dme.txt"), RifFileType.DME, 1),

  /** Sample A HHA static rif. */
  SAMPLE_A_HHA_SAMHSA(
      resourceUrl("rif-static-samples/samhsa/sample-a-hha.txt"), RifFileType.HHA, 1),

  /** Sample A Hospice static rif. */
  SAMPLE_A_HOSPICE_SAMHSA(
      resourceUrl("rif-static-samples/samhsa/sample-a-hospice.txt"), RifFileType.HOSPICE, 1),

  /** Sample A Inpatient static rif. */
  SAMPLE_A_INPATIENT_SAMHSA(
      resourceUrl("rif-static-samples/samhsa/sample-a-inpatient.txt"), RifFileType.INPATIENT, 1),

  /** Sample A Outpatient static rif. */
  SAMPLE_A_OUTPATIENT_SAMHSA(
      resourceUrl("rif-static-samples/samhsa/sample-a-outpatient.txt"), RifFileType.OUTPATIENT, 1),

  /** Sample A SNF static rif. */
  SAMPLE_A_SNF_SAMHSA(
      resourceUrl("rif-static-samples/samhsa/sample-a-snf.txt"), RifFileType.SNF, 1);

  /** The Resource URL Supplier for the different RIF files. */
  private final Supplier<URL> resourceUrlSupplier;

  /** The Rif File Type of the beneficiary files (EX: Beneficiary, Carrier, etc). */
  private final RifFileType rifFileType;

  /** The Record Count of the beneficiaries/claims/drug events in the RIF file. */
  private final int recordCount;

  /** The Resource URL of the resource's contents. */
  private URL resourceUrl;

  /**
   * Enum constant constructor.
   *
   * @param resourceUrlSupplier the value to use for {@link #getResourceSupplier()}
   * @param rifFileType the value to use for {@link #getRifFileType()}
   * @param recordCount the value to use for {@link #getRecordCount()}. If the supplied value is
   *     negative the size will be computed by scanning the file.
   */
  private StaticRifResource(
      Supplier<URL> resourceUrlSupplier, RifFileType rifFileType, int recordCount) {
    this.resourceUrlSupplier = resourceUrlSupplier;
    this.rifFileType = rifFileType;
    if (recordCount < 0) {
      this.recordCount = computeRecordCount();
    } else {
      this.recordCount = recordCount;
    }
  }

  /**
   * Gets the {@link #resourceUrl}.
   *
   * @return the {@link URL} to the resource's contents
   */
  public synchronized URL getResourceUrl() {
    if (resourceUrl == null) resourceUrl = resourceUrlSupplier.get();

    return resourceUrl;
  }

  /**
   * Gets the {@link #rifFileType}.
   *
   * @return the {@link RifFileType} of the RIF file
   */
  public RifFileType getRifFileType() {
    return rifFileType;
  }

  /**
   * Gets the {@link #recordCount}.
   *
   * @return the number of beneficiaries/claims/drug events in the RIF file excluding line items
   */
  public int getRecordCount() {
    return recordCount;
  }

  /**
   * Compute the number of records in the RIF file. Takes account of the configured id column so
   * that, e.g., the count would return the count of claims rather than the count of all claim
   * lines.
   *
   * @return the count of records
   */
  private int computeRecordCount() {
    RifFile file = toRifFile();
    String idColumn = null;
    if (getRifFileType().getIdColumn() != null) {
      idColumn = getRifFileType().getIdColumn().toString();
    }
    try {
      Iterable<CSVRecord> records =
          CSVFormat.RFC4180
              .withDelimiter('|')
              .withHeader()
              .parse(new InputStreamReader(file.open(), file.getCharset()));
      Set<String> uniqueIds = new HashSet<>();
      int i = 0;
      for (CSVRecord record : records) {
        if (idColumn != null) {
          uniqueIds.add(record.get(idColumn));
        } else {
          uniqueIds.add(Integer.toString(i++));
        }
      }
      return uniqueIds.size();
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Unable to open resource: " + resourceUrlSupplier.get().toString(), e);
    }
  }

  /**
   * Returns the Static Rif file as a {@link RifFile}.
   *
   * @return a {@link RifFile} based on this {@link StaticRifResource}
   */
  public RifFile toRifFile() {
    return new StaticRifFile(this);
  }

  /**
   * Gets the {@link Supplier} for the {@link URL} to the resource's contents.
   *
   * @param resourceName the name of the resource on the classpath (as might be passed to {@link
   *     ClassLoader#getResource(String)})
   * @return the resource url
   */
  private static Supplier<URL> resourceUrl(String resourceName) {
    return () -> {
      URL resource = Thread.currentThread().getContextClassLoader().getResource(resourceName);
      if (resource == null)
        throw new IllegalArgumentException("Unable to find resource: " + resourceName);

      return resource;
    };
  }
}
