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
/** 兼容当前商品工作台: GET /api/seller/workbench */
class Api38Test extends SingleApiTestSupport {
 @Test @DisplayName("API-38-01 唯一商品工作台")
 void case01() throws Exception {
  var p=product();var r=get("/api/seller/workbench",s,200);eq(r,"/product/id",p.getId());eq(r,"/intents",List.of());eq(r,"/waitingCount",0);eq(r,"/activeIntent",null);
 }
 @Test @DisplayName("API-38-02 多商品冲突")
 void case02() throws Exception {
  var p=product();var q=product();get("/api/seller/workbench",s,409);unchanged(p);unchanged(q);
 }
 @Test @DisplayName("API-38-03 未登录查询")
 void case03() throws Exception {
  product();get("/api/seller/workbench",null,401);
 }
 @Test @DisplayName("API-38-04 仅客户身份查询")
 void case04() throws Exception {
  product();get("/api/seller/workbench",b,401);
 }
 @Test @DisplayName("API-38-05 队列和交易对象一致")
 void case05() throws Exception {
  var p=product();var active=intent(p);var waiting=intent(p);trading(p,active);var r=get("/api/seller/workbench",s,200);eq(r,"/product/id",p.getId());eq(r,"/activeIntent/id",active.getId());eq(r,"/waitingCount",1);assertEquals(2,r.path("intents").size());eq(r,"/intents/1/id",waiting.getId());eq(r,"/intents/1/position",1);
 }
}
