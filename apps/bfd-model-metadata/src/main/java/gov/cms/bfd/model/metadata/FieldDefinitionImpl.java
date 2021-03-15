package gov.cms.bfd.model.metadata;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.module.paranamer.ParanamerModule;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

/** A boring, default implementation of {@link FieldDefinition}. */
@JsonAutoDetect(
    fieldVisibility = Visibility.ANY,
    getterVisibility = Visibility.NONE,
    setterVisibility = Visibility.NONE)
public final class FieldDefinitionImpl implements FieldDefinition {
  private static final ObjectMapper YAML_MAPPER =
      new ObjectMapper(new YAMLFactory())
          .registerModule(new ParanamerModule())
          .registerModule(new Jdk8Module());
  private static final Parser MARKDOWN_PARSER = Parser.builder().build();
  private static final HtmlRenderer MARKDOWN_RENDERER = HtmlRenderer.builder().build();

  /**
   * @param fieldDefinitionResourceName the classpath resource containing the field definition YAML
   *     file to be parsed
   * @return the {@link FieldDefinition} that was parsed from the specified YAML resource
   */
  public static FieldDefinitionImpl parseFromYaml(String fieldDefinitionResourceName) {
    InputStream fieldDefinitionResourceStream =
        Thread.currentThread()
            .getContextClassLoader()
            .getResourceAsStream(fieldDefinitionResourceName);
    try {
      return YAML_MAPPER.readValue(fieldDefinitionResourceStream, FieldDefinitionImpl.class);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private final String id;
  private final Optional<String> commonName;
  private final Optional<String> description;

  /**
   * Constructs a new {@link FieldDefinitionImpl}.
   *
   * @param id the value to use for {@link #getId()}
   * @param commonName the value to use for {@link #getCommonName()}
   * @param description the value to use for {@link #getDescriptionAsMarkdown()}
   */
  @JsonCreator
  FieldDefinitionImpl(
      @JsonProperty String id,
      @JsonProperty Optional<String> commonName,
      @JsonProperty Optional<String> description) {
    this.id = id;
    this.commonName = commonName;
    this.description = description;
  }

  /** @see gov.cms.bfd.model.metadata.FieldDefinition#getId() */
  @Override
  public String getId() {
    return id;
  }

  /** @see gov.cms.bfd.model.metadata.FieldDefinition#getCommonName() */
  @Override
  public Optional<String> getCommonName() {
    return commonName;
  }

  /** @see gov.cms.bfd.model.metadata.FieldDefinition#getDescriptionAsMarkdown() */
  @Override
  public Optional<String> getDescriptionAsMarkdown() {
    return description;
  }

  /** @see gov.cms.bfd.model.metadata.FieldDefinition#getDescriptionAsHtml() */
  @Override
  public Optional<String> getDescriptionAsHtml() {
    if (!description.isPresent()) return Optional.empty();

    /*
     * Design Note: we're explicitly not doing any HTML sanitization here, as the descriptions are
     * not coming from user input. If that ever changed, though, and descriptions were not
     * completely static, then HTML sanitization would become a critical security component. Without
     * it, we'd be susceptible to HTML/JavaScript injection.
     */
    Node descriptionAsMarkdownNode = MARKDOWN_PARSER.parse(description.get());
    String descriptionAsHtml = MARKDOWN_RENDERER.render(descriptionAsMarkdownNode);

    return Optional.of(descriptionAsHtml);
  }
}
