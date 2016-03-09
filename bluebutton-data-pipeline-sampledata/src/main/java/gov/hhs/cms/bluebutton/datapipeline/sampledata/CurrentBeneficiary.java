package gov.hhs.cms.bluebutton.datapipeline.sampledata;

import java.time.LocalDate;

import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

/**
 * Models rows in the CCW's <code>CCW_BENE_CRNT_VW</code> table, which represent
 * current Medicare beneficiaries.
 */
@PersistenceCapable(table = "CCW_BENE_CRNT_VW")
public class CurrentBeneficiary {
	@PrimaryKey
	@Persistent(column = "BENE_ID")
	private Integer id;

	@Persistent(column = "BENE_BIRTH_DT")
	private LocalDate birthDate;

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
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @param birthDate
	 *            the new value for {@link #getBirthDate()}
	 */
	public void setBirthDate(LocalDate birthDate) {
		this.birthDate = birthDate;
	}

	/**
	 * @return the beneficiary's birth date
	 */
	public LocalDate getBirthDate() {
		return birthDate;
	}
}
