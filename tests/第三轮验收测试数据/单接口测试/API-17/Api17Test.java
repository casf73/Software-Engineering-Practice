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
/** 卖家会话查询: GET /api/seller/session */
class Api17Test extends SingleApiTestSupport {
 @Test @DisplayName("API-17-01 未登录查询")
 void case01() throws Exception {
  eq(get("/api/seller/session",null,200),"/authenticated",false);
 }
 @Test @DisplayName("API-17-02 有效卖家查询")
 void case02() throws Exception {
  eq(get("/api/seller/session",s,200),"/authenticated",true);
 }
 @Test @DisplayName("API-17-03 仅客户身份查询")
 void case03() throws Exception {
  eq(get("/api/seller/session",b,200),"/authenticated",false);
 }
 @Test @DisplayName("API-17-04 卖家退出后查询")
 void case04() throws Exception {
  post("/api/seller/logout",s,null,204);eq(get("/api/seller/session",s,200),"/authenticated",false);
 }
 @Test @DisplayName("API-17-05 改密使旧会话失效")
 void case05() throws Exception {
  var old=new MockHttpSession();old.setAttribute(SellerAuthInterceptor.SESSION_KEY,s.getAttribute(SellerAuthInterceptor.SESSION_KEY));put("/api/seller/password",s,body("currentPassword","123456","newPassword","Changed1234"),204);eq(get("/api/seller/session",old,200),"/authenticated",false);
 }
}
