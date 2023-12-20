package gov.cms.bfd.model.rif;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

/** Represents a RIF file that can be read and deleted. */
public interface RifFile {
  /**
   * Gets a name that can be used in logs and such to identify and help debug this {@link RifFile}.
   *
   * @return the display name
   */
  String getDisplayName();

  /**
   * Gets the {@link RifFileType} for this {@link RifFile}, which identifies its format/contents.
   *
   * @return the file type
   */
  RifFileType getFileType();

  /**
   * Gets the {@link Charset} that the data in {@link #open()} is encoded in.
   *
   * @return the charset
   */
  Charset getCharset();

  /**
   * Gets a new {@link InputStream} to the RIF file's contents.
   *
   * @return a new {@link InputStream}
   */
  InputStream open();

  default File getFile() throws IOException {
    throw new IOException("not a file");
  }
}
