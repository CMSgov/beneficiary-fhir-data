package gov.cms.bfd.server.war.commons;

/** An interface for building page links in a Bundle. */
public interface LinkBuilder {
  /**
   * Is paging requested?
   *
   * @return true iff paging is requested
   */
  boolean isPagingRequested();

  /**
   * Return the size of the page (ie. _count value). Integer.MAX_VALUE if isPagingRequested is
   * false.
   */
  int getPageSize();

  /** Return is this a first page request. Always true if isPagingReuested() is false. */
  boolean isFirstPage();

  /**
   * Add the links from the builder to the bundle. No links are are added if paging is not
   * requested.
   *
   * @param to bundle
   */
  void addLinks(org.hl7.fhir.dstu3.model.Bundle to);

  /**
   * Add the links from the builder to the bundle. No links are are added if paging is not
   * requested.
   *
   * @param to bundle
   */
  void addLinks(org.hl7.fhir.r4.model.Bundle to);
}
