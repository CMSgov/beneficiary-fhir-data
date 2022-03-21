package gov.cms.bfd.pipeline.rda.grpc.source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import gov.cms.bfd.pipeline.rda.grpc.RdaChange;
import gov.cms.bfd.pipeline.sharedutils.IdHasher;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * The claim transformers {@link FissClaimTransformer} and {@link McsClaimTransformer} combined
 * populate 11 different JPA entity classes with data from corresponding gRPC API message objects.
 * The tests for these transformations need to apply specific checks for every field based on the
 * type of field being converted. These tests have a lot of commonality but work on different
 * protobuf and JPA objects that aren't connected through inheritance or any interfaces.
 *
 * <p>This abstract class implements the field checks using a set of abstract methods overridden by
 * entity specific derived classes to instantiate message and entity objects of the correct type.
 *
 * @param <TClaimBuilder> class of the protobuf message object's builder (e.g. FissClaim.Builder)
 * @param <TClaim> class of the protobuf message object (e.g. FissClaim)
 * @param <TClaimEntity> class of the JPA entity for a claim (e.g. {@link
 *     gov.cms.bfd.model.rda.PartAdjFissClaim})
 * @param <TTestEntityBuilder> class of the protobuf test object's builder (e.g.
 *     FissInsuredPayer.Builder)
 * @param <TTestEntity> class of the JPA entity for an object containing a field (e.g. {@link
 *     gov.cms.bfd.model.rda.PartAdjFissPayer})
 */
