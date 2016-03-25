package gov.hhs.cms.bluebutton.datapipeline.ccw.jdo;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.Column;
import javax.jdo.annotations.Extension;
import javax.jdo.annotations.Order;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Models rows in the CCW's <code>CCW_BENE_CRNT_VW</code> table, which represent
 * current Medicare beneficiaries.
 */
@PersistenceCapable(table = "CCW_BENE_CRNT_VW", detachable = "true")
public class CurrentBeneficiary {
	@Persistent
	@Column(name = "BENE_ID")
	@PrimaryKey
	private Integer id;

	@Persistent
	@Column(name = "BENE_BIRTH_DT", allowsNull = "true")
	private LocalDate birthDate;

	@Persistent
	@Column(name = "BENE_GVN_NAME", allowsNull = "true")
	private String givenName;

	@Persistent
	@Column(name = "BENE_SRNM_NAME", allowsNull = "true")
	private String surname;

	@Persistent
	@Column(name = "BENE_MLG_CNTCT_ADR", allowsNull = "true")
	private String contactAddress;

	@Persistent
	@Column(name = "BENE_MLG_CNTCT_ZIP_CD", allowsNull = "true")
	private String contactAddressZip;

	@Persistent(mappedBy = "beneficiary")
	@Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "id ASC"))
	private List<PartAClaimFact> partAClaimFacts = new ArrayList<>();

	@Persistent(mappedBy = "beneficiary")
	@Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "id ASC"))
	private List<PartBClaimFact> partBClaimFacts = new ArrayList<>();

	@Persistent(mappedBy = "beneficiary")
	@Order(extensions = @Extension(vendorName = "datanucleus", key = "list-ordering", value = "id ASC"))
	private List<PartDEventFact> partDEventFacts = new ArrayList<>();

	/**
	 * Constructs a new {@link CurrentBeneficiary} instance.
	 */
	public CurrentBeneficiary() {
	}

	/**
	 * @return the beneficiary's ID
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id
	 *            the new value for {@link #getId()}
	 * @return this instance (for call-chaining purposes)
	 */
	public CurrentBeneficiary setId(Integer id) {
		this.id = id;
		return this;
	}

	/**
	 * @return the beneficiary's birth date
	 */
	public LocalDate getBirthDate() {
		return birthDate;
	}

	/**
	 * @param birthDate
	 *            the new value for {@link #getBirthDate()}
	 * @return this instance (for call-chaining purposes)
	 */
	public CurrentBeneficiary setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
		return this;
	}

	/**
	 * @return the beneficiary's first/given name
	 */
	public String getGivenName() {
		return givenName;
	}

	/**
	 * @param givenName
	 *            the new value for {@link #getGivenName()}
	 * @return this instance (for call-chaining purposes)
	 */
	public CurrentBeneficiary setGivenName(String givenName) {
		this.givenName = givenName;
		return this;
	}

	/**
	 * @return the beneficiary's surname/last/family name
	 */
	public String getSurname() {
		return surname;
	}

	/**
	 * @param surname
	 *            the new value for {@link #getSurname()}
	 * @return this instance (for call-chaining purposes)
	 */
	public CurrentBeneficiary setSurname(String surname) {
		this.surname = surname;
		return this;
	}

	/**
	 * @return the beneficiary's contact/mailing address as a single-line
	 *         {@link String}, including everything but their zip code
	 */
	public String getContactAddress() {
		return contactAddress;
	}

	/**
	 * @param contactAddress
	 *            the new value for {@link #getContactAddress()}
	 * @return this instance (for call-chaining purposes)
	 */
	public CurrentBeneficiary setContactAddress(String contactAddress) {
		this.contactAddress = contactAddress;
		return this;
	}

	/**
	 * @return the zip code of the beneficiary's contact/mailing address
	 */
	public String getContactAddressZip() {
		return contactAddressZip;
	}

	/**
	 * @param contactAddressZip
	 *            the new value for {@link #getContactAddressZip()}
	 * @return this instance (for call-chaining purposes)
	 */
	public CurrentBeneficiary setContactAddressZip(String contactAddressZip) {
		this.contactAddressZip = contactAddressZip;
		return this;
	}

	/**
	 * @return the {@link PartAClaimFact}s associated with this
	 *         {@link CurrentBeneficiary}
	 */
	public List<PartAClaimFact> getPartAClaimFacts() {
		return partAClaimFacts;
	}

	/**
	 * @return the {@link PartBClaimFact}s associated with this
	 *         {@link CurrentBeneficiary}
	 */
	public List<PartBClaimFact> getPartBClaimFacts() {
		return partBClaimFacts;
	}

	/**
	 * @return the {@link PartDEventFact}s associated with this
	 *         {@link CurrentBeneficiary}
	 */
	public List<PartDEventFact> getPartDEventFacts() {
		return partDEventFacts;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CurrentBeneficiary [id=");
		builder.append(id);
		builder.append(", birthDate=");
		builder.append(birthDate);
		builder.append(", givenName=");
		builder.append(givenName);
		builder.append(", surname=");
		builder.append(surname);
		builder.append(", contactAddress=");
		builder.append(contactAddress);
		builder.append(", contactAddressZip=");
		builder.append(contactAddressZip);
		builder.append(", partAClaimFacts=");
		builder.append(partAClaimFacts);
		builder.append(", partBClaimFacts=");
		builder.append(partBClaimFacts);
		builder.append(", partDEventFacts=");
		builder.append(partDEventFacts);
		builder.append("]");
		return builder.toString();
	}
}
