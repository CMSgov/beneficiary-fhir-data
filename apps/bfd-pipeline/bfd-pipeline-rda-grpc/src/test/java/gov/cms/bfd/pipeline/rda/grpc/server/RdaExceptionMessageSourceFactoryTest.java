package gov.cms.bfd.pipeline.rda.grpc.server;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link RdaExceptionMessageSourceFactory}. */
public class RdaExceptionMessageSourceFactoryTest {
  /**
   * Verifies that message sources are wrapped in {@link ExceptionMessageSource}.
   *
   * @throws Exception required by method signatures of called methods
   */
  @Test
  void shouldWrapMessageSources() throws Exception {
    final var realFactory = mock(RdaMessageSourceFactory.class);

    final var realFissMessageSource = mock(RandomFissClaimSource.class);
    final var realMcsMessageSource = mock(RandomMcsClaimSource.class);
    doReturn(realFissMessageSource).when(realFactory).createFissMessageSource(anyLong());
    doReturn(realMcsMessageSource).when(realFactory).createMcsMessageSource(anyLong());

    final var testFactory = new RdaExceptionMessageSourceFactory(realFactory, 10);
    final var testFissMessageSource = testFactory.createFissMessageSource(12);
    Assertions.assertTrue(testFissMessageSource instanceof ExceptionMessageSource);
    verify(realFactory).createFissMessageSource(12);

    final var testMcsMessageSource = testFactory.createMcsMessageSource(48);
    Assertions.assertTrue(testMcsMessageSource instanceof ExceptionMessageSource);
    verify(realFactory).createMcsMessageSource(48);
  }

  /**
   * Verifes {@link RdaMessageSourceFactory#close} is called.
   *
   * @throws Exception required by method signatures of called methods
   */
  @Test
  void shouldCallCloseOnRealFactory() throws Exception {
    final var realFactory = mock(RdaMessageSourceFactory.class);
    final var testFactory = new RdaExceptionMessageSourceFactory(realFactory, 10);
    testFactory.close();
    verify(realFactory).close();
  }
}
