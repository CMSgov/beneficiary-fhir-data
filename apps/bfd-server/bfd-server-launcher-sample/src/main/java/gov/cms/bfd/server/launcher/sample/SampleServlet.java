package gov.cms.bfd.server.launcher.sample;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A simple sample servlet. */
public final class SampleServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger LOGGER = LoggerFactory.getLogger(SampleServlet.class);

  /** @see javax.servlet.GenericServlet#init() */
  @Override
  public void init() throws ServletException {
    // We'll use this log entry to verify that the servlet is running as expected.
    LOGGER.info("Johnny 5 is alive on SLF4J!");

    // These are just for debugging.
    System.out.println("Johnny 5 is alive on STDOUT.");
    System.err.println("Johnny 5 is alive on STDERR.");
  }

  /**
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *     javax.servlet.http.HttpServletResponse)
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {
    PrintWriter out = resp.getWriter();
    out.println("Johnny 5 is alive on HTTP!");
  }
}
