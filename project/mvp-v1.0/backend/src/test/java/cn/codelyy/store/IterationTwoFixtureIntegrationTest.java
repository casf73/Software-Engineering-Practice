package cn.codelyy.store;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
class IterationTwoFixtureIntegrationTest extends FixtureTestSupport {
 static Stream<Path> fixtures() throws Exception {return fixtures(2);}
 @ParameterizedTest(name="第2轮 {index} {0}") @MethodSource("fixtures")
 void instance(Path file) throws Exception {runFixture(file);}
}
