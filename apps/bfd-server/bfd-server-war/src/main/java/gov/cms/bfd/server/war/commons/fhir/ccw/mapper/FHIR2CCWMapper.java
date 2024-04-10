package gov.cms.bfd.server.war.commons.fhir.ccw.mapper;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/** converter generated, add logic in subclass. */
@Getter
@SuppressWarnings("all")
public class FHIR2CCWMapper {
  private Integer id;
  private String name;
  private String description;

  private List<String> appliesTo = new ArrayList<String>();

  private List<String> suppliedIn = new ArrayList<String>();
  private String bfdTableType;
  private String bfdColumnName;
  private String bfdDbType;
  private Integer bfdDbSize;
  private String bfdJavaFieldName;
  private List<String> ccwMapping = new ArrayList<String>();
  private List<Object> cclfMapping = new ArrayList<Object>();
  private List<FhirMapping> fhirMapping = new ArrayList<FhirMapping>();

  public void setId(Integer id) {
    this.id = id;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public void setAppliesTo(List<String> appliesTo) {
    this.appliesTo = appliesTo;
  }

  public void setSuppliedIn(List<String> suppliedIn) {
    this.suppliedIn = suppliedIn;
  }

  public void setBfdTableType(String bfdTableType) {
    this.bfdTableType = bfdTableType;
  }

  public void setBfdColumnName(String bfdColumnName) {
    this.bfdColumnName = bfdColumnName;
  }

  public void setBfdDbType(String bfdDbType) {
    this.bfdDbType = bfdDbType;
  }

  public void setBfdDbSize(Integer bfdDbSize) {
    this.bfdDbSize = bfdDbSize;
  }

  public void setBfdJavaFieldName(String bfdJavaFieldName) {
    this.bfdJavaFieldName = bfdJavaFieldName;
  }

  public void setCcwMapping(List<String> ccwMapping) {
    this.ccwMapping = ccwMapping;
  }

  public void setCclfMapping(List<Object> cclfMapping) {
    this.cclfMapping = cclfMapping;
  }

  public void setFhirMapping(List<FhirMapping> fhirMapping) {
    this.fhirMapping = fhirMapping;
  }
}
