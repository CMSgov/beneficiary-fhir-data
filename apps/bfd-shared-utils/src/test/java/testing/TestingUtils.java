package testing;

import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;

public class TestingUtils {

  private TestingUtils() {}

  public static Queue<Character> toCharQueue(String s) {
    return s.chars().mapToObj(c -> (char) c).collect(Collectors.toCollection(LinkedList::new));
  }
}
