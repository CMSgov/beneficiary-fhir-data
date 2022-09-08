package gov.cms.bfd.data.npi.utility;
/**
 * NPI common objects
 */
public class NPIAppObject {
   /**
   * The name of the classpath resource (for the project's main web application) for the NPI "Orgs"
   * TSV file.
   */
  public static final String NPI_RESOURCE = "npi_org_data_utf8.tsv";

  private boolean UseFakeDrugCode;

  private String OutputDir;

  private String DownloadUrl;

  /**
   *  
   */
  public NPIAppObject(boolean useFakeDrugCode, String outputDir){
    this.UseFakeDrugCode = useFakeDrugCode;
    this.OutputDir = outputDir;
  }

   /**
   *  
   */
  public NPIAppObject(String downloadUrl, String outputDir){
    this.DownloadUrl = downloadUrl;
    this.OutputDir = outputDir;
  }

    
}