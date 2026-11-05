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
/** 口令码意向查询: GET /api/public/intents/{passcode} */
class Api07Test extends SingleApiTestSupport {
 @Test @DisplayName("API-07-01 等待状态正常查询")
 void case01() throws Exception {
  var p=product();var i=intent(p);var r=get("/api/public/intents/"+i.getPasscode(),null,200);eq(r,"/name",i.getName());eq(r,"/phone",i.getPhone());eq(r,"/status","WAITING");eq(r,"/position",1);eq(r,"/productStatus","ON_SALE");
 }
 @Test @DisplayName("API-07-02 重排后查询排位")
 void case02() throws Exception {
  var p=product();var i=intent(p);intent(p);post("/api/seller/intents/"+i.getId()+"/action",s,body("action","REQUEUE"),204);var r=get("/api/public/intents/"+i.getPasscode(),null,200);eq(r,"/position",2);eq(r,"/status","WAITING");
 }
 @Test @DisplayName("API-07-03 成交后原码失效")
 void case03() throws Exception {
  var p=product();var i=intent(p);trading(p,i);post("/api/seller/trades/"+i.getId()+"/result",s,body("success",true),204);get("/api/public/intents/"+i.getPasscode(),null,404);
 }
 @Test @DisplayName("API-07-04 撤销后原码失效")
 void case04() throws Exception {
  var i=intent(product());delete("/api/public/intents/"+i.getPasscode(),null,204);get("/api/public/intents/"+i.getPasscode(),null,404);
 }
 @Test @DisplayName("API-07-05 不存在的口令码")
 void case05() throws Exception {
  intent(product());var r=get("/api/public/intents/UNKNOWN123",null,404);assertTrue(r.path("name").isMissingNode());
 }
}
