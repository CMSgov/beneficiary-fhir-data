package gov.cms.bfd.pipeline.app;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import java.util.LinkedHashMap;
import java.util.Map;

/** Adds custom fields to the logback json output. */
public class CustomJsonLayout extends JsonLayout {
  @Override
  protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
    map.put("bfd_env", System.getenv("BFD_ENV_NAME"));
    map.remove("context");

    // This should always be a LinkedHashMap, but check anyway out of an abundance of caution.
    if (map instanceof LinkedHashMap<String, Object> linkedHashMap) {
      linkedHashMap.putFirst("@timestamp", map.get("timestamp"));
    } else {
      map.put("@timestamp", map.get("timestamp"));
    }
    map.remove("timestamp");
  }
}
