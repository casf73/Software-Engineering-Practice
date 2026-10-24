package cn.codelyy.store;

import cn.codelyy.store.domain.SellerAccount;
import cn.codelyy.store.repository.IntentRepository;
import cn.codelyy.store.repository.ProductRepository;
import cn.codelyy.store.repository.SellerAccountRepository;
import cn.codelyy.store.service.PasswordHasher;
import cn.codelyy.store.service.SellerCredentialService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:iteration-two;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.upload-directory=target/iteration-two-uploads"
})
@AutoConfigureMockMvc
class IterationTwoIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ProductRepository products;
    @Autowired IntentRepository intents;
    @Autowired SellerAccountRepository accounts;
    @Autowired SellerCredentialService credentials;

    @BeforeEach
    void reset() {
        intents.deleteAll();
        products.deleteAll();
        SellerAccount account = accounts.findById(1L).orElseThrow();
        account.setPasswordHash(PasswordHasher.hash("123456"));
        account.setSessionVersion(account.getSessionVersion() + 1);
        accounts.saveAndFlush(account);
    }

    @Test
    void contactUpdateKeepsCodeAndQueueTimes() throws Exception {
        MockHttpSession seller = login("123456");
        createProduct(seller);
        String first = submit("甲", "13800000001");
        String second = submit("乙", "13800000002");
        var before = intents.findByPasscode(second).orElseThrow();
        var id = before.getId();
        var submittedAt = before.getSubmittedAt();
        var queueAt = before.getQueueAt();
        JsonNode changed = body(mvc.perform(put("/api/public/intents/{code}", second.toLowerCase())
                .contentType(MediaType.APPLICATION_JSON).content(contact("乙更新", "13900000002")))
                .andExpect(status().isOk()).andReturn());
        assertEquals("乙更新", changed.path("name").asText());
        assertEquals("13900000002", changed.path("phone").asText());
        assertEquals(2, changed.path("position").asInt());
        var after = intents.findByPasscode(second).orElseThrow();
        assertEquals(id, after.getId());
        assertEquals(submittedAt, after.getSubmittedAt());
        assertEquals(queueAt, after.getQueueAt());
        assertEquals(1, query(first).path("position").asInt());
        JsonNode workbench = body(mvc.perform(get("/api/seller/workbench").session(seller)).andReturn());
        assertEquals("13900000002", workbench.path("intents").get(1).path("phone").asText());
    }

    @Test
    void waitingBuyerCanEditDuringBothKindsOfFreezeButActiveBuyerCannot() throws Exception {
        MockHttpSession seller = login("123456");
        createProduct(seller);
        String first = submit("甲", "13800000001");
        String second = submit("乙", "13800000002");
        mvc.perform(post("/api/seller/products/current/freeze").session(seller)).andExpect(status().isOk());
        mvc.perform(put("/api/public/intents/{code}", second).contentType(MediaType.APPLICATION_JSON)
                .content(contact("乙冻结更新", "13900000002"))).andExpect(status().isOk());
        mvc.perform(post("/api/seller/products/current/unfreeze").session(seller)).andExpect(status().isOk());
        mvc.perform(post("/api/seller/trades/start").session(seller)).andExpect(status().isOk());
        mvc.perform(put("/api/public/intents/{code}", first).contentType(MediaType.APPLICATION_JSON)
                .content(contact("不能更新", "13900000001"))).andExpect(status().isConflict());
        mvc.perform(put("/api/public/intents/{code}", second).contentType(MediaType.APPLICATION_JSON)
                .content(contact("乙交易期间更新", "13700000002"))).andExpect(status().isOk());
        assertEquals("甲", query(first).path("name").asText());
        assertEquals(1, query(second).path("position").asInt());
    }

    @Test
    void invalidOrCancelledCodesAndInvalidContactAreRejected() throws Exception {
        MockHttpSession seller = login("123456"); createProduct(seller);
        String code = submit("甲", "13800000001");
        for (String payload : new String[]{contact(" ", "13800000001"), contact("甲", "bad")}) {
            mvc.perform(put("/api/public/intents/{code}", code).contentType(MediaType.APPLICATION_JSON)
                    .content(payload)).andExpect(status().isBadRequest());
        }
        mvc.perform(delete("/api/public/intents/{code}", code)).andExpect(status().isNoContent());
        mvc.perform(put("/api/public/intents/{code}", code).contentType(MediaType.APPLICATION_JSON)
                .content(contact("甲更新", "13900000001"))).andExpect(status().isNotFound());
    }

    @Test
    void manualFreezePreservesQueueAndEnforcesAllBoundaries() throws Exception {
        MockHttpSession seller = login("123456"); createProduct(seller);
        String first = submit("甲", "13800000001");
        String second = submit("乙", "13800000002");
        mvc.perform(post("/api/seller/products/current/freeze").session(seller)).andExpect(status().isOk());
        assertEquals("MANUALLY_FROZEN", query(first).path("productStatus").asText());
        assertEquals(2, query(second).path("position").asInt());
        mvc.perform(post("/api/public/intents").contentType(MediaType.APPLICATION_JSON)
                .content(contact("丙", "13800000003"))).andExpect(status().isConflict());
        for (String route : new String[]{"/products/current/freeze", "/products/current/delist", "/trades/start"}) {
            mvc.perform(post("/api/seller" + route).session(seller)).andExpect(status().isConflict());
        }
        mvc.perform(delete("/api/public/intents/{code}", first)).andExpect(status().isNoContent());
        assertEquals(1, query(second).path("position").asInt());
        mvc.perform(post("/api/seller/products/current/unfreeze").session(seller)).andExpect(status().isOk());
        submit("丙", "13800000003");
        assertEquals(1, query(second).path("position").asInt());
        mvc.perform(post("/api/seller/trades/start").session(seller)).andExpect(status().isOk());
    }

    @Test
    void tradeFreezeCannotBeManuallyUnlocked() throws Exception {
        MockHttpSession seller = login("123456"); createProduct(seller); submit("甲", "13800000001");
        mvc.perform(post("/api/seller/trades/start").session(seller)).andExpect(status().isOk());
        mvc.perform(post("/api/seller/products/current/unfreeze").session(seller)).andExpect(status().isConflict());
        mvc.perform(post("/api/seller/products/current/freeze").session(seller)).andExpect(status().isConflict());
        JsonNode product = body(mvc.perform(get("/api/public/products/current")).andReturn());
        assertEquals("IN_TRADE", product.path("status").asText());
    }

    @Test
    void sellerEndpointsRequireLoginAndRejectInvalidState() throws Exception {
        mvc.perform(post("/api/seller/products/current/freeze")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/seller/products/current/unfreeze")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/seller/password").contentType(MediaType.APPLICATION_JSON)
                .content(password("123456", "new-password"))).andExpect(status().isUnauthorized());
        MockHttpSession seller = login("123456");
        mvc.perform(post("/api/seller/products/current/freeze").session(seller)).andExpect(status().isNotFound());
        createProduct(seller);
        mvc.perform(post("/api/seller/products/current/unfreeze").session(seller)).andExpect(status().isConflict());
    }

    @Test
    void passwordChangeChecksCurrentPasswordAndMinimumLength() throws Exception {
        MockHttpSession seller = login("123456");
        mvc.perform(put("/api/seller/password").session(seller).contentType(MediaType.APPLICATION_JSON)
                .content(password("wrong", "new-password"))).andExpect(status().isBadRequest());
        mvc.perform(put("/api/seller/password").session(seller).contentType(MediaType.APPLICATION_JSON)
                .content(password("123456", "short"))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/seller/workbench").session(seller)).andExpect(status().isOk());
        login("123456");
    }

    @Test
    void passwordChangeRevokesAllSessionsAndDoesNotResetAtStartup() throws Exception {
        MockHttpSession first = login("123456");
        MockHttpSession second = login("123456");
        mvc.perform(put("/api/seller/password").session(first).contentType(MediaType.APPLICATION_JSON)
                .content(password("123456", "new-password-2026"))).andExpect(status().isNoContent());
        assertFalse(first.isInvalid(), "改密只移除卖家身份，不能销毁独立客户会话");
        assertNull(first.getAttribute(cn.codelyy.store.config.SellerAuthInterceptor.SESSION_KEY));
        mvc.perform(get("/api/seller/workbench").session(first)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/seller/workbench").session(second)).andExpect(status().isUnauthorized());
        JsonNode session = body(mvc.perform(get("/api/seller/session").session(second)).andReturn());
        assertFalse(session.path("authenticated").asBoolean());
        mvc.perform(post("/api/seller/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"123456\"}")).andExpect(status().isUnauthorized());
        String stored = accounts.findById(1L).orElseThrow().getPasswordHash();
        assertNotEquals("new-password-2026", stored);
        assertTrue(PasswordHasher.matches("new-password-2026", stored));
        credentials.run(new DefaultApplicationArguments(new String[0]));
        assertEquals(stored, accounts.findById(1L).orElseThrow().getPasswordHash());
        MockHttpSession updated = login("new-password-2026");
        mvc.perform(get("/api/seller/workbench").session(updated)).andExpect(status().isOk());
    }

    @Test
    void equalPasswordsUseDifferentRandomSalts() {
        String first = PasswordHasher.hash("same-password");
        String second = PasswordHasher.hash("same-password");
        assertNotEquals(first, second);
        assertTrue(PasswordHasher.matches("same-password", first));
        assertTrue(PasswordHasher.matches("same-password", second));
        assertFalse(PasswordHasher.matches("wrong-password", first));
        assertFalse(PasswordHasher.matches("same-password", "malformed"));
    }

    private MockHttpSession login(String password) throws Exception {
        MvcResult result = mvc.perform(post("/api/seller/login").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("username", "admin", "password", password))))
                .andExpect(status().isOk()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
    private void createProduct(MockHttpSession seller) throws Exception {
        mvc.perform(post("/api/seller/products").session(seller).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试商品\",\"price\":88}")).andExpect(status().isCreated());
    }
    private String submit(String name, String phone) throws Exception {
        return body(mvc.perform(post("/api/public/intents").contentType(MediaType.APPLICATION_JSON)
                .content(contact(name, phone))).andExpect(status().isCreated()).andReturn()).path("passcode").asText();
    }
    private JsonNode query(String code) throws Exception {
        return body(mvc.perform(get("/api/public/intents/{code}", code)).andExpect(status().isOk()).andReturn());
    }
    private String contact(String name, String phone) throws Exception {
        return mapper.writeValueAsString(Map.of("name", name, "phone", phone));
    }
    private String password(String old, String next) throws Exception {
        return mapper.writeValueAsString(Map.of("currentPassword", old, "newPassword", next));
    }
    private JsonNode body(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
