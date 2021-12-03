package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.Assert.*;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The claim transformers need to apply specific tests based on the type of field being converted.
 * These tests have a lot of commonality but work on different protobuf and JPA objects that aren't
 * connected through inheritance or any interfaces. This class allows derived classes specific to
 * each transformer's test class to implement specific methods required to run the field tests.
 *
 * @param <TClaimBuilder> class of the protobuf message object's builder (e.g. FissClaim.Builder)
 * @param <TClaim> class of the protobuf message object (e.g. FissClaim)
 * @param <TClaimEntity> class of the JPA entity for a claim (e.g. PreAdjFissClaim)
 * @param <TTestEntityBuilder> class of the protobuf test object's builder (e.g.
 *     FissInsuredPayer.Builder)
 * @param <TTestEntity> class of the JPA entity for an object containing a field (e.g.
 *     PreAdjFissPayer)
 */
public abstract class ClaimTransformerFieldTester<
    TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity> {
  @CanIgnoreReturnValue
  ClaimTransformerFieldTester<TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
      stringField(
          BiConsumer<TTestEntityBuilder, String> setter,
          Function<TTestEntity, String> getter,
          String fieldLabel,
          int maxLength) {
    final BiConsumer<TClaimBuilder, String> wrappedSetter =
        (claimBuilder, value) -> setter.accept(getTestEntityBuilder(claimBuilder), value);
    final Function<TClaimEntity, String> wrappedGetter =
        claim -> getter.apply(getTestEntity(claim));
    final String wrappedFieldLabel = getLabel(fieldLabel);
    verifyStringFieldTransformationCorrect(wrappedSetter, wrappedGetter, maxLength);
    verifyStringFieldLengthLimitsEnforced(wrappedSetter, wrappedFieldLabel, maxLength, 0);
    verifyStringFieldLengthLimitsEnforced(
        wrappedSetter, wrappedFieldLabel, maxLength, maxLength + 1);
    return this;
  }

  @CanIgnoreReturnValue
  ClaimTransformerFieldTester<TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
      dateField(
          BiConsumer<TTestEntityBuilder, String> setter,
          Function<TTestEntity, LocalDate> getter,
          String fieldLabel) {
    final BiConsumer<TClaimBuilder, String> wrappedSetter =
        (claimBuilder, value) -> setter.accept(getTestEntityBuilder(claimBuilder), value);
    final Function<TClaimEntity, LocalDate> wrappedGetter =
        claim -> getter.apply(getTestEntity(claim));
    verifyFieldTransformationSucceeds(
        claimBuilder -> wrappedSetter.accept(claimBuilder, "2021-12-01"),
        wrappedGetter,
        LocalDate.of(2021, 12, 1));
    verifyFieldTransformationFails(
        claimBuilder -> wrappedSetter.accept(claimBuilder, "not-a-date"),
        getLabel(fieldLabel),
        "invalid date");
    return this;
  }

  @CanIgnoreReturnValue
  ClaimTransformerFieldTester<TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
      amountField(
          BiConsumer<TTestEntityBuilder, String> setter,
          Function<TTestEntity, BigDecimal> getter,
          String fieldLabel) {
    final BiConsumer<TClaimBuilder, String> wrappedSetter =
        (claimBuilder, value) -> setter.accept(getTestEntityBuilder(claimBuilder), value);
    final Function<TClaimEntity, BigDecimal> wrappedGetter =
        claim -> getter.apply(getTestEntity(claim));
    verifyFieldTransformationSucceeds(
        claimBuilder -> wrappedSetter.accept(claimBuilder, "1234.50"),
        wrappedGetter,
        new BigDecimal("1234.50"));
    verifyFieldTransformationFails(
        claimBuilder -> wrappedSetter.accept(claimBuilder, "not-a-number"),
        getLabel(fieldLabel),
        "invalid amount");
    return this;
  }

  @CanIgnoreReturnValue
  <TEnum extends Enum<?>>
      ClaimTransformerFieldTester<
              TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
          enumField(
              BiConsumer<TTestEntityBuilder, TEnum> setter,
              Function<TTestEntity, String> getter,
              TEnum enumValue,
              String stringValue) {
    verifyFieldTransformationSucceeds(
        claimBuilder -> setter.accept(getTestEntityBuilder(claimBuilder), enumValue),
        claim1 -> getter.apply(getTestEntity(claim1)),
        stringValue);
    return this;
  }

  /**
   * Overridden to access a builder object from the claimBuilder so that it can be used to assign
   * values in a test.
   *
   * @param claimBuilder the builder being used to construct a claim
   * @return a builder for the object within the claim that we want to test
   */
  abstract TTestEntityBuilder getTestEntityBuilder(TClaimBuilder claimBuilder);

  /**
   * Overridden to access an entity object within a claim entity so that it can be tested.
   *
   * @param claim the claim entity
   * @return the entity within the claim that we want to be able to test
   */
  abstract TTestEntity getTestEntity(TClaimEntity claim);

  /**
   * Adds any necessary prefix to the provided error label. This is generally something like
   * "payer-0-".
   *
   * @param basicLabel generally the raw field name
   * @return the label with any necessary prefix
   */
  String getLabel(String basicLabel) {
    return basicLabel;
  }

  /**
   * Overridden to produce a protobuf builder object for the root message/claim object to be
   * transformed.
   *
   * @return A valid builder.
   */
  abstract TClaimBuilder createClaimBuilder();

  /**
   * Finishes building a message/claim object. Generally just a call to builder.build().
   *
   * @param builder the builder
   * @return the result of calling the build method on the builder
   */
  abstract TClaim buildClaim(TClaimBuilder builder);

  /**
   * Runs the transformation to convert the claim/message into its corresponding entity object.
   *
   * @param claim the claim to transform
   * @return an entity produced as a result of the transformation
   */
  abstract RdaChange<TClaimEntity> transformClaim(TClaim claim);

  private void verifyStringFieldTransformationCorrect(
      BiConsumer<TClaimBuilder, String> setter,
      Function<TClaimEntity, String> getter,
      int maxLength) {
    final var value = createString(maxLength);
    verifyFieldTransformationSucceeds(
        claimBuilder -> setter.accept(claimBuilder, value), getter, value);
  }

  private void verifyStringFieldLengthLimitsEnforced(
      BiConsumer<TClaimBuilder, String> setter, String fieldLabel, int maxLength, int length) {
    verifyFieldTransformationFails(
        claimBuilder -> setter.accept(claimBuilder, createString(length)),
        fieldLabel,
        String.format("invalid length: expected=[1,%d] actual=%d", maxLength, length));
  }

  private <T> void verifyFieldTransformationSucceeds(
      Consumer<TClaimBuilder> setter, Function<TClaimEntity, T> getter, T expectedValue) {
    var claimBuilder = createClaimBuilder();

    setter.accept(claimBuilder);

    final var change = transformClaim(buildClaim(claimBuilder));
    assertEquals(expectedValue, getter.apply(change.getClaim()));
  }

  private void verifyFieldTransformationFails(
      Consumer<TClaimBuilder> setter, String fieldLabel, String... errorMessages) {
    try {
      var claimBuilder = createClaimBuilder();

      setter.accept(claimBuilder);

      transformClaim(buildClaim(claimBuilder));
      fail("should have thrown");
    } catch (DataTransformer.TransformationException ex) {
      var errors = ImmutableList.builder();
      for (String errorMessage : errorMessages) {
        errors.add(new DataTransformer.ErrorMessage(fieldLabel, errorMessage));
      }
      assertEquals(errors.build(), ex.getErrors());
    }
  }

  private String createString(int length) {
    StringBuilder sb = new StringBuilder();
    var digit = 1;
    while (sb.length() < length) {
      sb.append(digit);
      digit = (digit + 1) % 10;
    }
    return sb.toString();
  }
}
