package gov.cms.bfd.model.codebook.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import gov.cms.bfd.model.codebook.extractor.SupportedCodebook;

/**
 * Represents the data contained in a
 * <a href="https://www.ccwdata.org/web/guest/data-dictionaries">CMS Chronic
 * Conditions Warehouse (CCW) data dictionary</a> codebook.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public final class Codebook {
	@XmlAttribute
	private final String id;

	@XmlAttribute
	private final String name;

	@XmlAttribute
	private final String version;

	@XmlElement(name = "variable")
	private final List<Variable> variables;

	/**
	 * Constructs a new {@link Codebook}.
	 * 
	 * @param codebookSource
	 *            the {@link SupportedCodebook} that this {@link Codebook} is being
	 *            built from
	 */
	public Codebook(SupportedCodebook codebookSource) {
		this.id = codebookSource.name();
		this.name = codebookSource.getDisplayName();
		this.version = codebookSource.getVersion();
		this.variables = new ArrayList<>();
	}

	/**
	 * This public no-arg constructor is required by JAXB.
	 */
	@Deprecated
	public Codebook() {
		this.id = null;
		this.name = null;
		this.version = null;
		this.variables = new ArrayList<>();
	}

	/**
	 * @return the short identifier for this {@link Codebook}, for use in debugging
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the descriptive English name for this {@link Codebook}
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return a human-readable {@link String} that identifies which version of the
	 *         data is represented by this {@link Codebook}, typically something
	 *         like "<code>December 2042, Version 42.0</code>"
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @return the mutable {@link List} of {@link Variable}s in the {@link Codebook}
	 */
	public List<Variable> getVariables() {
		return variables;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getId();
	}
}
