package gov.cms.bfd.pipeline.app;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import java.util.LinkedHashMap;
import java.util.Map;

/** Customizes logback json output. */
public class CustomJsonLayout extends JsonLayout {
  @Override
  protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
    map.put("bfd_env", System.getenv("BFD_ENV_NAME"));
    // This field is added by default by logback, but we don't really need it for our purposes.
    map.remove("context");
    // logback uses timestamp, but the standard label is @timestamp, which also allows cloudwatch to
    // use this value as the event timestamp.
    // This should always be a LinkedHashMap, but check anyway out of an abundance of caution.
    if (map instanceof LinkedHashMap<String, Object> linkedHashMap) {
      // Put the timestamp first, for readability.
      linkedHashMap.putFirst("@timestamp", map.get("timestamp"));
    } else {
      map.put("@timestamp", map.get("timestamp"));
    }
    map.remove("timestamp");
  }
}
