package gov.cms.bfd.pipeline.bridge.etl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DelimitedStringExtractor implements ETLJob.Extractor<String[]> {

  private final String line;
  private final String delimiterRegEx;

  @Override
  public String[] extract() {
    return line.split(delimiterRegEx, 0);
  }
}
