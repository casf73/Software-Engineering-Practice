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
/** 卖家修改密码: PUT /api/seller/password */
class Api19Test extends SingleApiTestSupport {
 @Test @DisplayName("API-19-01 合法改密")
 void case01() throws Exception {
  var both=dual();var old=new MockHttpSession();old.setAttribute(SellerAuthInterceptor.SESSION_KEY,both.getAttribute(SellerAuthInterceptor.SESSION_KEY));put("/api/seller/password",both,body("currentPassword","123456","newPassword","Changed1234"),204);loggedOut(old,"seller");eq(post("/api/seller/login",new MockHttpSession(),credentials("admin","Changed1234"),200),"/authenticated",true);eq(get("/api/customer/session",both,200),"/customer/id",customer.getId());post("/api/seller/login",null,credentials("admin","123456"),401);
 }
 @Test @DisplayName("API-19-02 未登录改密")
 void case02() throws Exception {
  String hash=accounts.findById(1L).orElseThrow().getPasswordHash();put("/api/seller/password",null,body("currentPassword","123456","newPassword","Changed1234"),401);assertEquals(hash,accounts.findById(1L).orElseThrow().getPasswordHash());
 }
 @Test @DisplayName("API-19-03 仅客户身份改密")
 void case03() throws Exception {
  String hash=accounts.findById(1L).orElseThrow().getPasswordHash();put("/api/seller/password",b,body("currentPassword","123456","newPassword","Changed1234"),401);assertEquals(hash,accounts.findById(1L).orElseThrow().getPasswordHash());
 }
 @Test @DisplayName("API-19-04 新密码低于下界")
 void case04() throws Exception {
  put("/api/seller/password",s,body("currentPassword","123456","newPassword","1234567"),400);eq(post("/api/seller/login",null,credentials("admin","123456"),200),"/authenticated",true);
 }
 @Test @DisplayName("API-19-05 缺少当前密码")
 void case05() throws Exception {
  String hash=accounts.findById(1L).orElseThrow().getPasswordHash();put("/api/seller/password",s,body("currentPassword","","newPassword","Changed1234"),400);assertEquals(hash,accounts.findById(1L).orElseThrow().getPasswordHash());
 }
}
