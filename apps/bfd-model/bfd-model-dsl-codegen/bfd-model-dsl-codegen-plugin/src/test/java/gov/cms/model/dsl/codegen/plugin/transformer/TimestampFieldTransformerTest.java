package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.Assert.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.accessor.GrpcGetter;
import gov.cms.model.dsl.codegen.plugin.accessor.StandardSetter;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.Test;

/** Unit test for {@link TimestampFieldTransformer}. */
public class TimestampFieldTransformerTest {
  /** Verifies that setter is used correctly. */
  @Test
  public void test() {
    ColumnBean column =
        ColumnBean.builder().name("lastUpdated").sqlType("timestamp with time zone").build();
    TransformationBean transformation = TransformationBean.builder().to("lastUpdated").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.test.Entity")
            .transformation(transformation)
            .build();

    TimestampFieldTransformer generator = new TimestampFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping, column, transformation, GrpcGetter.Instance, StandardSetter.Instance);
    assertEquals("to.setLastUpdated(now);\n", block.toString());
  }
}
