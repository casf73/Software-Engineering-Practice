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
/** 卖家客户列表查询: GET /api/seller/customers */
class Api35Test extends SingleApiTestSupport {
 @Test @DisplayName("API-35-01 用户名搜索")
 void case01() throws Exception {
  var r=get("/api/seller/customers?q=buyer",s,200);eq(r,"/total",1);eq(r,"/items/0/id",customer.getId());noSecrets(r.path("items").get(0));
 }
 @Test @DisplayName("API-35-02 电话搜索")
 void case02() throws Exception {
  var r=get("/api/seller/customers?q=13800000001",s,200);eq(r,"/total",1);eq(r,"/items/0/id",customer.getId());
 }
 @Test @DisplayName("API-35-03 无匹配结果")
 void case03() throws Exception {
  var r=get("/api/seller/customers?q=nomatch_unique",s,200);eq(r,"/total",0);eq(r,"/items",List.of());
 }
 @Test @DisplayName("API-35-04 非法分页")
 void case04() throws Exception {
  get("/api/seller/customers?size=0",s,400);
 }
 @Test @DisplayName("API-35-05 客户身份不能访问")
 void case05() throws Exception {
  var r=get("/api/seller/customers",b,401);assertTrue(r.path("items").isMissingNode());
 }
}
