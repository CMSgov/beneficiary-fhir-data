package gov.hhs.cms.bluebutton.datapipeline.desynpuf;

public enum SynpufDownload {
	SAMPLE_1(
			"https://www.cms.gov/Research-Statistics-Data-and-Systems/Downloadable-Public-Use-Files/SynPUFs/Downloads/DE1_0_2008_Beneficiary_Summary_File_Sample_1.zip",
			"https://www.cms.gov/Research-Statistics-Data-and-Systems/Downloadable-Public-Use-Files/SynPUFs/Downloads/DE1_0_2009_Beneficiary_Summary_File_Sample_1.zip",
			"https://www.cms.gov/Research-Statistics-Data-and-Systems/Downloadable-Public-Use-Files/SynPUFs/Downloads/DE1_0_2010_Beneficiary_Summary_File_Sample_1.zip",
			"https://www.cms.gov/Research-Statistics-Data-and-Systems/Downloadable-Public-Use-Files/SynPUFs/Downloads/DE1_0_2008_to_2010_Inpatient_Claims_Sample_1.zip",
			"https://www.cms.gov/Research-Statistics-Data-and-Systems/Downloadable-Public-Use-Files/SynPUFs/Downloads/DE1_0_2008_to_2010_Outpatient_Claims_Sample_1.zip",
			"http://downloads.cms.gov/files/DE1_0_2008_to_2010_Carrier_Claims_Sample_1A.zip",
			"http://downloads.cms.gov/files/DE1_0_2008_to_2010_Carrier_Claims_Sample_1B.zip",
			"http://downloads.cms.gov/files/DE1_0_2008_to_2010_Prescription_Drug_Events_Sample_1.zip");

	private final String beneficiaries2008;
	private final String beneficiaries2009;
	private final String beneficiaries2010;
	private final String partAClaimsInpatient;
	private final String partAClaimsOutpatient;
	private final String partBClaimsA;
	private final String partBClaimsB;
	private final String partDClaims;

	/**
	 * Enum constant constructor.
	 * 
	 * @param beneficiaries2008
	 *            the value to use for {@link #getBeneficiaries2008()}
	 * @param beneficiaries2009
	 *            the value to use for {@link #getBeneficiaries2009()}
	 * @param beneficiaries2010
	 *            the value to use for {@link #getBeneficiaries2010()}
	 * @param partAClaimsInpatient
	 *            the value to use for {@link #getPartAClaimsInpatient()}
	 * @param partAClaimsOutpatient
	 *            the value to use for {@link #getPartAClaimsOutpatient()}
	 * @param partBClaimsA
	 *            the value to use for {@link #getPartBClaimsA()}
	 * @param partBClaimsB
	 *            the value to use for {@link #getPartBClaimsB()}
	 * @param partDClaims
	 *            the value to use for {@link #getPartDClaims()}
	 */
	private SynpufDownload(String beneficiaries2008, String beneficiaries2009, String beneficiaries2010,
			String partAClaimsInpatient, String partAClaimsOutpatient, String partBClaimsA, String partBClaimsB,
			String partDClaims) {
		this.beneficiaries2008 = beneficiaries2008;
		this.beneficiaries2009 = beneficiaries2009;
		this.beneficiaries2010 = beneficiaries2010;
		this.partAClaimsInpatient = partAClaimsInpatient;
		this.partAClaimsOutpatient = partAClaimsOutpatient;
		this.partBClaimsA = partBClaimsA;
		this.partBClaimsB = partBClaimsB;
		this.partDClaims = partDClaims;
	}

	public String getBeneficiaries2008() {
		return beneficiaries2008;
	}

	public String getBeneficiaries2009() {
		return beneficiaries2009;
	}

	public String getBeneficiaries2010() {
		return beneficiaries2010;
	}

	public String getPartAClaimsInpatient() {
		return partAClaimsInpatient;
	}

	public String getPartAClaimsOutpatient() {
		return partAClaimsOutpatient;
	}

	public String getPartBClaimsA() {
		return partBClaimsA;
	}

	public String getPartBClaimsB() {
		return partBClaimsB;
	}

	public String getPartDClaims() {
		return partDClaims;
	}
}
