package cn.codelyy.store;

import cn.codelyy.store.domain.*;
import cn.codelyy.store.repository.*;
import cn.codelyy.store.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:customer-intent-auth;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.upload-directory=target/customer-intent-auth-uploads"
})
@AutoConfigureMockMvc
@Transactional
class CustomerIntentAuthorizationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ProductRepository products;
    @Autowired IntentRepository intents;
    @Autowired CustomerRepository customers;
    Product product;
    Customer customer;

    @BeforeEach
    void setup() {
        product = new Product();
        product.setName("登录校验商品");
        product.setPrice(new BigDecimal("88.50"));
        product.setStock(2);
        product.setStatus(ProductStatus.ON_SALE);
        products.saveAndFlush(product);
        customer = new Customer();
        customer.setUsername("buyer_auth");
        customer.setUsernameKey("buyer_auth");
        customer.setPasswordHash("unused-test-hash");
        customer.setPhone("13800000001");
        customer.setDefaultLocation("学校南门");
        customers.saveAndFlush(customer);
    }

    private String path(String template) { return template.replace("{id}", product.getId().toString()); }
    private MockHttpSession customerSession() {
        var session = new MockHttpSession();
        session.setAttribute(CustomerService.SESSION_KEY, customer.getId());
        return session;
    }
    private String body() throws Exception {
        return mapper.writeValueAsString(Map.of("name", "买家", "phone", "13800000001", "meetingLocation", "图书馆"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/public/intents", "/api/public/products/{id}/intents", "/api/customer/products/{id}/intents"})
    void anonymousSubmissionCannotCreateAnyIntent(String endpoint) throws Exception {
        mvc.perform(post(path(endpoint)).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnauthorized());
        assertEquals(0, intents.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/public/intents", "/api/public/products/{id}/intents", "/api/customer/products/{id}/intents"})
    void sellerSessionAloneCannotSubmit(String endpoint) throws Exception {
        var result = mvc.perform(post("/api/seller/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk()).andReturn();
        var seller = (MockHttpSession) result.getRequest().getSession(false);
        mvc.perform(post(path(endpoint)).session(seller).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnauthorized());
        assertEquals(0, intents.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/public/intents", "/api/public/products/{id}/intents", "/api/customer/products/{id}/intents"})
    void staleCustomerIdCannotSubmit(String endpoint) throws Exception {
        var session = new MockHttpSession();
        session.setAttribute(CustomerService.SESSION_KEY, Long.MAX_VALUE);
        mvc.perform(post(path(endpoint)).session(session).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnauthorized());
        assertEquals(0, intents.count());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/public/intents", "/api/public/products/{id}/intents", "/api/customer/products/{id}/intents"})
    void authenticatedSubmissionAlwaysBelongsToCustomer(String endpoint) throws Exception {
        mvc.perform(post(path(endpoint)).session(customerSession()).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isCreated()).andExpect(jsonPath("passcode").isNotEmpty());
        var saved = intents.findAll().get(0);
        assertEquals(customer.getId(), saved.getCustomer().getId());
        assertEquals(endpoint.startsWith("/api/customer/") ? "图书馆" : "学校南门", saved.getMeetingLocation());
        assertEquals("登录校验商品", saved.getProductNameSnapshot());
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/public/intents", "/api/public/products/{id}/intents", "/api/customer/products/{id}/intents"})
    void logoutRevokesSubmission(String endpoint) throws Exception {
        var session = customerSession();
        mvc.perform(post("/api/customer/logout").session(session)).andExpect(status().isNoContent());
        mvc.perform(post(path(endpoint)).session(session).contentType(MediaType.APPLICATION_JSON).content(body()))
                .andExpect(status().isUnauthorized());
        assertEquals(0, intents.count());
    }

    @Test
    void browsingAndExistingPasscodeRemainAvailableWithoutLogin() throws Exception {
        mvc.perform(get("/api/public/products")).andExpect(status().isOk());
        mvc.perform(get("/api/public/products/" + product.getId())).andExpect(status().isOk());
        var old = new PurchaseIntent();
        old.setProduct(product); old.setName("历史访客"); old.setPhone("13800000001");
        old.setStatus(IntentStatus.WAITING); old.setPasscode("OLDVISITOR123");
        intents.saveAndFlush(old);
        mvc.perform(get("/api/public/intents/OLDVISITOR123")).andExpect(status().isOk());
        mvc.perform(put("/api/public/intents/OLDVISITOR123").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"更新访客\",\"phone\":\"13900000001\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("name").value("更新访客"));
        mvc.perform(delete("/api/public/intents/OLDVISITOR123")).andExpect(status().isNoContent());
    }
}
