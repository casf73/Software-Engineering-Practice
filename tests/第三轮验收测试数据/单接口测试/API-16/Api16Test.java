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
/** 卖家登录: POST /api/seller/login */
class Api16Test extends SingleApiTestSupport {
 @Test @DisplayName("API-16-01 正确凭据登录")
 void case01() throws Exception {
  var session=new MockHttpSession();var r=post("/api/seller/login",session,credentials("admin","123456"),200);eq(r,"/authenticated",true);assertNotNull(session.getAttribute(SellerAuthInterceptor.SESSION_KEY));
 }
 @Test @DisplayName("API-16-02 密码错误")
 void case02() throws Exception {
  var session=new MockHttpSession();post("/api/seller/login",session,credentials("admin","Wrong1234"),401);assertNull(session.getAttribute(SellerAuthInterceptor.SESSION_KEY));
 }
 @Test @DisplayName("API-16-03 卖家账号不存在")
 void case03() throws Exception {
  post("/api/seller/login",null,credentials("missing","123456"),401);
 }
 @Test @DisplayName("API-16-04 用户名为空")
 void case04() throws Exception {
  post("/api/seller/login",null,credentials("","123456"),400);
 }
 @Test @DisplayName("API-16-05 保留已有客户身份")
 void case05() throws Exception {
  post("/api/seller/login",b,credentials("admin","123456"),200);eq(get("/api/customer/session",b,200),"/customer/id",customer.getId());eq(get("/api/seller/session",b,200),"/authenticated",true);
 }
}
