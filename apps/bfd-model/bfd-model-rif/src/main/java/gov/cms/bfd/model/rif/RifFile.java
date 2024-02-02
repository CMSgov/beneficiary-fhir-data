package gov.cms.bfd.model.rif;

import java.io.InputStream;
import java.nio.charset.Charset;
import javax.annotation.Nullable;

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

  /**
   * Returns true if the file has not been fully processed yet.
   *
   * @return true if needs processing
   */
  default boolean requiresProcessing() {
    return true;
  }

  /**
   * Record used by {@link #getRecordId} to return primary key information for {@link
   * gov.cms.bfd.model.rif.entities.S3DataFile} corresponding to this file.
   *
   * @param manifestId unique id for {@link gov.cms.bfd.model.rif.entities.S3ManifestFile} record
   * @param index index of data file within the manifest
   */
  record RecordId(long manifestId, short index) {}

  /**
   * Gets an optional unique id for a record in the database for the data file.
   *
   * @return null or a unique record id
   */
  @Nullable
  default RecordId getRecordId() {
    return null;
  }

  /** Marks the file as having started processing. */
  default void markAsStarted() {}

  /** Marks the file as fully processed. */
  default void markAsProcessed() {}

  /**
   * Return the highest record number for which we know that every record with that record number or
   * lower has been processed. Returns zero if such a value is unknown (either because processing
   * has not been started, or progress has not been tracked).
   *
   * @return the value as described in this comment
   */
  default long getLastRecordNumber() {
    return 0L;
  }

  /**
   * Updates the last record number value in some manner. This might include writing to a database
   * table or doing nothing at all.
   *
   * @param recordNumber the new value
   */
  default void updateLastRecordNumber(long recordNumber) {}
}
