package gov.cms.bfd.pipeline.rda.grpc.source;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Object that can convert a FissClaim from the RDA API into a PreAdjFissClaim suitable for writing
 * to the database. Validates all fields in the API record and throws an exception if there are any
 * invalid fields.
 */
public class DataTransformer {
  private final Map<String, List<String>> errors = new HashMap<>();

  public boolean isSuccessful() {
    return errors.isEmpty();
  }

  public Map<String, List<String>> getErrors() {
    final ImmutableMap.Builder<String, List<String>> answer = ImmutableMap.builder();
    for (Map.Entry<String, List<String>> entry : errors.entrySet()) {
      answer.put(entry.getKey(), ImmutableList.copyOf(entry.getValue()));
    }
    return answer.build();
  }

  public DataTransformer copyString(
      String fieldName,
      String value,
      boolean nullable,
      int minLength,
      int maxLength,
      Consumer<String> copier) {
    if (nonNull(fieldName, nullable, value) && lengthOk(fieldName, value, minLength, maxLength)) {
      copier.accept(value);
    }
    return this;
  }

  public DataTransformer copyHashedString(
      String fieldName,
      String value,
      boolean nullable,
      int minLength,
      int maxLength,
      Consumer<String> copier) {
    if (nonNull(fieldName, nullable, value) && lengthOk(fieldName, value, minLength, maxLength)) {
      // TODO plug in real hasher here
      Hasher hasher = Hashing.sha256().newHasher();
      hasher.putString(value, Charsets.UTF_8);
      copier.accept(hasher.hash().toString());
    }
    return this;
  }

  public DataTransformer copyCharacter(String fieldName, String value, Consumer<Character> copier) {
    if (nonNull(fieldName, false, value) && lengthOk(fieldName, value, 1, 1)) {
      copier.accept(value.charAt(0));
    }
    return this;
  }

  public DataTransformer copyDate(
      String fieldName, String value, boolean nullable, Consumer<LocalDate> copier) {
    if (nonNull(fieldName, nullable, value)) {
      try {
        LocalDate date = LocalDate.parse(value);
        copier.accept(date);
      } catch (DateTimeParseException ex) {
        addError(fieldName, "invalid date");
      }
    }
    return this;
  }

  public DataTransformer copyAmount(
      String fieldName, String value, boolean nullable, Consumer<BigDecimal> copier) {
    if (nonNull(fieldName, nullable, value)) {
      try {
        BigDecimal amount = new BigDecimal(value);
        copier.accept(amount);
      } catch (DateTimeParseException ex) {
        addError(fieldName, "invalid amount");
      }
    }
    return this;
  }

  private boolean nonNull(String fieldName, boolean nullable, Object value) {
    if (value != null) {
      return true;
    }
    if (nullable) {
      return true;
    }
    addError(fieldName, "is null");
    return false;
  }

  private boolean lengthOk(String fieldName, String value, int minLength, int maxLength) {
    final int length = value.length();
    if (length < minLength || length > maxLength) {
      addError(
          fieldName, "invalid length: expected=[%d,%d] actual=%d", minLength, maxLength, length);
      return false;
    }
    return true;
  }

  private void addError(String fieldName, String errorFormat, Object... args) {
    final String message = String.format(errorFormat, args);
    final List<String> messageList = errors.computeIfAbsent(fieldName, k -> new ArrayList<>());
    messageList.add(message);
  }
}
