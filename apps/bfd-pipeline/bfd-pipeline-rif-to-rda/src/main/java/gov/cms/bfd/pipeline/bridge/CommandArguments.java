package gov.cms.bfd.pipeline.bridge;

import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Helper class to parse and manage the command line arguments. */
public class CommandArguments {

  private final RegisteredArguments registeredArguments;
  private final Map<String, List<String>> arguments;
  private final String commandName;

  private static final String NO_FLAG = "NO_FLAG";

  public CommandArguments(String commandName) {
    registeredArguments = new RegisteredArguments();
    arguments = new HashMap<>();
    arguments.put(NO_FLAG, new ArrayList<>());
    this.commandName = commandName;
  }

  public RegisteredArguments register() {
    return registeredArguments;
  }

  /**
   * Parses the provided flags/arguments.
   *
   * @param args The array of command line arguments to parse.
   */
  public void addAll(String[] args) {
    Deque<String> argQueue = new LinkedList<>(Arrays.asList(args));

    while (!argQueue.isEmpty()) {
      processArgument(argQueue.pop(), argQueue);
    }

    validateArguments();
  }

  /**
   * Checks if any arguments (not flags) have been parsed.
   *
   * @return True if any arguments have been parsed, false otherwise.
   */
  public boolean hasArgs() {
    return !arguments.get(NO_FLAG).isEmpty();
  }

  /**
   * Get an {@link Optional} of the argument at the requested index.
   *
   * <p>Parsed arguments are stored based on order in an indexed list.
   *
   * @param index The index of the desired argument.
   * @return An {@link Optional} containing the desired argument, if it exists, or an empty {@link
   *     Optional} otherwise.
   */
  public Optional<String> getArg(int index) {
    return getFlagValue(NO_FLAG, index);
  }

  /**
   * Get an {@link Optional} of the first flag argument value.
   *
   * @param flagName The name of the flag to get an argument value for.
   * @return An {@link Optional} containing the flag's argument value, or an empty {@link Optional}
   *     if there is no flag or if the flag has no arguments.
   */
  public Optional<String> getFlagValue(String flagName) {
    return getFlagValue(flagName, 0);
  }

  /**
   * Get an {@link Optional} of the first flag argument value.
   *
   * @param flagName The name of the flag to get an argument value for.
   * @param index The index of the argument value to retrieve.
   * @return An {@link Optional} containing the flag's argument value at the specified index, or an
   *     empty {@link Optional} if there is no flag or if the flag has no argument at the specified
   *     index.
   */
  public Optional<String> getFlagValue(String flagName, int index) {
    Optional<String> arg;

    if (arguments.containsKey(flagName) && index >= 0 && index < arguments.get(flagName).size()) {
      arg = Optional.of(arguments.get(flagName).get(index));
    } else {
      arg = Optional.empty();
    }

    return arg;
  }

  /**
   * Provides usage output to display.
   *
   * @return A String containing the usage output.
   */
  public String getUsage() {
    StringBuilder usage = new StringBuilder();
    usage.append(commandName).append(' ');

    StringBuilder options = new StringBuilder();

    for (Map.Entry<String, RegisteredArguments.Argument> entry :
        registeredArguments.arguments.entrySet()) {
      if (!NO_FLAG.equals(entry.getKey())) {
        usage.append(entry.getValue().label).append(" ");
        options
            .append("\n    ")
            .append(entry.getValue().label)
            .append(": ")
            .append(entry.getValue().description);
      }
    }

    usage
        .append(registeredArguments.arguments.get(NO_FLAG).label)
        .append("\n    ")
        .append(registeredArguments.arguments.get(NO_FLAG).label)
        .append(": ")
        .append(registeredArguments.arguments.get(NO_FLAG).description);

    usage.append("\n  Options:").append(options);
    return usage.toString();
  }

  @VisibleForTesting
  void processArgument(String argument, Deque<String> argQueue) {
    if (argument.startsWith("-")) {
      processFlagArgument(argument.substring(1), argQueue);
    } else {
      arguments.get(NO_FLAG).add(argument);
    }
  }

  @VisibleForTesting
  void processFlagArgument(String flagName, Deque<String> argQueue) {
    arguments.putIfAbsent(flagName, new ArrayList<>());

    if (registeredArguments.hasFlag(flagName)
        && registeredArguments.get(flagName).argumentCount > 0) {
      if (!argQueue.isEmpty()) {
        arguments.get(flagName).add(argQueue.pop());
      } else {
        throw new IllegalArgumentException(
            "Expected argument for flag '" + flagName + "', but found none");
      }
    }
  }

  @VisibleForTesting
  void validateArguments() {
    for (Map.Entry<String, List<String>> argument : arguments.entrySet()) {
      String key = argument.getKey();

      if (registeredArguments.hasFlag(key)
          && argument.getValue().size() < registeredArguments.get(key).argumentCount) {
        throw new IllegalArgumentException(
            "Expected flag '"
                + key
                + "' to have "
                + registeredArguments.get(key).argumentCount
                + " arguments, but only "
                + argument.getValue().size()
                + " found");
      }
    }
  }

  /** Helper class for managing registered CLI arguments */
  @VisibleForTesting
  public static class RegisteredArguments {

    private final Map<String, Argument> arguments = new HashMap<>();

    private RegisteredArguments() {}

    public Argument argument() {
      Argument newArgument = new Argument();
      arguments.put(NO_FLAG, newArgument);
      return newArgument;
    }

    public Argument flag(String flagName) {
      Argument newFlag = new Argument();
      arguments.put(flagName, newFlag);
      return newFlag;
    }

    public boolean hasFlag(String flagName) {
      return arguments.containsKey(flagName);
    }

    public Argument get(String flagName) {
      return arguments.get(flagName);
    }

    /** Helper clas for managing a single argument in a CLI invocation. */
    public static class Argument {

      private String label;
      private String description;
      private int argumentCount;

      private Argument() {}

      public Argument label(String label) {
        this.label = label;
        return this;
      }

      public Argument description(String description) {
        this.description = description;
        return this;
      }

      public Argument argumentCount(int argumentCount) {
        if (argumentCount < 0) {
          throw new IllegalArgumentException("Flags must have 0 or more arguments");
        }

        this.argumentCount = argumentCount;
        return this;
      }
    }
  }
}
