package cn.codelyy.store;

import cn.codelyy.store.domain.IntentStatus;
import cn.codelyy.store.repository.IntentRepository;
import cn.codelyy.store.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

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

    @Test
    @DisplayName("T01 卖家登录、鉴权和退出")
    void sellerLoginAndAuthorization() throws Exception {
        mvc.perform(get("/api/seller/workbench")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/seller/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());

        MockHttpSession session = login();
        JsonNode state = body(mvc.perform(get("/api/seller/session").session(session))
                .andExpect(status().isOk()).andReturn());
        assertTrue(state.path("authenticated").asBoolean());
        mvc.perform(get("/api/seller/workbench").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/seller/logout").session(session)).andExpect(status().isNoContent());
        mvc.perform(get("/api/seller/workbench").session(session)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("T02 商品必填校验、发布、编辑和单件限制")
    void productCreationAndValidation() throws Exception {
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

        JsonNode created = createProduct(session);
        assertEquals("ON_SALE", created.path("status").asText());
        assertEquals("测试商品", created.path("name").asText());
        assertEquals(0, created.path("images").size());
        mvc.perform(post("/api/seller/products").session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(productJson("第二件商品", 88)))
                .andExpect(status().isConflict());

        JsonNode changed = body(mvc.perform(put("/api/seller/products/current").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(productJson("修改后的商品", 99)))
                .andExpect(status().isOk()).andReturn());
        assertEquals("修改后的商品", changed.path("name").asText());
        assertEquals("修改后的商品", body(mvc.perform(get("/api/public/products/current"))
                .andExpect(status().isOk()).andReturn()).path("name").asText());
    }

    @Test
    @DisplayName("T03 买家提交、唯一口令码、FIFO、撤销、重排和作废")
    void queueAndIntentActions() throws Exception {
        MockHttpSession session = login();
        createProduct(session);
        mvc.perform(post("/api/public/intents").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"phone\":\"13800000001\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/public/intents").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"甲\",\"phone\":\"\"}"))
                .andExpect(status().isBadRequest());

        String a = submit("甲", "13800000001");
        String b = submit("乙", "13800000002");
        String c = submit("丙", "13800000003");
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

    @Test
    @DisplayName("T04 开始交易只选队首，冻结期间等待者可撤销，交易者不可撤销")
    void tradeFreezeAndCancellation() throws Exception {
        MockHttpSession session = login();
        createProduct(session);
        String a = submit("甲", "13800000001");
        String b = submit("乙", "13800000002");
        String c = submit("丙", "13800000003");

        JsonNode active = startTrade(session);
        assertEquals("甲", active.path("name").asText());
        assertEquals("IN_TRADE", query(a).path("status").asText());
        assertEquals("IN_TRADE", body(mvc.perform(get("/api/public/products/current"))
                .andExpect(status().isOk()).andReturn()).path("status").asText());
        mvc.perform(post("/api/public/intents").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"丁\",\"phone\":\"13800000004\"}"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/public/intents/{passcode}", a)).andExpect(status().isConflict());
        mvc.perform(post("/api/seller/products/current/delist").session(session))
                .andExpect(status().isConflict());

        mvc.perform(delete("/api/public/intents/{passcode}", b)).andExpect(status().isNoContent());
        invalidCode(b);
        assertEquals(1, query(c).path("position").asInt());
    }

    @Test
    @DisplayName("T05 交易失败并作废：商品恢复在售，不自动开始下一笔")
    void failedTradeVoidsCurrentIntent() throws Exception {
        MockHttpSession session = login();
        createProduct(session);
        String a = submit("甲", "13800000001");
        String b = submit("乙", "13800000002");
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

    @Test
    @DisplayName("T06 交易失败并重排：原口令码继续有效，买家回到队尾")
    void failedTradeRequeuesWithSameCode() throws Exception {
        MockHttpSession session = login();
        createProduct(session);
        String a = submit("甲", "13800000001");
        String b = submit("乙", "13800000002");
        long activeId = startTrade(session).path("id").asLong();
        finishTrade(session, activeId, false, "REQUEUE");

        assertEquals("WAITING", query(a).path("status").asText());
        assertEquals(2, query(a).path("position").asInt());
        assertEquals(1, query(b).path("position").asInt());
        assertEquals("ON_SALE", query(a).path("productStatus").asText());
        assertTrue(workbench(session).path("activeIntent").isNull());
    }

    @Test
    @DisplayName("T07 交易成功：商品下架、剩余意向失败、全部口令码失效")
    void successfulTradeDelistsProduct() throws Exception {
        MockHttpSession session = login();
        createProduct(session);
        String a = submit("甲", "13800000001");
        String b = submit("乙", "13800000002");
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

    @Test
    @DisplayName("T08 卖家主动下架：停止接受意向、口令码失效、允许发布下一件")
    void sellerDelistsProduct() throws Exception {
        MockHttpSession session = login();
        createProduct(session);
        String code = submit("甲", "13800000001");
        mvc.perform(post("/api/seller/products/current/delist").session(session))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/public/products/current")).andExpect(status().isNotFound());
        invalidCode(code);
        assertEquals("ON_SALE", createProduct(session).path("status").asText());
    }

    @Test
    @DisplayName("T09 图片上传：格式、大小和首轮单图上限")
    void imageUploadValidation() throws Exception {
        MockHttpSession session = login();
        createProduct(session);
        MockMultipartFile png = new MockMultipartFile("file", "sample.png", "image/png", new byte[]{1, 2, 3});
        JsonNode uploaded = body(mvc.perform(multipart("/api/seller/products/image").file(png).session(session))
                .andExpect(status().isOk()).andReturn());
        String imageUrl = uploaded.path("url").asText();
        assertTrue(imageUrl.startsWith("/uploads/"));
        MockMultipartFile jpg = new MockMultipartFile("file", "sample.jpg", "image/jpeg", new byte[]{4, 5, 6});
        mvc.perform(multipart("/api/seller/products/image").file(jpg).session(session))
                .andExpect(status().isOk());

        String oneImage = "{\"name\":\"测试商品\",\"price\":88,\"images\":[\"" + imageUrl + "\"]}";
        JsonNode updated = body(mvc.perform(put("/api/seller/products/current").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(oneImage))
                .andExpect(status().isOk()).andReturn());
        assertEquals(1, updated.path("images").size());
        String twoImages = "{\"name\":\"测试商品\",\"price\":88,\"images\":[\"" + imageUrl + "\",\"/uploads/second.png\"]}";
        mvc.perform(put("/api/seller/products/current").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(twoImages))
                .andExpect(status().isBadRequest());

        MockMultipartFile other = new MockMultipartFile("file", "sample.txt", "text/plain", new byte[]{1});
        mvc.perform(multipart("/api/seller/products/image").file(other).session(session))
                .andExpect(status().isBadRequest());
        MockMultipartFile oversized = new MockMultipartFile("file", "large.png", "image/png", new byte[5 * 1024 * 1024 + 1]);
        mvc.perform(multipart("/api/seller/products/image").file(oversized).session(session))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("T10 旧版多图数据在首轮接口中只展示第一张")
    void oldMultiImageProductShowsOnlyFirstImage() throws Exception {
        MockHttpSession session = login();
        createProduct(session);
        var stored = products.findAll().get(0);
        stored.setImageUrls("/uploads/old-first.jpg|/uploads/old-second.jpg");
        products.saveAndFlush(stored);

        JsonNode current = body(mvc.perform(get("/api/public/products/current"))
                .andExpect(status().isOk()).andReturn());
        assertEquals(1, current.path("images").size());
        assertEquals("/uploads/old-first.jpg", current.path("images").get(0).asText());
    }

    private MockHttpSession login() throws Exception {
        MvcResult result = mvc.perform(post("/api/seller/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }

    private JsonNode createProduct(MockHttpSession session) throws Exception {
        return body(mvc.perform(post("/api/seller/products").session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(productJson("测试商品", 88)))
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
