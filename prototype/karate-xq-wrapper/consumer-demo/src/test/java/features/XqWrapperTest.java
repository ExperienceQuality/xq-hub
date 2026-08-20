package features;

import io.karatelabs.junit6.Karate;
import org.junit.jupiter.api.DynamicNode;

class XqWrapperTest {

    @Karate.Test
    Iterable<DynamicNode> xqWrapper() {
        return Karate.run("classpath:features/xq-wrapper.feature");
    }
}
