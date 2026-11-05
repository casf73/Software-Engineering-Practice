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
/** 等待意向处理: POST /api/seller/intents/{intentId}/action */
class Api34Test extends SingleApiTestSupport {
 @Test @DisplayName("API-34-01 等待意向重排")
 void case01() throws Exception {
  var p=product();var first=intent(p);var second=intent(p);var q=product();var other=intent(q);post("/api/seller/intents/"+first.getId()+"/action",s,body("action","REQUEUE"),204);eq(get("/api/public/intents/"+first.getPasscode(),null,200),"/position",2);eq(get("/api/public/intents/"+second.getPasscode(),null,200),"/position",1);unchanged(q);unchanged(other);
 }
 @Test @DisplayName("API-34-02 等待意向作废")
 void case02() throws Exception {
  var p=product();var first=intent(p);var second=intent(p);post("/api/seller/intents/"+first.getId()+"/action",s,body("action","VOID"),204);assertEquals(IntentStatus.VOIDED,reload(first).getStatus());assertNull(reload(first).getPasscode());eq(get("/api/public/intents/"+second.getPasscode(),null,200),"/position",1);
 }
 @Test @DisplayName("API-34-03 非法操作名")
 void case03() throws Exception {
  var i=intent(product());post("/api/seller/intents/"+i.getId()+"/action",s,body("action","UNKNOWN"),400);unchanged(i);
 }
 @Test @DisplayName("API-34-04 不能处理交易中意向")
 void case04() throws Exception {
  var p=product();var i=intent(p);trading(p,i);post("/api/seller/intents/"+i.getId()+"/action",s,body("action","VOID"),409);unchanged(i);unchanged(p);
 }
 @Test @DisplayName("API-34-05 未登录处理")
 void case05() throws Exception {
  var i=intent(product());post("/api/seller/intents/"+i.getId()+"/action",null,body("action","VOID"),401);unchanged(i);
 }
}
