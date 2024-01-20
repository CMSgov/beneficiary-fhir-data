package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import com.google.common.annotations.VisibleForTesting;
import gov.cms.bfd.model.rif.entities.S3DataFile;
import gov.cms.bfd.model.rif.entities.S3ManifestFile;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Data access object for working with S3 manifest database entities. */
@AllArgsConstructor
public class S3ManifestDbDao {
  /** Used to run transactions. */
  private final TransactionManager transactionManager;

  /**
   * Checks for an already existing record for the provided manifest. If one is found check it for
   * consistency with the real manifest and return it. If one is not found create a new record,
   * insert it into the database, and then return it. Either way when this call returns the manifest
   * and its associated data files will be in our database.
   *
   * @param manifestS3Key S3 key of the manifest file
   * @param xmlManifest the manifest file as loaded from S3
   * @param now timestamp to use for current time
   * @return the entity that we either loaded or created
   */
  public S3ManifestFile insertOrReadManifestAndDataFiles(
      String manifestS3Key, DataSetManifest xmlManifest, Instant now) {
    return transactionManager.executeFunction(
        entityManager -> {
          S3ManifestFile dbManifest = readS3ManifestAndDataFilesImpl(manifestS3Key, entityManager);
          if (dbManifest != null) {
            verifyExistingRecordMatchesDataSetManifest(xmlManifest, dbManifest);
            return dbManifest;
          }
          dbManifest = createNewManifestAndFiles(manifestS3Key, xmlManifest, now);
          entityManager.persist(dbManifest);
          return dbManifest;
        });
  }

  /**
   * Update the {@link S3ManifestFile} and its associated {@link S3DataFile}s in the database.
   *
   * @param manifestFile contains the manifest to update
   */
  public void updateS3ManifestAndDataFiles(S3ManifestFile manifestFile) {
    transactionManager.executeProcedure(entityManager -> entityManager.merge(manifestFile));
  }

  /**
   * Finds recent manifests in the database that have status values indicating they are not eligible
   * for processing and returns their S3 keys. Used to quickly eliminate S3 manifest files from
   * consideration based on their existing status in database.
   *
   * @param minTimestamp only return records from this time forward
   * @return immutable set of s3 keys for ineligible manifests
   */
  public Set<String> readIneligibleManifestS3Keys(Instant minTimestamp) {
    return transactionManager.executeFunction(
        entityManager -> {
          final var records =
              entityManager
                  .createQuery(
                      "select m.s3Key from S3ManifestFile m where (m.discoveryTimestamp >= :minTimestamp) and (m.status not in :okStatus)",
                      String.class)
                  .setParameter("minTimestamp", minTimestamp)
                  .setParameter(
                      "okStatus",
                      Set.of(
                          S3ManifestFile.ManifestStatus.DISCOVERED,
                          S3ManifestFile.ManifestStatus.STARTED))
                  .getResultList();
          return Set.copyOf(records);
        });
  }

  /**
   * Searches for a record matching the provided S3 key and returns it if one is found. Returns null
   * if no record is found. Only intended for use in tests. Use {@link
   * #insertOrReadManifestAndDataFiles} for actual processing by ETL pipeline.
   *
   * @param manifestS3Key S3 key of the manifest file
   * @return null or the manifest entity
   */
  @VisibleForTesting
  @Nullable
  public S3ManifestFile readS3ManifestAndDataFiles(String manifestS3Key) {
    return transactionManager.executeFunction(
        entityManager -> readS3ManifestAndDataFilesImpl(manifestS3Key, entityManager));
  }

  /**
   * Finds a record matching the given S3 key and returns an entity representing it. Returns null if
   * no record exists.
   *
   * @param manifestS3Key S3 key of the manifest file
   * @param entityManager used to query the database
   * @return null or the record entity
   */
  @Nullable
  private S3ManifestFile readS3ManifestAndDataFilesImpl(
      String manifestS3Key, EntityManager entityManager) {
    final var records =
        entityManager
            .createQuery("select m from S3ManifestFile m where s3Key=:s3Key", S3ManifestFile.class)
            .setParameter("s3Key", manifestS3Key)
            .getResultList();
    if (records.isEmpty()) {
      return null;
    } else {
      return records.getFirst();
    }
  }

