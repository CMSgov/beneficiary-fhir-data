package gov.cms.bfd.model.rda;

import java.time.Instant;
import javax.annotation.Nullable;

/**
 * This class provides static convenience methods for working with the generated {@link Mbi} class.
 */
public final class MbiUtil {
  public static Mbi.MbiBuilder builder(String mbi, String hash) {
    return Mbi.builder().mbi(mbi).hash(hash).lastUpdated(Instant.now());
  }

  public static Mbi newMbi(String mbi, String hash) {
    return builder(mbi, hash).build();
  }

  public static Mbi nonNull(@Nullable Mbi possiblyNullRecord) {
    return possiblyNullRecord == null ? new Mbi() : possiblyNullRecord;
  }

  public static final class McsFields {
    public static final String mbi = "idrClaimMbi";
  }
}
