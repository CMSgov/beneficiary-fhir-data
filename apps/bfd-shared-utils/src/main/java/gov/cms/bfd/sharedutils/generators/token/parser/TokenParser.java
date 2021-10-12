package gov.cms.bfd.sharedutils.generators.token.parser;

import gov.cms.bfd.sharedutils.generators.token.pattern.TokenPattern;
import java.util.Queue;

public interface TokenParser {

  TokenPattern parse(Queue<Character> patternStream);
}
