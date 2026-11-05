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
/** 客户会话查询: GET /api/customer/session */
class Api12Test extends SingleApiTestSupport {
 @Test @DisplayName("API-12-01 未登录查询")
 void case01() throws Exception {
  var r=get("/api/customer/session",null,200);eq(r,"/authenticated",false);eq(r,"/customer",null);
 }
 @Test @DisplayName("API-12-02 注册后查询")
 void case02() throws Exception {
  var session=new MockHttpSession();var registered=post("/api/customer/register",session,registration(),201);var r=get("/api/customer/session",session,200);eq(r,"/authenticated",true);eq(r,"/customer/id",registered.at("/customer/id"));eq(r,"/customer/username","newbuyer");
 }
 @Test @DisplayName("API-12-03 登录后查询")
 void case03() throws Exception {
  var session=new MockHttpSession();post("/api/customer/login",session,credentials("buyer01","Buyer1234"),200);var r=get("/api/customer/session",session,200);eq(r,"/customer/id",customer.getId());eq(r,"/authenticated",true);noSecrets(r);
 }
 @Test @DisplayName("API-12-04 客户退出后查询")
 void case04() throws Exception {
  post("/api/customer/logout",b,null,204);var r=get("/api/customer/session",b,200);eq(r,"/authenticated",false);eq(r,"/customer",null);
 }
 @Test @DisplayName("API-12-05 退出卖家不影响客户")
 void case05() throws Exception {
  var both=dual();post("/api/seller/logout",both,null,204);var r=get("/api/customer/session",both,200);eq(r,"/authenticated",true);eq(r,"/customer/id",customer.getId());
 }
}