  /**
   * Check the database representation of a manifest against the actual manifest as loaded from S3
   * to confirm that they are consistent. Specifically that the number, name, type, and order of
   * data files match.
   *
   * @param xmlManifest real manifest data
   * @param dbManifest database representation to check for consistency
   * @throws DataSetOutOfSyncException if any discrepancy is found
   */
  private void verifyExistingRecordMatchesDataSetManifest(
      DataSetManifest xmlManifest, S3ManifestFile dbManifest) throws DataSetOutOfSyncException {
    if (xmlManifest.getEntries().size() != dbManifest.getDataFiles().size()) {
      throw new DataSetOutOfSyncException(
          xmlManifest,
          dbManifest,
          "number of entries do not match: db=%d xml=%d",
          dbManifest.getDataFiles().size(),
          xmlManifest.getEntries().size());
    }

    int index = 0;
    while (index < xmlManifest.getEntries().size()) {
      final var xmlEntry = xmlManifest.getEntries().get(index);
      final var dbEntry = dbManifest.getDataFiles().get(index);
      if (!xmlEntry.getName().equals(dbEntry.getFileName())) {
        throw new DataSetOutOfSyncException(
            xmlManifest,
            dbManifest,
            "entry name mismatch: index=%d db=%s xml=%s",
            index,
            dbEntry.getFileName(),
            xmlEntry.getName());
      } else if (!xmlEntry.getType().name().equals(dbEntry.getFileType())) {
        throw new DataSetOutOfSyncException(
            xmlManifest,
            dbManifest,
            "entry type mismatch: index=%d name=%s db=%s xml=%s",
            index,
            xmlEntry.getName(),
            dbEntry.getFileType(),
            xmlEntry.getType());
      }
      index += 1;
    }
  }

  /**
   * Constructs a new {@link S3ManifestFile} and associated {@link S3DataFile} objects to represent
   * the specified {@link DataSetManifest} in the database.
   *
   * @param manifestS3Key S3 key for the manifest
   * @param manifestFileData the manifest as parsed from XML
   * @param now timestamp to use for current time
   * @return the new entity object
   */
  private S3ManifestFile createNewManifestAndFiles(
      String manifestS3Key, DataSetManifest manifestFileData, Instant now) {
    var manifest = new S3ManifestFile();
    manifest.setS3Key(manifestS3Key);
    manifest.setStatus(S3ManifestFile.ManifestStatus.DISCOVERED);
    manifest.setStatusTimestamp(now);
    manifest.setManifestTimestamp(manifestFileData.getTimestamp());
    manifest.setDiscoveryTimestamp(now);
    List<S3DataFile> dataFiles = manifest.getDataFiles();
    final String dataFileS3KeyPrefix = S3FileManager.extractPrefixFromS3Key(manifestS3Key);
    short index = 0;
    for (var entry : manifestFileData.getEntries()) {
      var dataFile = new S3DataFile();
      dataFile.setParentManifest(manifest);
      dataFile.setIndex(index);
      dataFile.setS3Key(dataFileS3KeyPrefix + entry.getName());
      dataFile.setFileName(entry.getName());
      dataFile.setFileType(entry.getType().toString());
      dataFile.setStatus(S3DataFile.FileStatus.DISCOVERED);
      dataFile.setStatusTimestamp(now);
      dataFile.setDiscoveryTimestamp(now);
      dataFiles.add(dataFile);
      index += 1;
    }
    return manifest;
  }

  /**
   * Exception thrown when a mismatch is detected between the contents of a manifest file and its
   * representation in the database.
   */
  public static class DataSetOutOfSyncException extends RuntimeException {
    /** The {@link DataSetManifest} we expected to match. */
    @Getter private final DataSetManifest xmlManifest;

    /** The {@link S3ManifestFile} that did not match. */
    @Getter private final S3ManifestFile dbManifest;

    /**
     * Initializes an instance with an error message formed using {@link String#format} and the
     * provided format string and optional arguments.
     *
     * @param xmlManifest manifest as loaded from S3 XML file
     * @param dbManifest manifest as loaded from database
     * @param message message format string
     * @param args optional array of arguments for message
     */
    public DataSetOutOfSyncException(
        DataSetManifest xmlManifest, S3ManifestFile dbManifest, String message, Object... args) {
      super(String.format(message, args));
      this.xmlManifest = xmlManifest;
      this.dbManifest = dbManifest;
    }
  }
}
