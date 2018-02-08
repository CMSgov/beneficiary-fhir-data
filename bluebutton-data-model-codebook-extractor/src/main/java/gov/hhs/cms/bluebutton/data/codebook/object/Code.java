package gov.hhs.cms.bluebutton.data.codebook.object;

import java.util.ArrayList;
import java.util.List;

public class Code {

	private String name = "";
	private String label = "";
	private String description = "";
	private String shortName = "";
	private String longName = "";
	private String type = "";
	private String length = "";
	private String source = "";
	private String valueFormat = "";
	private List<ValueGroup> valueGroups;
	private String comment = "";
	
	public Code() {
		this.valueGroups = new ArrayList<ValueGroup>();
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getShortName() {
		return shortName;
	}
	public void setShortName(String shortName) {
		this.shortName = shortName;
	}
	public String getLongName() {
		return longName;
	}
	public void setLongName(String longName) {
		this.longName = longName;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getLength() {
		return length;
	}
	public void setLength(String length) {
		this.length = length;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public String getValueFormat() {
		return valueFormat;
	}
	public void setValueFormat(String valueFormat) {
		this.valueFormat = valueFormat;
	}
	public List<ValueGroup> getValueGroups() {
		return valueGroups;
	}
	public void setValueGroups(List<ValueGroup> valueGroups) {
		this.valueGroups = valueGroups;
	}
	public String getComment() {
		return comment;
	}
	public void setComment(String comment) {
		this.comment = comment;
	}
	
	public String toString() {
		
		String valuesTxt = "";
		if(valueGroups != null && valueGroups.size() > 0) {
			for(ValueGroup valGrp : valueGroups) {
				valuesTxt += "        <valuegroup>\n";
				valuesTxt += "          <description>" + valGrp.getDescription() + "</description>\n";
				
				if(valGrp.getValues() != null && valGrp.getValues().size() > 0) {
					for(Value val : valGrp.getValues()) {
						valuesTxt += "          <value value=\"" + val.getValue() + "\" description=\"" + val.getDescription() + "\"/>\n";
					}
				}
				valuesTxt += "        </valuegroup>\n";
			}
		}
		
		String output = "    <code>\n" +
				"      <name>" + name + "</name>\n" +
				"      <label>" + label + "</label>\n" +
				"      <description>" + description + "</description>\n" +
				"      <shortName>" + shortName + "</shortName>\n" +
				"      <longName>" + longName + "</longName>\n" +
				"      <type>" + type + "</type>\n" +
				"      <length>" + length + "</length>\n" +
				"      <source>" + source + "</source>\n" +
				"      <valueFormat>" + valueFormat + "</valueFormat>\n" +
				"      <values>\n" 
				  + valuesTxt + 
				"      </values>\n" +
				"      <comment>" + comment + "</comment>\n" +
				"    <code>";
		
		return output;
	}
}
