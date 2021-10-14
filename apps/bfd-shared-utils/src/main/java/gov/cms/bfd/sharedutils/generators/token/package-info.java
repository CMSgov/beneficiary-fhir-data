/**
 * This package is a token generator/parser used to create or parse tokens of a specific pattern.
 *
 * <p>The API creates tokens based of of a specified pattern and uses a backing numeric value to
 * allow for deterministic sequential values to be generated. This is useful if you want to generate
 * realistic looking tokens that follow a specific pattern, either in a sequence or randomly.
 *
 * <p>Example: [ABF]\-[0-9]
 *
 * <p>This would generate the values: A-0, A-1, A-2, ... A-9, B-0, B-1, ... B-9, F-0, ... F-9
 *
 * <p>The API also allows converting from one token to another, allowing you to have a deterministic
 * conversion from one pattern definition to another. One contrived example of this would be
 * converting decimal to hex.
 *
 * <p>Example: \d{8} to [0-9A-F]{8}
 *
 * <p>00004351 -> 000010FF
 *
 * <p>A subset of Regular Expressions is used to define the token patterns, allowing for rather
 * complex definitions.
 *
 * <p>Example: [AB][02468] would generate only "even" values; A0, A2, A4, A6, A8, B0, B2, B4, B6, B8
 */
package gov.cms.bfd.sharedutils.generators.token;
