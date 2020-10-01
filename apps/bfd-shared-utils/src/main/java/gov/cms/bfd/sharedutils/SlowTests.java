package gov.cms.bfd.sharedutils;

/**
 * Use this marker interface on any test case that takes longer than 30 seconds (on a slower box,
 * not a fast monster). It should be applied like this:
 *
 * <pre>
 * import org.junit.experimental.categories.Category;
 * import gov.cms.bfd.sharedutils.SlowTests;
 *
 * public final class SomeKindaIT {
 *   &#64;Test
 *   public void fastTest() {
 *     // ...
 *   }
 *
 *   &#64;Test
 *   &#64;Category(SlowTests.class)
 *   public void slowTest() {
 *     // ...
 *   }
 * }
 * </pre>
 *
 * <p>This category of tests is disabled by default in our root <code>apps/pom.xml</code>. To run
 * them, run the build like this:
 *
 * <pre>
 * $ mvn clean verify -Dtests.categories.include=gov.cms.bfd.sharedutils.SlowTests -Dtests.categories.exclude=
 * </pre>
 *
 * <p>Note that, unfortunately, Surefire/Failsafe don't seem to provide a way to run these tests
 * <em>with</em> all of the non-categorized tests, at the same time; categorized tests always have
 * to be run in a separate build.
 */
public interface SlowTests {
  // This is just a marker interface; doesn't need anything.
}
