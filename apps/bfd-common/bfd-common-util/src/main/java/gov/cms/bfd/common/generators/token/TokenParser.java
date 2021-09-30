package gov.cms.bfd.common.generators.token;

import java.util.Queue;

public interface TokenParser {

  TokenPattern parse(Queue<Character> patternStream);
}
