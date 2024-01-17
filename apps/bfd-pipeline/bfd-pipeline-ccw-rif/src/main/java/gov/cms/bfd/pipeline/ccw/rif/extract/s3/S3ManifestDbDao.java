package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import gov.cms.bfd.model.rif.entities.S3DataFile;
import gov.cms.bfd.model.rif.entities.S3ManifestFile;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/** Data access object for working with S3 manifest database entities. */
@RequiredArgsConstructor
public class S3ManifestDbDao {
  private final TransactionManager transactionManager;

  @Nullable
  public S3ManifestFile readS3ManifestAndDataFiles(String manifestS3Key) {
    return transactionManager.executeFunction(
        entityManager -> readS3ManifestAndDataFilesImpl(manifestS3Key, entityManager));
  }

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

  public void updateS3ManifestAndDataFiles(S3ManifestFile manifestFile) {
    transactionManager.executeProcedure(entityManager -> entityManager.merge(manifestFile));
  }

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

  private void verifyExistingRecordMatchesDataSetManifest(
      DataSetManifest xmlManifest, S3ManifestFile dbManifest) throws DataSetOutOfSyncException {
    if (xmlManifest.getEntries().size() != dbManifest.getDataFiles().size()) {
      throw new DataSetOutOfSyncException(
          "number of entries do not match: db=%d xml=%d",
          dbManifest.getDataFiles().size(), xmlManifest.getEntries().size());
    }

    int index = 0;
    while (index < xmlManifest.getEntries().size()) {
      final var xmlEntry = xmlManifest.getEntries().get(index);
      final var dbEntry = dbManifest.getDataFiles().get(index);
      if (!xmlEntry.getName().equals(dbEntry.getFileName())) {
        throw new DataSetOutOfSyncException(
            "entry name mismatch: index=%d db=%s xml=%s",
            index, xmlEntry.getName(), dbEntry.getFileName());
      } else if (!xmlEntry.getType().name().equals(dbEntry.getFileType())) {
        throw new DataSetOutOfSyncException(
            "entry type mismatch: index=%d name=%s db=%s xml=%s",
            index, xmlEntry.getName(), xmlEntry.getType(), dbEntry.getFileType());
      }
      index += 1;
    }
  }

  public static class DataSetOutOfSyncException extends RuntimeException {
    public DataSetOutOfSyncException(String message, Object... args) {
      super(String.format(message, args));
    }
  }

  private S3ManifestFile createNewManifestAndFiles(
      String manifestS3Key, DataSetManifest manifestFileData, Instant now) {
    var manifest = new S3ManifestFile();
    manifest.setS3Key(manifestS3Key);
    manifest.setStatus(S3ManifestFile.ManifestStatus.DISCOVERED);
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
      dataFile.setDiscoveryTimestamp(now);
      dataFiles.add(dataFile);
      index += 1;
    }
    return manifest;
  }
}
