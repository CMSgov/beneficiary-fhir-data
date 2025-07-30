package gov.cms.bfd.server.ng.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/** Prevents annoying 404 errors when browsers request teh favicon. */
@Controller
public class FaviconController {

  /** Returns an empty favicon. */
  @GetMapping("favicon.ico")
  @ResponseBody
  public void returnNoFavicon() {}
}
