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
/** 卖家客户详情查询: GET /api/seller/customers/{id} */
class Api36Test extends SingleApiTestSupport {
 @Test @DisplayName("API-36-01 正常查询客户")
 void case01() throws Exception {
  var r=get("/api/seller/customers/"+customer.getId(),s,200);eq(r,"/id",customer.getId());eq(r,"/username","buyer01");eq(r,"/phone","13800000001");eq(r,"/defaultLocation","学校南门");assertFalse(r.path("createdAt").asText().isBlank());noSecrets(r);
 }
 @Test @DisplayName("API-36-02 客户不存在")
 void case02() throws Exception {
  get("/api/seller/customers/"+MISSING,s,404);
 }
 @Test @DisplayName("API-36-03 访客不能查询")
 void case03() throws Exception {
  get("/api/seller/customers/"+customer.getId(),null,401);
 }
 @Test @DisplayName("API-36-04 仅客户身份不能查询")
 void case04() throws Exception {
  get("/api/seller/customers/"+customer.getId(),b,401);
 }
 @Test @DisplayName("API-36-05 双身份按卖家权限查询")
 void case05() throws Exception {
  var other=customer("buyer02");var r=get("/api/seller/customers/"+other.getId(),dual(),200);eq(r,"/id",other.getId());eq(r,"/username","buyer02");assertNotEquals(customer.getId().longValue(),r.path("id").asLong());
 }
}
