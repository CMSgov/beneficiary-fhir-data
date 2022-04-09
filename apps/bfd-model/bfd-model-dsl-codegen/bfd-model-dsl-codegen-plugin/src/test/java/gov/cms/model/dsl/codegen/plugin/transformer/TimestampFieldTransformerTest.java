package gov.cms.model.dsl.codegen.plugin.transformer;

import static org.junit.Assert.assertEquals;

import com.squareup.javapoet.CodeBlock;
import gov.cms.model.dsl.codegen.plugin.model.ColumnBean;
import gov.cms.model.dsl.codegen.plugin.model.MappingBean;
import gov.cms.model.dsl.codegen.plugin.model.RootBean;
import gov.cms.model.dsl.codegen.plugin.model.TransformationBean;
import org.junit.Test;

public class TimestampFieldTransformerTest {
  @Test
  public void test() {
    ColumnBean column =
        ColumnBean.builder().name("lastUpdated").sqlType("timestamp with time zone").build();
    TransformationBean transformation = TransformationBean.builder().to("lastUpdated").build();
    MappingBean mapping =
        MappingBean.builder()
            .entityClassName("gov.cms.bfd.model.rda.PreAdjFissClaim")
            .transformation(transformation)
            .build();
    RootBean model = RootBean.builder().mapping(mapping).build();

    TimestampFieldTransformer generator = new TimestampFieldTransformer();
    CodeBlock block =
        generator.generateCodeBlock(
            mapping,
            column,
            transformation,
            GrpcFromCodeGenerator.Instance,
            StandardToCodeGenerator.Instance);
    assertEquals("to.setLastUpdated(now);\n", block.toString());
  }
}
