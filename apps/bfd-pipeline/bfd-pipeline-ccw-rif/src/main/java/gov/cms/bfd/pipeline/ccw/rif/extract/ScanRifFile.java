package gov.cms.bfd.pipeline.ccw.rif.extract;

import static org.apache.commons.lang3.builder.EqualsBuilder.reflectionEquals;

import gov.cms.bfd.model.rif.RifFile;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ScanRifFile {
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.err.printf("usage: %s csv-name parquet-name%n", ScanRifFile.class.getSimpleName());
      System.exit(1);
    }

    Map<Long, HHAClaim> csvClaims = new HashMap<>();
    Map<Long, HHAClaim> parquetClaims = new HashMap<>();

    RifFile csvFile = new LocalRifFile(new File(args[0]), RifFileType.HHA);
    RifFile parquetFile = new LocalRifFile(new File(args[1]), RifFileType.HHA);

    RifFilesEvent csvEvent = new RifFilesEvent(Instant.now(), false, csvFile);
    RifFilesEvent parquetEvent = new RifFilesEvent(Instant.now(), false, parquetFile);

    RifFileRecords csvRecords =
        new RifFilesProcessor().produceRecords(csvEvent.getFileEvents().getFirst());
    RifFileRecords parquetRecords =
        new RifFilesProcessor().produceRecords(parquetEvent.getFileEvents().getFirst());

    System.out.println(new Date() + ": reading parquet claims...");
    parquetRecords
        .getRecords()
        .doOnNext(
            record -> {
              HHAClaim claim = (HHAClaim) record.getRecord();
              parquetClaims.put(claim.getClaimId(), claim);
            })
        .count()
        .block();
    System.out.println(new Date() + ": reading csv claims...");
    csvRecords
        .getRecords()
        .doOnNext(
            record -> {
              HHAClaim claim = (HHAClaim) record.getRecord();
              csvClaims.put(claim.getClaimId(), claim);
            })
        .count()
        .block();

    System.out.println(new Date() + ": comparing sizes...");
    if (csvClaims.size() != parquetClaims.size()) {
      System.out.printf("claims size mismatch: %s != %s%n", csvClaims.size(), parquetClaims.size());
      return;
    }

    System.out.println(new Date() + ": comparing claims...");
    for (HHAClaim parquetClaim : parquetClaims.values()) {
      HHAClaim csvClaim = csvClaims.get(parquetClaim.getClaimId());
      if (csvClaim == null) {
        System.out.printf(new Date() + ": no match for claimId %s%n", parquetClaim.getClaimId());
        return;
      }
      if (!reflectionEquals(csvClaim, parquetClaim, "lines")) {
        System.out.printf(
            new Date() + ": claim reflection equals returned false for claimId %s%n",
            parquetClaim.getClaimId());
        return;
      }
      var csvLines = csvClaim.getLines();
      var parquetLines = parquetClaim.getLines();
      if (csvLines.size() != parquetLines.size()) {
        System.out.printf(
            new Date() + ": lines size mismatch: %s != %s%n", csvLines.size(), parquetLines.size());
      }
      for (int i = 0; i < csvLines.size(); ++i) {
        if (!reflectionEquals(csvLines.get(i), parquetLines.get(i), "parentClaim")) {
          System.out.printf(
              new Date() + ": line reflection equals returned false for claimId %s lineNum %s/%s%n",
              parquetClaim.getClaimId(),
              csvLines.get(i).getLineNumber(),
              parquetLines.get(i).getLineNumber());
          return;
        }
      }
    }
    System.out.printf("All claims were equal: count=%d%n", parquetClaims.size());
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
