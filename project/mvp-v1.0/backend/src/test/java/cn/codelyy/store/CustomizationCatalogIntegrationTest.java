package cn.codelyy.store;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomizationCatalogIntegrationTest extends FixtureTestSupport {
    @Test
    void filtersBeforePaginationAndReportsOnlyCustomizableTotal() throws Exception {
        MockHttpSession seller = seller();
        long custom = create(seller, "定制木牌");
        configure(seller, custom, true);
        create(seller, "普通相框");
        create(seller, "普通摆件");
        mvc.perform(get("/api/public/products").param("customizable", "true").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items[0].id").value(custom))
            .andExpect(jsonPath("$.items[0].customizable").value(true));
        mvc.perform(get("/api/public/products").param("customizable", "true").param("size", "1").param("page", "1"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.items").isEmpty());
        mvc.perform(get("/api/public/products"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3));
    }

    @Test
    void disabledAndDelistedProductsAreExcludedWhileDetailReflectsSettings() throws Exception {
        MockHttpSession seller = seller();
        long id = create(seller, "可关闭定制木牌");
        configure(seller, id, true);
        mvc.perform(get("/api/public/products/" + id))
            .andExpect(status().isOk()).andExpect(jsonPath("$.customizable").value(true));
        configure(seller, id, false);
        mvc.perform(get("/api/public/products").param("customizable", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
        configure(seller, id, true);
        mvc.perform(post("/api/seller/products/" + id + "/delist").session(seller))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/public/products").param("customizable", "true"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void directStudioRouteForwardsToFrontend() throws Exception {
        mvc.perform(get("/customization").param("product", "1"))
            .andExpect(status().isOk()).andExpect(forwardedUrl("/index.html"));
    }

    private MockHttpSession seller() throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/seller/login")
            .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"admin\",\"password\":\"123456\"}"))
            .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private long create(MockHttpSession session, String name) throws Exception {
        String response = mvc.perform(post("/api/seller/products").session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content(mapper.writeValueAsString(java.util.Map.of("name", name, "price", 20, "stock", 3))))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).path("id").asLong();
    }

    private void configure(MockHttpSession session, long id, boolean enabled) throws Exception {
        mvc.perform(put("/api/seller/products/" + id + "/customization").session(session)
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"customizable\":" + enabled + ",\"artisanName\":\"木作工坊\",\"artisanBio\":\"手工制作\"}"))
            .andExpect(status().isOk());
    }
}
