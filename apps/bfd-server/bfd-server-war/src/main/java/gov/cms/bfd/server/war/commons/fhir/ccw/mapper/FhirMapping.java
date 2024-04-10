package gov.cms.bfd.server.war.commons.fhir.ccw.mapper;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;

/** converted generated, add logic in subclass. */
@Getter
@SuppressWarnings("all")
public class FhirMapping {
  private String version;
  private String resource;
  private String element;
  private String fhirPath;
  private List<String> discriminator = new ArrayList<String>();
  private List<String> additional = new ArrayList<String>();
  private String derived;
  private String note;
  private String example;

  public void setVersion(String version) {
    this.version = version;
  }

  public void setResource(String resource) {
    this.resource = resource;
  }

  public void setElement(String element) {
    this.element = element;
  }

  public void setFhirPath(String fhirPath) {
    this.fhirPath = fhirPath;
  }

  public void setDiscriminator(List<String> discriminator) {
    this.discriminator = discriminator;
  }

  public void setAdditional(List<String> additional) {
    this.additional = additional;
  }

  public void setDerived(String derived) {
    this.derived = derived;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public void setExample(String example) {
    this.example = example;
  }
}
