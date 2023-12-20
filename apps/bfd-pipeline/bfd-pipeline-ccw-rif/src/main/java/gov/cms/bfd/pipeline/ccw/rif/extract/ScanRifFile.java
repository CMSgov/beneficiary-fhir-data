package gov.cms.bfd.pipeline.ccw.rif.extract;

import gov.cms.bfd.model.rif.RifFile;
import gov.cms.bfd.model.rif.RifFileEvent;
import gov.cms.bfd.model.rif.RifFileType;
import gov.cms.bfd.model.rif.RifFilesEvent;
import gov.cms.bfd.model.rif.entities.HHAClaim;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.lang3.builder.EqualsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class ScanRifFile {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.err.printf("usage: %s csv-name parquet-name%n", ScanRifFile.class.getSimpleName());
      System.exit(1);
    }

    RifFile csvFile = new LocalRifFile(new File(args[0]), RifFileType.HHA);
    RifFile parquetFile = new LocalRifFile(new File(args[1]), RifFileType.HHA);
    RifFilesEvent csvEvent = new RifFilesEvent(Instant.now(), false, csvFile);
    RifFilesEvent parquetEvent = new RifFilesEvent(Instant.now(), false, parquetFile);
    new RifFileEvent(parquetEvent, parquetFile);
    RifFileRecords csvRecords =
        new RifFilesProcessor().produceRecords(csvEvent.getFileEvents().getFirst());
    RifFileRecords parquetRecords =
        new RifFilesProcessor().produceRecords(parquetEvent.getFileEvents().getFirst());
    var recordNumber = new AtomicLong();
    Mono<Boolean> equals =
        Flux.zip(
                csvRecords.getRecords(),
                parquetRecords.getRecords(),
                (csvRecord, parquetRecord) -> {
                  recordNumber.incrementAndGet();
                  if (recordNumber.get() % 1000 == 0) {
                    System.out.printf("processed %d so far...%n", recordNumber.get());
                  }
                  HHAClaim csvClaim = (HHAClaim) csvRecord.getRecord();
                  HHAClaim parquetClaim = (HHAClaim) csvRecord.getRecord();
                  if (csvClaim.getBeneficiaryId() != parquetClaim.getBeneficiaryId()) {
                    System.out.printf(
                        "bene_id mismatch: %s != %s%n",
                        csvClaim.getBeneficiaryId(), parquetClaim.getBeneficiaryId());
                    return false;
                  }
                  if (csvClaim.getClaimId() != parquetClaim.getClaimId()) {
                    System.out.printf(
                        "claim_id mismatch: %s != %s%n",
                        csvClaim.getClaimId(), parquetClaim.getClaimId());
                    return false;
                  }
                  if (csvClaim.getLines().size() != parquetClaim.getLines().size()) {
                    System.out.printf(
                        "line count mismatch: %s != %s%n",
                        csvClaim.getLines().size(), parquetClaim.getLines().size());
                    return false;
                  }
                  if (!EqualsBuilder.reflectionEquals(csvClaim, parquetClaim)) {
                    System.out.println("reflection equals returned false");
                    return false;
                  }

                  return true;
                })
            .all(x -> x);
    System.out.printf("Final result is %s even using reflection equals!%n", equals.blockOptional());
  }

  public static class LocalRifFile implements RifFile {
    /** The local file. */
    private final File localFile;

    /** The file type. */
    private final RifFileType rifFileType;

    /**
     * Constructs a new {@link LocalRifFile}.
     *
     * @param localFile the {@link Path} of the local file being represented
     * @param rifFileType the {@link RifFileType} of the file
     */
    public LocalRifFile(File localFile, RifFileType rifFileType) {
      this.localFile = localFile;
      this.rifFileType = rifFileType;
    }

    @Override
    public String getDisplayName() {
      return localFile.getAbsolutePath();
    }

    @Override
    public RifFileType getFileType() {
      return rifFileType;
    }

    @Override
    public Charset getCharset() {
      return StandardCharsets.UTF_8;
    }

    @Override
    public InputStream open() {
      try {
        return new BufferedInputStream(new FileInputStream(localFile));
      } catch (FileNotFoundException e) {
        throw new UncheckedIOException(e);
      }
    }

    @Override
    public File getFile() {
      return localFile;
    }
  }
}
