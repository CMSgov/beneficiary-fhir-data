package gov.cms.bfd.pipeline.rda.grpc.sink;

public class MultiCloser {
  private Exception error = null;

  public void close(Closer closer) {
    try {
      closer.close();
    } catch (Exception ex) {
      if (error == null) {
        error = ex;
      } else {
        error.addSuppressed(ex);
      }
    }
  }

  public void finish() throws Exception {
    if (error != null) {
      throw error;
    }
  }

  public interface Closer {
    void close() throws Exception;
  }
}
