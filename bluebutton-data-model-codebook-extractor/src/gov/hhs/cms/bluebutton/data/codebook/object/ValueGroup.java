package gov.hhs.cms.bluebutton.data.codebook.object;

import java.util.ArrayList;
import java.util.List;

public class ValueGroup {
	
	private List<Value> values;
	private String description = "";
	
	public List<Value> getValues() {
		if(values == null) {
			values = new ArrayList<Value>();
		}
		return values;
	}
	public void setValues(List<Value> values) {
		this.values = values;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
}
