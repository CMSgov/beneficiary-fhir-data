package gov.cms.bfd.pipeline.ccw.rif.extract.s3;

import gov.cms.bfd.model.rif.entities.S3ManifestFile;
import gov.cms.bfd.pipeline.sharedutils.TransactionManager;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;

/** Data access object for working with S3 file entities. */
@RequiredArgsConstructor
public class S3FilesDao {
  private final TransactionManager transactionManager;

  @Nullable
  S3ManifestFile readS3ManifestAndDataFiles(String manifestS3Key) {
    return transactionManager.executeFunction(
        entityManager -> {
          final var records =
              entityManager
                  .createQuery(
                      "select m from S3ManifestFile m where s3Path=:s3Path", S3ManifestFile.class)
                  .setParameter("s3Path", manifestS3Key)
                  .getResultList();
          if (records.isEmpty()) {
            return null;
          } else {
            return records.getFirst();
          }
        });
  }

  void insertS3ManifestAndDataFiles(S3ManifestFile manifestFile) {
    transactionManager.executeProcedure(entityManager -> entityManager.persist(manifestFile));
  }

  void updateS3ManifestAndDataFiles(S3ManifestFile manifestFile) {
    transactionManager.executeProcedure(entityManager -> entityManager.merge(manifestFile));
  }
}
