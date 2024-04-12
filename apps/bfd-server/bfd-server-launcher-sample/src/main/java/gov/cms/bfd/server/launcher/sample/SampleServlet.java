package gov.cms.bfd.server.launcher.sample;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** A simple sample servlet. */
public final class SampleServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  /** The logger for the class logic. */
  private static final Logger LOGGER_MISC = LoggerFactory.getLogger(SampleServlet.class);

  /** The logger for the http access. */
  private static final Logger LOGGER_ACCESS = LoggerFactory.getLogger("HTTP_ACCESS");

  /** {@inheritDoc} */
  @Override
  public void init() throws ServletException {
    // We'll use this log entry to verify that the servlet is running as expected.
    LOGGER_MISC.info("Johnny 5 is alive on SLF4J!");

    // We'll use this log entry to verify that the JSON access log is working.
    LOGGER_ACCESS.info("request");

    // These are just for debugging.
    System.out.println("Johnny 5 is alive on STDOUT.");
    System.err.println("Johnny 5 is alive on STDERR.");
  }

  /** {@inheritDoc} */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
    try {
      PrintWriter out = resp.getWriter();
      out.print("Johnny 5 is alive on HTTP!");
    } catch (IOException io) {
      LOGGER_MISC.info("An IOException occurred in SampleServlet", io);
    }
  }
}
