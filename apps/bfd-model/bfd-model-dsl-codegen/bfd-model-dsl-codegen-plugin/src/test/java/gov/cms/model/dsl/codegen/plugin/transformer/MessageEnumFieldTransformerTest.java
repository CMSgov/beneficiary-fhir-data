package gov.cms.model.dsl.codegen.plugin.transformer;

import static gov.cms.model.dsl.codegen.plugin.transformer.MessageEnumFieldTransformer.ENUM_CLASS_OPT;
import static gov.cms.model.dsl.codegen.plugin.transformer.MessageEnumFieldTransformer.EXTRACTOR_OPTIONS_OPT;
import static gov.cms.model.dsl.codegen.plugin.transformer.MessageEnumFieldTransformer.UNSUPPORTED_ENUM_VALUES_OPT;
import static org.junit.Assert.assertEquals;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import java.util.List;
import org.junit.Test;

/** Unit test for {@link MessageEnumFieldTransformer}. */
public class MessageEnumFieldTransformerTest {
  /** Verify String fields are copied using {@code copyEnumAsString}. */
  @Test
  public void testStringField() {
    ColumnBean column = ColumnBean.builder().name("claimStatus").sqlType("varchar(20)").build();
    TransformationBean transformation =
        TransformationBean.builder()
            .optional(false)
            .from("claimStatus")
            .transformer("MessageEnum")
            .build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    var generator = new MessageEnumFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyEnumAsString(namePrefix + gov.cms.test.Entity.Fields.claimStatus, false, 0, 20, Entity_claimStatus_Extractor.getEnumString(from), to::setClaimStatus);\n",
        block.toString());
  }

  /** Verify char fields are copied using {@code copyEnumAsCharacter}. */
  @Test
  public void testCharacterField() {
    ColumnBean column =
        ColumnBean.builder().name("claimStatus").sqlType("char(1)").javaType("char").build();
    TransformationBean transformation =
        TransformationBean.builder()
            .optional(false)
            .from("claimStatus")
            .transformer("MessageEnum")
            .optional(true)
            .build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    var generator = new MessageEnumFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals(
        "transformer.copyEnumAsCharacter(namePrefix + gov.cms.test.Entity.Fields.claimStatus, Entity_claimStatus_Extractor.getEnumString(from), to::setClaimStatus);\n",
        block.toString());
  }

  /** Verify that {@link FieldSpec} is defined correctly. */
  @Test
  public void testGenerateFieldSpecs() {
    ColumnBean column = ColumnBean.builder().name("claimStatus").sqlType("varchar(20)").build();
    TransformationBean transformation =
        TransformationBean.builder()
            .optional(false)
            .from("claimStatus")
            .transformer("MessageEnum")
            .transformerOption(ENUM_CLASS_OPT, "gov.cms.test.Value")
            .build();
    MappingBean mapping =
        MappingBean.builder()
            .messageClassName("gov.cms.test.Message")
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    var generator = new MessageEnumFieldTransformer();
    List<FieldSpec> block = generator.generateFieldSpecs(mapping, column, transformation);
    assertEquals(
        "[private final gov.cms.model.dsl.codegen.library.EnumStringExtractor<gov.cms.test.Message, gov.cms.test.Value> Entity_claimStatus_Extractor;\n]",
        block.toString());
  }

  /** Verify that field initializer is defined correctly. */
  @Test
  public void testGenerateFieldInitializers() {
    ColumnBean column = ColumnBean.builder().name("claimStatus").sqlType("varchar(20)").build();
    TransformationBean transformation =
        TransformationBean.builder()
            .optional(false)
            .from("claimStatus")
            .transformer("MessageEnum")
            .transformerOption(ENUM_CLASS_OPT, "gov.cms.test.Value")
            .transformerOption(UNSUPPORTED_ENUM_VALUES_OPT, "Bad,Worse")
            .transformerOption(EXTRACTOR_OPTIONS_OPT, "Alpha,Beta")
            .build();
    MappingBean mapping =
        MappingBean.builder()
            .messageClassName("gov.cms.test.Message")
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    var generator = new MessageEnumFieldTransformer();
    List<CodeBlock> block = generator.generateFieldInitializers(mapping, column, transformation);
    assertEquals(
        "[Entity_claimStatus_Extractor = enumExtractorFactory.createEnumStringExtractor(gov.cms.test.Message::hasClaimStatusEnum,gov.cms.test.Message::getClaimStatusEnum,gov.cms.test.Message::hasClaimStatusUnrecognized,gov.cms.test.Message::getClaimStatusUnrecognized,gov.cms.test.Value.UNRECOGNIZED,java.util.Set.of(gov.cms.test.Value.Bad,gov.cms.test.Value.Worse),java.util.Set.of(gov.cms.model.dsl.codegen.library.EnumStringExtractor.Options.Alpha,gov.cms.model.dsl.codegen.library.EnumStringExtractor.Options.Beta));\n]",
        block.toString());
  }
}
