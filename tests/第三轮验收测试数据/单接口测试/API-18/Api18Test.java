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
/** 卖家退出: POST /api/seller/logout */
class Api18Test extends SingleApiTestSupport {
 @Test @DisplayName("API-18-01 已登录卖家退出")
 void case01() throws Exception {
  post("/api/seller/logout",s,null,204);loggedOut(s,"seller");
 }
 @Test @DisplayName("API-18-02 未登录请求退出")
 void case02() throws Exception {
  post("/api/seller/logout",null,null,401);
 }
 @Test @DisplayName("API-18-03 仅客户身份请求退出")
 void case03() throws Exception {
  post("/api/seller/logout",b,null,401);eq(get("/api/customer/session",b,200),"/authenticated",true);
 }
 @Test @DisplayName("API-18-04 双身份仅清除卖家")
 void case04() throws Exception {
  var both=dual();post("/api/seller/logout",both,null,204);loggedOut(both,"seller");eq(get("/api/customer/session",both,200),"/authenticated",true);
 }
 @Test @DisplayName("API-18-05 退出后管理接口受限")
 void case05() throws Exception {
  post("/api/seller/logout",s,null,204);get("/api/seller/products",s,401);
 }
}
