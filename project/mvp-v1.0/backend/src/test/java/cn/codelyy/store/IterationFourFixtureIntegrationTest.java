package cn.codelyy.store;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:acceptance-fourth;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.upload-directory=target/fourth-fixture-uploads"
})
class IterationFourFixtureIntegrationTest extends FixtureTestSupport {
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void clearCommerceData() {
        jdbc.update("delete from shop_order_events");
        jdbc.update("delete from shop_order_lines");
        jdbc.update("delete from shop_orders");
        jdbc.update("delete from shopping_entries");
    }

    static Stream<Path> combined() throws Exception {
        return findFixtures("综合接口测试");
    }

    static Stream<Path> single() throws Exception {
        return findFixtures("单接口测试");
    }

    private static Stream<Path> findFixtures(String group) throws Exception {
        for (Path path = Path.of("").toAbsolutePath(); path != null; path = path.getParent()) {
            Path directory = path.resolve("tests").resolve("第四轮验收测试数据").resolve(group);
            if (Files.isDirectory(directory)) {
                try (Stream<Path> files = Files.walk(directory)) {
                    return files.filter(file -> file.getFileName().toString().equals("instance.json"))
                        .sorted().toList().stream();
                }
            }
        }
        throw new IllegalStateException("找不到第四轮测试目录：" + group);
    }

    @ParameterizedTest(name = "综合接口 {0}")
    @MethodSource("combined")
    void combinedFixture(Path fixture) throws Exception {
        runFixture(fixture);
    }

    @ParameterizedTest(name = "单接口 {0}")
    @MethodSource("single")
    void singleFixture(Path fixture) throws Exception {
        runFixture(fixture);
    }
}
