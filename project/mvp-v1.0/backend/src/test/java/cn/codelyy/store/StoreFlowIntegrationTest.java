package cn.codelyy.store;

import cn.codelyy.store.domain.IntentStatus;
import cn.codelyy.store.repository.IntentRepository;
import cn.codelyy.store.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:store-test;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.upload-directory=target/test-uploads"
})
@AutoConfigureMockMvc
class StoreFlowIntegrationTest {
    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private IntentRepository intents;
    @Autowired private ProductRepository products;

    @BeforeEach
    void clearTestData() {
        intents.deleteAll();
        products.deleteAll();
    }

    @ParameterizedTest(name = "T01 {index} {0}")
    @MethodSource("casesT01")
    @DisplayName("T01 卖家登录、鉴权和退出")
    void sellerLoginAndAuthorization(Path instanceFile) throws Exception {
        String wrongPassword = fixture(instanceFile).path("input").path("wrongPassword").asText();
        mvc.perform(get("/api/seller/workbench")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/seller/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("username", "admin", "password", wrongPassword))))
                .andExpect(status().isUnauthorized());

        MockHttpSession session = login();
        JsonNode state = body(mvc.perform(get("/api/seller/session").session(session))
                .andExpect(status().isOk()).andReturn());
        assertTrue(state.path("authenticated").asBoolean());
        mvc.perform(get("/api/seller/workbench").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/seller/logout").session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/seller/workbench").session(session)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "T02 {index} {0}")
    @MethodSource("casesT02")
    @DisplayName("T02 商品必填校验、发布和编辑")
    void productCreationAndValidation(Path instanceFile) throws Exception {
        JsonNode input = fixture(instanceFile).path("input");
        String productName = input.path("productName").asText();
        String updatedName = input.path("updatedName").asText();
        int price = input.path("price").asInt();
        MockHttpSession session = login();
        mvc.perform(get("/api/public/products/current")).andExpect(status().isNotFound());
        mvc.perform(post("/api/seller/products").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":88}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/seller/products").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试商品\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/seller/products").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"测试商品\",\"price\":0}"))
                .andExpect(status().isBadRequest());

        JsonNode created = createProduct(session, productName, price);
        assertEquals("ON_SALE", created.path("status").asText());
        assertEquals(productName, created.path("name").asText());
        assertEquals(0, created.path("images").size());
        JsonNode changed = body(mvc.perform(put("/api/seller/products/current").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(productJson(updatedName, price + 11)))
                .andExpect(status().isOk()).andReturn());
        assertEquals(updatedName, changed.path("name").asText());
        assertEquals(updatedName, body(mvc.perform(get("/api/public/products/current"))
                .andExpect(status().isOk()).andReturn()).path("name").asText());

    }

    @ParameterizedTest(name = "T03 {index} {0}")
    @MethodSource("casesT03")
    @DisplayName("T03 买家提交、唯一口令码、FIFO、撤销、重排和作废")
    void queueAndIntentActions(Path instanceFile) throws Exception {
        String phonePrefix = fixture(instanceFile).path("input").path("phonePrefix").asText();
        MockHttpSession session = login();
        createProduct(session);
        mvc.perform(post("/api/public/intents").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"phone\":\"13800000001\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/public/intents").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"甲\",\"phone\":\"\"}"))
                .andExpect(status().isBadRequest());

        String a = submit("甲", phone(phonePrefix, 1));
        String b = submit("乙", phone(phonePrefix, 2));
        String c = submit("丙", phone(phonePrefix, 3));
        assertNotEquals(a, b);
        assertNotEquals(b, c);
        assertEquals(1, query(a).path("position").asInt());
        assertEquals(2, query(b).path("position").asInt());
        assertEquals(3, query(c).path("position").asInt());

        mvc.perform(delete("/api/public/intents/{passcode}", b)).andExpect(status().isNoContent());
        invalidCode(b);
        assertEquals(2, query(c).path("position").asInt());

        long aId = intentId(session, "甲");
        process(session, aId, "REQUEUE");
        assertEquals(2, query(a).path("position").asInt());
        assertEquals(1, query(c).path("position").asInt());
        process(session, intentId(session, "丙"), "VOID");
        invalidCode(c);
        assertEquals(1, query(a).path("position").asInt());
    }

    @ParameterizedTest(name = "T04 {index} {0}")
    @MethodSource("casesT04")
    @DisplayName("T04 开始交易只选队首，冻结期间等待者可撤销，交易者不可撤销")
    void tradeFreezeAndCancellation(Path instanceFile) throws Exception {
        String phonePrefix = fixture(instanceFile).path("input").path("phonePrefix").asText();
        MockHttpSession session = login();
        createProduct(session);
        String a = submit("甲", phone(phonePrefix, 1));
        String b = submit("乙", phone(phonePrefix, 2));
        String c = submit("丙", phone(phonePrefix, 3));

        JsonNode active = startTrade(session);
        assertEquals("甲", active.path("name").asText());
        assertEquals("IN_TRADE", query(a).path("status").asText());
        assertEquals("IN_TRADE", body(mvc.perform(get("/api/public/products/current"))
                .andExpect(status().isOk()).andReturn()).path("status").asText());
        mvc.perform(post("/api/public/intents").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(new BuyerRequest("丁", phone(phonePrefix, 4)))))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/public/intents/{passcode}", a)).andExpect(status().isConflict());
        mvc.perform(post("/api/seller/products/current/delist").session(session))
                .andExpect(status().isConflict());

        mvc.perform(delete("/api/public/intents/{passcode}", b)).andExpect(status().isNoContent());
        invalidCode(b);
        assertEquals(1, query(c).path("position").asInt());
    }

    @ParameterizedTest(name = "T05 {index} {0}")
    @MethodSource("casesT05")
    @DisplayName("T05 交易失败并作废：商品恢复在售，不自动开始下一笔")
    void failedTradeVoidsCurrentIntent(Path instanceFile) throws Exception {
        String phonePrefix = fixture(instanceFile).path("input").path("phonePrefix").asText();
        MockHttpSession session = login();
        createProduct(session);
        String a = submit("甲", phone(phonePrefix, 1));
        String b = submit("乙", phone(phonePrefix, 2));
        long activeId = startTrade(session).path("id").asLong();
        finishTrade(session, activeId, false, "VOID");

        invalidCode(a);
        assertEquals(1, query(b).path("position").asInt());
        assertEquals("ON_SALE", body(mvc.perform(get("/api/public/products/current"))
                .andExpect(status().isOk()).andReturn()).path("status").asText());
        JsonNode workbench = workbench(session);
        assertTrue(workbench.path("activeIntent").isNull());
        assertEquals(1, workbench.path("waitingCount").asInt());
        assertEquals(IntentStatus.TRADE_FAILED, intents.findAll().stream()
                .filter(intent -> "甲".equals(intent.getName())).findFirst().orElseThrow().getStatus());
    }

    @ParameterizedTest(name = "T06 {index} {0}")
    @MethodSource("casesT06")
    @DisplayName("T06 交易失败并重排：原口令码继续有效，买家回到队尾")
    void failedTradeRequeuesWithSameCode(Path instanceFile) throws Exception {
        String phonePrefix = fixture(instanceFile).path("input").path("phonePrefix").asText();
        MockHttpSession session = login();
        createProduct(session);
        String a = submit("甲", phone(phonePrefix, 1));
        String b = submit("乙", phone(phonePrefix, 2));
        long activeId = startTrade(session).path("id").asLong();
        finishTrade(session, activeId, false, "REQUEUE");

        assertEquals("WAITING", query(a).path("status").asText());
        assertEquals(2, query(a).path("position").asInt());
        assertEquals(1, query(b).path("position").asInt());
        assertEquals("ON_SALE", query(a).path("productStatus").asText());
        assertTrue(workbench(session).path("activeIntent").isNull());
    }

    @ParameterizedTest(name = "T07 {index} {0}")
    @MethodSource("casesT07")
    @DisplayName("T07 交易成功：商品下架、剩余意向失败、全部口令码失效")
    void successfulTradeDelistsProduct(Path instanceFile) throws Exception {
        String phonePrefix = fixture(instanceFile).path("input").path("phonePrefix").asText();
        MockHttpSession session = login();
        createProduct(session);
        String a = submit("甲", phone(phonePrefix, 1));
        String b = submit("乙", phone(phonePrefix, 2));
        long activeId = startTrade(session).path("id").asLong();
        finishTrade(session, activeId, true, null);

        mvc.perform(get("/api/public/products/current")).andExpect(status().isNotFound());
        invalidCode(a);
        invalidCode(b);
        assertEquals(IntentStatus.TRADE_SUCCESS, intents.findAll().stream()
                .filter(intent -> "甲".equals(intent.getName())).findFirst().orElseThrow().getStatus());
        assertEquals(IntentStatus.TRADE_FAILED, intents.findAll().stream()
                .filter(intent -> "乙".equals(intent.getName())).findFirst().orElseThrow().getStatus());
        assertEquals("ON_SALE", createProduct(session).path("status").asText());
    }

    @ParameterizedTest(name = "T08 {index} {0}")
    @MethodSource("casesT08")
    @DisplayName("T08 卖家主动下架：停止接受意向、口令码失效、允许发布下一件")
    void sellerDelistsProduct(Path instanceFile) throws Exception {
        String phonePrefix = fixture(instanceFile).path("input").path("phonePrefix").asText();
        MockHttpSession session = login();
        createProduct(session);
        String code = submit("甲", phone(phonePrefix, 1));
        mvc.perform(post("/api/seller/products/current/delist").session(session))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/public/products/current")).andExpect(status().isNotFound());
        invalidCode(code);
        assertEquals("ON_SALE", createProduct(session).path("status").asText());
    }

    @ParameterizedTest(name = "T09 {index} {0}")
    @MethodSource("casesT09")
    @DisplayName("T09 图片上传：格式、大小和多图支持")
    void imageUploadValidation(Path instanceFile) throws Exception {
        JsonNode input = fixture(instanceFile).path("input");
        Path folder = instanceFile.getParent();
        MockHttpSession session = login();
        createProduct(session);
        MockMultipartFile png = new MockMultipartFile("file", input.path("png").asText(), "image/png",
                Files.readAllBytes(folder.resolve(input.path("png").asText())));
        JsonNode uploaded = body(mvc.perform(multipart("/api/seller/products/image").file(png).session(session))
                .andExpect(status().isOk()).andReturn());
        String imageUrl = uploaded.path("url").asText();
        assertTrue(imageUrl.startsWith("/uploads/"));
        MockMultipartFile jpg = new MockMultipartFile("file", input.path("jpg").asText(), "image/jpeg",
                Files.readAllBytes(folder.resolve(input.path("jpg").asText())));
        mvc.perform(multipart("/api/seller/products/image").file(jpg).session(session))
                .andExpect(status().isOk());

        String oneImage = "{\"name\":\"测试商品\",\"price\":88,\"images\":[\"" + imageUrl + "\"]}";
        JsonNode updated = body(mvc.perform(put("/api/seller/products/current").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(oneImage))
                .andExpect(status().isOk()).andReturn());
        assertEquals(1, updated.path("images").size());
        MockMultipartFile other = new MockMultipartFile("file", input.path("invalid").asText(), "text/plain",
                Files.readAllBytes(folder.resolve(input.path("invalid").asText())));
        mvc.perform(multipart("/api/seller/products/image").file(other).session(session))
                .andExpect(status().isBadRequest());
        MockMultipartFile oversized = new MockMultipartFile("file", input.path("oversized").asText(), "image/png",
                Files.readAllBytes(folder.resolve(input.path("oversized").asText())));
        mvc.perform(multipart("/api/seller/products/image").file(oversized).session(session))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "T10 {index} {0}")
    @MethodSource("casesT10")
    @DisplayName("T10 既有图片记录的首图顺序兼容")
    void oldMultiImageProductReturnsAllImages(Path instanceFile) throws Exception {
        JsonNode input = fixture(instanceFile).path("input");
        String firstImage = input.path("firstImage").asText();
        String secondImage = input.path("secondImage").asText();
        MockHttpSession session = login();
        createProduct(session);
        var stored = products.findAll().get(0);
        stored.setImageUrls(firstImage + "|" + secondImage);
        products.saveAndFlush(stored);

        JsonNode current = body(mvc.perform(get("/api/public/products/current"))
                .andExpect(status().isOk()).andReturn());
        assertFalse(current.path("images").isEmpty());
        assertEquals(firstImage, current.path("images").get(0).asText());
    }

    @Test
    @DisplayName("全部 17 个总用例各有 8 个可读取的测试实例")
    void fixtureCatalogIsComplete() throws Exception {
        for (int i = 1; i <= 10; i++) verifyThreeFixtures("T%02d".formatted(i), "automated");
        for (int i = 1; i <= 7; i++) verifyThreeFixtures("U%02d".formatted(i), "manual");
    }

    private void verifyThreeFixtures(String caseId, String mode) throws Exception {
        var paths = fixturePaths(caseId).toList();
        assertEquals(8, paths.size(), caseId + " 必须有 8 个实例");
        for (int i = 0; i < paths.size(); i++) {
            JsonNode data = fixture(paths.get(i));
            assertEquals(caseId + "-%02d".formatted(i + 1), data.path("id").asText());
            assertEquals(caseId, data.path("caseId").asText());
            assertEquals(mode, data.path("mode").asText());
            assertFalse(data.path("expected").asText().isBlank());
        }
    }

    private static Stream<Path> casesT01() { return fixturePaths("T01").limit(3); }
    private static Stream<Path> casesT02() { return fixturePaths("T02").limit(3); }
    private static Stream<Path> casesT03() { return fixturePaths("T03").limit(3); }
    private static Stream<Path> casesT04() { return fixturePaths("T04").limit(3); }
    private static Stream<Path> casesT05() { return fixturePaths("T05").limit(3); }
    private static Stream<Path> casesT06() { return fixturePaths("T06").limit(3); }
    private static Stream<Path> casesT07() { return fixturePaths("T07").limit(3); }
    private static Stream<Path> casesT08() { return fixturePaths("T08").limit(3); }
    private static Stream<Path> casesT09() { return fixturePaths("T09").limit(3); }
    private static Stream<Path> casesT10() { return fixturePaths("T10").limit(3); }

    private static Stream<Path> fixturePaths(String caseId) {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (directory != null) {
            Path candidate = directory.resolve("tests").resolve("验收测试数据").resolve(caseId.startsWith("T") ? "接口测试" : "功能测试").resolve(caseId);
            if (Files.isDirectory(candidate)) {
                try (Stream<Path> paths = Files.list(candidate)) {
                    return paths.filter(Files::isDirectory).sorted().map(path -> path.resolve("instance.json"))
                            .toList().stream();
                } catch (IOException error) {
                    throw new IllegalStateException("无法读取测试实例目录：" + candidate, error);
                }
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("找不到 tests/验收测试数据/" + caseId);
    }

    private JsonNode fixture(Path path) throws IOException {
        return mapper.readTree(Files.readString(path, StandardCharsets.UTF_8));
    }

    private String phone(String prefix, int lastDigit) {
        assertEquals(10, prefix.length(), "测试数据 phonePrefix 应为 10 位");
        return prefix + lastDigit;
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mvc.perform(post("/api/seller/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private JsonNode createProduct(MockHttpSession session) throws Exception {
        return createProduct(session, "测试商品", 88);
    }

    private JsonNode createProduct(MockHttpSession session, String name, int price) throws Exception {
        return body(mvc.perform(post("/api/seller/products").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(productJson(name, price)))
                .andExpect(status().isCreated()).andReturn());
    }

    private String productJson(String name, int price) {
        return "{\"name\":\"" + name + "\",\"price\":" + price + ",\"description\":\"测试描述\",\"images\":[]}";
    }

    private String submit(String name, String phone) throws Exception {
        String payload = mapper.writeValueAsString(new BuyerRequest(name, phone));
        JsonNode issued = body(mvc.perform(post("/api/public/intents").contentType(MediaType.APPLICATION_JSON)
                        .content(payload)).andExpect(status().isCreated()).andReturn());
        String code = issued.path("passcode").asText();
        assertFalse(code.isBlank());
        return code;
    }

    private JsonNode query(String code) throws Exception {
        return body(mvc.perform(get("/api/public/intents/{passcode}", code))
                .andExpect(status().isOk()).andReturn());
    }

    private void invalidCode(String code) throws Exception {
        mvc.perform(get("/api/public/intents/{passcode}", code)).andExpect(status().isNotFound());
    }

    private JsonNode workbench(MockHttpSession session) throws Exception {
        return body(mvc.perform(get("/api/seller/workbench").session(session))
                .andExpect(status().isOk()).andReturn());
    }

    private long intentId(MockHttpSession session, String name) throws Exception {
        for (JsonNode row : workbench(session).path("intents")) {
            if (name.equals(row.path("name").asText())) return row.path("id").asLong();
        }
        throw new AssertionError("没有找到买家：" + name);
    }

    private void process(MockHttpSession session, long id, String action) throws Exception {
        mvc.perform(post("/api/seller/intents/{id}/action", id).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"" + action + "\"}"))
                .andExpect(status().isNoContent());
    }

    private JsonNode startTrade(MockHttpSession session) throws Exception {
        return body(mvc.perform(post("/api/seller/trades/start").session(session))
                .andExpect(status().isOk()).andReturn());
    }

    private void finishTrade(MockHttpSession session, long id, boolean success, String disposition) throws Exception {
        String payload = mapper.writeValueAsString(new TradeResult(success, disposition));
        mvc.perform(post("/api/seller/trades/{id}/result", id).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isNoContent());
    }

    private JsonNode body(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private record BuyerRequest(String name, String phone) {}
    private record TradeResult(boolean success, String disposition) {}
}
