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
/** 客户登录: POST /api/customer/login */
class Api11Test extends SingleApiTestSupport {
 @Test @DisplayName("API-11-01 正确凭据登录")
 void case01() throws Exception {
  var session=new MockHttpSession();String old=session.getId();var r=post("/api/customer/login",session,credentials("buyer01","Buyer1234"),200);eq(r,"/authenticated",true);eq(r,"/customer/id",customer.getId());noSecrets(r);assertNotEquals(old,session.getId());assertEquals(customer.getId(),session.getAttribute(CustomerService.SESSION_KEY));
 }
 @Test @DisplayName("API-11-02 密码错误")
 void case02() throws Exception {
  var session=new MockHttpSession();post("/api/customer/login",session,credentials("buyer01","Wrong1234"),401);assertNull(session.getAttribute(CustomerService.SESSION_KEY));
 }
 @Test @DisplayName("API-11-03 账号不存在")
 void case03() throws Exception {
  var session=new MockHttpSession();post("/api/customer/login",session,credentials("missingbuyer","Buyer1234"),401);assertNull(session.getAttribute(CustomerService.SESSION_KEY));
 }
 @Test @DisplayName("API-11-04 缺少用户名")
 void case04() throws Exception {
  post("/api/customer/login",null,credentials("","Buyer1234"),400);
 }
 @Test @DisplayName("API-11-05 密码超过请求上限")
 void case05() throws Exception {
  var session=new MockHttpSession();post("/api/customer/login",session,credentials("buyer01","a".repeat(129)),400);assertNull(session.getAttribute(CustomerService.SESSION_KEY));
 }
}
