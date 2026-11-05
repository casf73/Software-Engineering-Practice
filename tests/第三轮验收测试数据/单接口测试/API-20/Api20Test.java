package cn.codelyy.store.singleapi;
import cn.codelyy.store.domain.*;
import cn.codelyy.store.service.CustomerService;
import cn.codelyy.store.config.SellerAuthInterceptor;
import org.springframework.mock.web.MockHttpSession;
import org.junit.jupiter.api.*;
import java.util.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
/** 卖家商品列表查询: GET /api/seller/products */
class Api20Test extends SingleApiTestSupport {
 @Test @DisplayName("API-20-01 返回管理范围商品")
 void case01() throws Exception {
  var p=product();var q=product("下架",ProductStatus.DELISTED,1);var r=get("/api/seller/products?page=0&size=10",s,200);eq(r,"/total",2);assertEquals(2,r.path("items").size());Set<Long> ids=new HashSet<>();r.path("items").forEach(x->ids.add(x.path("id").asLong()));assertEquals(Set.of(p.getId(),q.getId()),ids);
 }
 @Test @DisplayName("API-20-02 分页大小为1")
 void case02() throws Exception {
  product();product();var r=get("/api/seller/products?page=0&size=1",s,200);eq(r,"/total",2);assertEquals(1,r.path("items").size());
 }
 @Test @DisplayName("API-20-03 合法空页")
 void case03() throws Exception {
  product();var r=get("/api/seller/products?page=1&size=1",s,200);eq(r,"/total",1);eq(r,"/items",List.of());
 }
 @Test @DisplayName("API-20-04 页码为负")
 void case04() throws Exception {
  get("/api/seller/products?page=-1",s,400);
 }
 @Test @DisplayName("API-20-05 未登录查询管理数据")
 void case05() throws Exception {
  product();var r=get("/api/seller/products",null,401);assertTrue(r.path("items").isMissingNode());
 }
}
