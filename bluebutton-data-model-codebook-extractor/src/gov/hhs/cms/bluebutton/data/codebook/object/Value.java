package gov.hhs.cms.bluebutton.data.codebook.object;

import org.apache.commons.lang3.StringUtils;

public class Value {
	
	private String value = "";
	private String description = "";
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = StringUtils.replace(description, "\"", "");
	}
}