public abstract class ClaimTransformerFieldTester<
    TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity> {
  /**
   * Verifies that a string field transformation is working properly. This includes verifying that a
   * value is properly copied from the message object to the entity object and that minimum and
   * maximum length validations are performed. This method uses a minimum length of 0 for the field.
   *
   * @param setter method reference or lambda to set a value of the field being tested on a message
   *     object
   * @param getter method reference of lambda to get a value of the field being tested from an
   *     entity object
   * @param fieldLabel text identifying the field in {@link
   *     gov.cms.bfd.pipeline.rda.grpc.source.DataTransformer.TransformationException error
   *     messages}
   * @param minLength minimum valid length for the string field
   * @param maxLength maximum valid length for the string field
   * @return this object so that calls can be chained
   */
  @CanIgnoreReturnValue
  ClaimTransformerFieldTester<TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
      verifyStringFieldCopiedCorrectlyEmptyOK(
          BiConsumer<TTestEntityBuilder, String> setter,
          Function<TTestEntity, String> getter,
          String fieldLabel,
          int maxLength) {
    return verifyStringFieldCopiedCorrectlyImpl(setter, getter, fieldLabel, 0, maxLength);
  }

  /**
   * Verifies that a string field transformation is working properly. This includes verifying that a
   * value is properly copied from the message object to the entity object and that minimum and
   * maximum length validations are performed. This method uses a minimum length of 1 for the field.
   *
   * @param setter method reference or lambda to set a value of the field being tested on a message
   *     object
   * @param getter method reference of lambda to get a value of the field being tested from an
   *     entity object
   * @param fieldLabel text identifying the field in {@link
   *     gov.cms.bfd.pipeline.rda.grpc.source.DataTransformer.TransformationException error
   *     messages}
   * @param maxLength maximum valid length for the string field
   * @return this object so that calls can be chained
   */
  @CanIgnoreReturnValue
  ClaimTransformerFieldTester<TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
      verifyStringFieldCopiedCorrectly(
          BiConsumer<TTestEntityBuilder, String> setter,
          Function<TTestEntity, String> getter,
          String fieldLabel,
          int maxLength) {
    return verifyStringFieldCopiedCorrectlyImpl(setter, getter, fieldLabel, 1, maxLength);
  }

  /**
   * Verifies that a string field transformation is working properly. This includes verifying that a
   * value is properly copied from the message object to the entity object and that minimum and
   * maximum length validations are performed.
   *
   * @param setter method reference or lambda to set a value of the field being tested on a message
   *     object
   * @param getter method reference of lambda to get a value of the field being tested from an
   *     entity object
   * @param fieldLabel text identifying the field in {@link
   *     gov.cms.bfd.pipeline.rda.grpc.source.DataTransformer.TransformationException error
   *     messages}
   * @param minLength minimum valid length for the string field
   * @param maxLength maximum valid length for the string field
   * @return this object so that calls can be chained
   */
  @CanIgnoreReturnValue
  ClaimTransformerFieldTester<TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
      verifyStringFieldCopiedCorrectlyImpl(
          BiConsumer<TTestEntityBuilder, String> setter,
          Function<TTestEntity, String> getter,
          String fieldLabel,
          int minLength,
          int maxLength) {
    final BiConsumer<TClaimBuilder, String> wrappedSetter =
        (claimBuilder, value) -> setter.accept(getTestEntityBuilder(claimBuilder), value);
    final Function<TClaimEntity, String> wrappedGetter =
        claim -> getter.apply(getTestEntity(claim));
    final String wrappedFieldLabel = getLabel(fieldLabel);
    // limits the length os string tested for clob/text fields
    verifyStringFieldTransformationCorrect(
        wrappedSetter, wrappedGetter, Math.min(10000, maxLength));
    if (minLength > 0) {
      verifyStringFieldLengthLimitsEnforced(
          wrappedSetter, wrappedFieldLabel, minLength, maxLength, minLength - 1);
    }
    if (maxLength < Integer.MAX_VALUE) {
      verifyStringFieldLengthLimitsEnforced(
          wrappedSetter, wrappedFieldLabel, minLength, maxLength, maxLength + 1);
    }
    return this;
  }

  /**
   * Verifies that a string field transformation always sets a certain field to the hashed value of
   * another field.
   *
   * @param setter method reference or lambda to set a value of the field being hashed on a message
   *     object
   * @param getter method reference of lambda to get a value of the hashed value field from an
   *     entity object
   * @param maxLength maximum valid length for the string field being hashed
   * @param hasher {@link gov.cms.bfd.pipeline.sharedutils.IdHasher} object used to compute a hashed
   *     value
   * @return this object so that calls can be chained
   */
  @CanIgnoreReturnValue
  ClaimTransformerFieldTester<TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
      verifyIdHashFieldPopulatedCorrectly(
          BiConsumer<TTestEntityBuilder, String> setter,
          Function<TTestEntity, String> getter,
          int maxLength,
          IdHasher hasher) {
    final BiConsumer<TClaimBuilder, String> wrappedSetter =
        (claimBuilder, value) -> setter.accept(getTestEntityBuilder(claimBuilder), value);
    final Function<TClaimEntity, String> wrappedGetter =
        claim -> getter.apply(getTestEntity(claim));
    final String valueToHash = createString(maxLength);
    final String hashValue = hasher.computeIdentifierHash(valueToHash);
    verifyFieldTransformationSucceeds(
        claimBuilder -> wrappedSetter.accept(claimBuilder, valueToHash), wrappedGetter, hashValue);
    return this;
  }

  /**
   * Verifies that a date field transformation is working properly. This includes verifying that a
   * value is properly copied from the message object to the entity object and that invalid date
   * strings generate exceptions.
   *
   * @param setter method reference or lambda to set a value of the field being tested on a message
   *     object
   * @param getter method reference of lambda to get a value of the field being tested from an
   *     entity object
   * @param fieldLabel text identifying the field in {@link
   *     gov.cms.bfd.pipeline.rda.grpc.source.DataTransformer.TransformationException error
   *     messages}
   * @return this object so that calls can be chained
   */
  @CanIgnoreReturnValue
  ClaimTransformerFieldTester<TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
      verifyDateStringFieldTransformedCorrectly(
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

  /**
   * Verifies that an amount field transformation is working properly. This includes verifying that
   * a value is properly copied from the message object to the entity object and that invalid
   * decimal strings generate exceptions.
   *
   * @param setter method reference or lambda to set a value of the field being tested on a message
   *     object
   * @param getter method reference of lambda to get a value of the field being tested from an
   *     entity object
   * @param fieldLabel text identifying the field in {@link
   *     gov.cms.bfd.pipeline.rda.grpc.source.DataTransformer.TransformationException error
   *     messages}
   * @return this object so that calls can be chained
   */
  @CanIgnoreReturnValue
  ClaimTransformerFieldTester<TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
      verifyAmountStringFieldTransformedCorrectly(
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

  /**
   * Verifies that a int field transformation is working properly. This includes verifying that a
   * value is properly copied from the message object to the entity object.
   *
   * @param setter method reference or lambda to set a value of the field being tested on a message
   *     object
   * @param getter method reference of lambda to get a value of the field being tested from an
   *     entity object
   * @return this object so that calls can be chained
   */
  @CanIgnoreReturnValue
  ClaimTransformerFieldTester<TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
      verifyIntFieldCopiedCorrectly(
          BiConsumer<TTestEntityBuilder, Integer> setter, Function<TTestEntity, Integer> getter) {
    final BiConsumer<TClaimBuilder, Integer> wrappedSetter =
        (claimBuilder, value) -> setter.accept(getTestEntityBuilder(claimBuilder), value);
    final Function<TClaimEntity, Integer> wrappedGetter =
        claim -> getter.apply(getTestEntity(claim));
    verifyFieldTransformationSucceeds(
        claimBuilder -> wrappedSetter.accept(claimBuilder, 1234), wrappedGetter, 1234);
    return this;
  }

  /**
   * Verifies that an enum field transformation is working properly. This includes verifying that a
   * value is properly copied from the message object to the entity object and that the copied value
   * matches the expected string value associated with the enum.
   *
   * @param setter method reference or lambda to set a value of the field being tested on a message
   *     object
   * @param getter method reference of lambda to get a value of the field being tested from an
   *     entity object
   * @param enumValue a valid enum value to be set on the message object
   * @param stringValue the expected matching string value associated with the provided enumValue
   * @return this object so that calls can be chained
   */
  @CanIgnoreReturnValue
  <TEnum extends Enum<?>>
      ClaimTransformerFieldTester<
              TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
          verifyEnumFieldStringValueExtractedCorrectly(
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
   * Verifies that an enum field transformation that has been configured to reject unrecognized
   * values throws a proper error.
   *
   * @param setter method reference or lambda to set a value of the field being tested on a message
   *     object
   * @param fieldLabel text identifying the field in {@link DataTransformer.TransformationException
   *     error messages}
   * @param badStringValue a string value that should throw an error indicating the value is
   *     unsupported by the field
   * @return this object so that calls can be chained
   */
  @CanIgnoreReturnValue
  ClaimTransformerFieldTester<TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
      verifyEnumFieldTransformationRejectsUnrecognizedValue(
          BiConsumer<TTestEntityBuilder, String> setter, String fieldLabel, String badStringValue) {
    verifyFieldTransformationFails(
        claimBuilder -> setter.accept(getTestEntityBuilder(claimBuilder), badStringValue),
        getLabel(fieldLabel),
        "unsupported enum value");
    return this;
  }

  /**
   * Verifies that an enum field transformation that has been configured to reject specific values
   * throws a proper error.
   *
   * @param setter method reference or lambda to set a value of the field being tested on a message
   *     object
   * @param fieldLabel text identifying the field in {@link
   *     gov.cms.bfd.pipeline.rda.grpc.source.DataTransformer.TransformationException error
   *     messages}
   * @param badValues a variadic list of values that should throw an error indicating the value is
   *     unsupported by the field
   * @return this object so that calls can be chained
   */
  @CanIgnoreReturnValue
  <TEnum extends Enum<?>>
      ClaimTransformerFieldTester<
              TClaimBuilder, TClaim, TClaimEntity, TTestEntityBuilder, TTestEntity>
          verifyEnumFieldTransformationRejectsSpecificValues(
              BiConsumer<TTestEntityBuilder, TEnum> setter, String fieldLabel, TEnum... badValues) {
    for (TEnum badValue : badValues) {
      verifyFieldTransformationFails(
          claimBuilder -> setter.accept(getTestEntityBuilder(claimBuilder), badValue),
          getLabel(fieldLabel),
          "unsupported enum value");
    }
    return this;
  }

  /**
   * Overridden by entity specific derived classes to obtain an appropriate builder object that can
   * be used to create TTestEntity object for testing.
   *
   * @param claimBuilder the builder being used to construct a claim
   * @return a builder for the object within the claim that we want to test
   */
  abstract TTestEntityBuilder getTestEntityBuilder(TClaimBuilder claimBuilder);

  /**
   * Overridden by entity specific derived classes to obtain a TTestEntity instance from the claim
   * for testing.
   *
   * @param claim the claim entity
   * @return the entity within the claim that we want to be able to test
   */
  abstract TTestEntity getTestEntity(TClaimEntity claim);

  /**
   * Adds any necessary prefix to the provided error label. This is generally something like
   * "payer-0-" though for claim objects it will simply be the unmodified label.
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
      BiConsumer<TClaimBuilder, String> setter,
      String fieldLabel,
      int minLength,
      int maxLength,
      int length) {
    verifyFieldTransformationFails(
        claimBuilder -> setter.accept(claimBuilder, createString(length)),
        fieldLabel,
        String.format("invalid length: expected=[%d,%d] actual=%d", minLength, maxLength, length));
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
