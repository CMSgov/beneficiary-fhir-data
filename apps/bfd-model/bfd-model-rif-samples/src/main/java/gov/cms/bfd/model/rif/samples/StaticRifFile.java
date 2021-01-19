package gov.cms.bfd.model.rif.samples;

import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileType;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/** This {@link RifFile} implementation operates on local files. */
final class StaticRifFile implements RifFile {
  private final StaticRifResource staticRifResource;

  /**
   * Constructs a new {@link StaticRifFile}.
   *
   * @param staticRifResource the {@link StaticRifResource} that this {@link StaticRifFile} will be
   *     based on
   */
  public StaticRifFile(StaticRifResource staticRifResource) {
    this.staticRifResource = staticRifResource;
  }

  /** @see gov.cms.bfd.model.rif.RifFile#getFileType() */
  @Override
  public RifFileType getFileType() {
    return staticRifResource.getRifFileType();
  }

  /** @see gov.cms.bfd.model.rif.RifFile#getDisplayName() */
  @Override
  public String getDisplayName() {
    return staticRifResource.name();
  }

  /** @see gov.cms.bfd.model.rif.RifFile#getCharset() */
  @Override
  public Charset getCharset() {
    return StandardCharsets.UTF_8;
  }

  /** @see gov.cms.bfd.model.rif.RifFile#open() */
  @Override
  public InputStream open() {
    try {
      return staticRifResource.getResourceUrl().openStream();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /** @see java.lang.Object#toString() */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("StaticRifFile [staticRifResource=");
    builder.append(staticRifResource);
    builder.append("]");
    return builder.toString();
  }
}
