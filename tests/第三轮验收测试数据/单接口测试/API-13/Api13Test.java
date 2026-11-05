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
/** 客户退出: POST /api/customer/logout */
class Api13Test extends SingleApiTestSupport {
 @Test @DisplayName("API-13-01 已登录客户退出")
 void case01() throws Exception {
  post("/api/customer/logout",b,null,204);loggedOut(b,"customer");
 }
 @Test @DisplayName("API-13-02 重复退出")
 void case02() throws Exception {
  post("/api/customer/logout",b,null,204);post("/api/customer/logout",b,null,204);loggedOut(b,"customer");
 }
 @Test @DisplayName("API-13-03 匿名退出")
 void case03() throws Exception {
  post("/api/customer/logout",null,null,204);
 }
 @Test @DisplayName("API-13-04 双身份仅清除客户")
 void case04() throws Exception {
  var both=dual();post("/api/customer/logout",both,null,204);loggedOut(both,"customer");eq(get("/api/seller/session",both,200),"/authenticated",true);
 }
 @Test @DisplayName("API-13-05 退出后私人接口受限")
 void case05() throws Exception {
  intent(product());post("/api/customer/logout",b,null,204);get("/api/customer/orders",b,401);
 }
}
