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
/** 商品工作台查询: GET /api/seller/products/{id}/workbench */
class Api23Test extends SingleApiTestSupport {
 @Test @DisplayName("API-23-01 查询独立工作台")
 void case01() throws Exception {
  var p=product();var i=intent(p);var r=get("/api/seller/products/"+p.getId()+"/workbench",s,200);eq(r,"/product/id",p.getId());eq(r,"/waitingCount",1);eq(r,"/intents/0/id",i.getId());eq(r,"/activeIntent",null);
 }
 @Test @DisplayName("API-23-02 多个商品队列隔离")
 void case02() throws Exception {
  var p=product();intent(p);var q=product();var i=intent(q);var r=get("/api/seller/products/"+q.getId()+"/workbench",s,200);eq(r,"/product/id",q.getId());eq(r,"/waitingCount",1);eq(r,"/intents/0/id",i.getId());assertEquals(1,r.path("intents").size());
 }
 @Test @DisplayName("API-23-03 交易对象正确")
 void case03() throws Exception {
  var p=product();var active=intent(p);var waiting=intent(p);trading(p,active);var r=get("/api/seller/products/"+p.getId()+"/workbench",s,200);eq(r,"/activeIntent/id",active.getId());eq(r,"/waitingCount",1);eq(r,"/intents/1/id",waiting.getId());eq(r,"/intents/1/status","WAITING");
 }
 @Test @DisplayName("API-23-04 商品不存在")
 void case04() throws Exception {
  get("/api/seller/products/"+MISSING+"/workbench",s,404);
 }
 @Test @DisplayName("API-23-05 未登录查询")
 void case05() throws Exception {
  var p=product();get("/api/seller/products/"+p.getId()+"/workbench",null,401);
 }
}
