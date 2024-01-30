package gov.cms.bfd.model.rif.samples;

import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileType;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/** This {@link RifFile} implementation operates on local files. */
final class StaticRifFile implements RifFile {
  /**
   * {@link StaticRifResource} that identifies a static RIF file, its type, and expected record
   * count.
   */
  private final StaticRifResource staticRifResource;

  /** Tracks the values set by calls to {@link #updateLastRecordNumber}. */
  private final AtomicLong lastRecordNumber = new AtomicLong(0L);

  /**
   * Constructs a new {@link StaticRifFile}.
   *
   * @param staticRifResource the {@link StaticRifResource} that this {@link StaticRifFile} will be
   *     based on
   */
  public StaticRifFile(StaticRifResource staticRifResource) {
    this.staticRifResource = staticRifResource;
  }

  /** {@inheritDoc} */
  @Override
  public RifFileType getFileType() {
    return staticRifResource.getRifFileType();
  }

  /** {@inheritDoc} */
  @Override
  public String getDisplayName() {
    return staticRifResource.name();
  }

  /** {@inheritDoc} */
  @Override
  public Charset getCharset() {
    return StandardCharsets.UTF_8;
  }

  /** {@inheritDoc} */
  @Override
  public InputStream open() {
    try {
      return staticRifResource.getResourceUrl().openStream();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public long getLastRecordNumber() {
    return lastRecordNumber.get();
  }

  @Override
  public void updateLastRecordNumber(long recordNumber) {
    lastRecordNumber.set(recordNumber);
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("StaticRifFile [staticRifResource=");
    builder.append(staticRifResource);
    builder.append("]");
    return builder.toString();
  }
}
