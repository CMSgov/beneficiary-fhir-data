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
  /**
   * {@link StaticRifResource} that identifies a static RIF file, its type, and expected record
   * count.
   */
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

  @Override
  public RifFileType getFileType() {
    return staticRifResource.getRifFileType();
  }

  @Override
  public String getDisplayName() {
    return staticRifResource.name();
  }

  @Override
  public Charset getCharset() {
    return StandardCharsets.UTF_8;
  }

  @Override
  public InputStream open() {
    try {
      return staticRifResource.getResourceUrl().openStream();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("StaticRifFile [staticRifResource=");
    builder.append(staticRifResource);
    builder.append("]");
    return builder.toString();
  }
}
