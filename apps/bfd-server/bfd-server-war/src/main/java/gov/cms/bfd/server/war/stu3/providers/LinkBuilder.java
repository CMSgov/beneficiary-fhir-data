package gov.cms.bfd.server.war.stu3.providers;

import org.hl7.fhir.dstu3.model.Bundle;

/** An interface for building page links in a Bundle. */
public interface LinkBuilder {
  /**
   * Is paging requested?
   *
   * @return true iff requested
   */
  boolean isPagingRequested();

  /**
   * Add the links from the builder to the bundle
   *
   * @param to bundle
   */
  void addLinks(Bundle to);
}
