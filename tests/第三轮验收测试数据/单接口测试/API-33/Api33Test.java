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
/** 登记交易结果: POST /api/seller/trades/{intentId}/result */
class Api33Test extends SingleApiTestSupport {
 @Test @DisplayName("API-33-01 成交成功扣1")
 void case01() throws Exception {
  var p=product();var i=intent(p);trading(p,i);post("/api/seller/trades/"+i.getId()+"/result",s,body("success",true,"disposition",null),204);assertEquals(1,reload(p).getStock());assertEquals(ProductStatus.ON_SALE,reload(p).getStatus());assertEquals(IntentStatus.TRADE_SUCCESS,reload(i).getStatus());assertNull(reload(i).getPasscode());get("/api/public/intents/"+i.getPasscode(),null,404);
 }
 @Test @DisplayName("API-33-02 交易失败重排")
 void case02() throws Exception {
  var p=product();var i=intent(p);intent(p);trading(p,i);post("/api/seller/trades/"+i.getId()+"/result",s,body("success",false,"disposition","REQUEUE"),204);assertEquals(2,reload(p).getStock());assertEquals(IntentStatus.WAITING,reload(i).getStatus());assertEquals(i.getPasscode(),reload(i).getPasscode());eq(get("/api/public/intents/"+i.getPasscode(),null,200),"/position",2);
 }
 @Test @DisplayName("API-33-03 交易失败作废")
 void case03() throws Exception {
  var p=product();var i=intent(p);trading(p,i);post("/api/seller/trades/"+i.getId()+"/result",s,body("success",false,"disposition","VOID"),204);assertEquals(2,reload(p).getStock());assertEquals(IntentStatus.TRADE_FAILED,reload(i).getStatus());assertNull(reload(i).getPasscode());get("/api/public/intents/"+i.getPasscode(),null,404);
 }
 @Test @DisplayName("API-33-04 重复登记不再扣库存")
 void case04() throws Exception {
  var p=product();var i=intent(p);trading(p,i);post("/api/seller/trades/"+i.getId()+"/result",s,body("success",true),204);var ended=reload(i);var saved=reload(p);post("/api/seller/trades/"+i.getId()+"/result",s,body("success",true),409);unchanged(ended);unchanged(saved);
 }
 @Test @DisplayName("API-33-05 未登录登记")
 void case05() throws Exception {
  var p=product();var i=intent(p);trading(p,i);post("/api/seller/trades/"+i.getId()+"/result",null,body("success",true),401);unchanged(p);unchanged(i);
 }
}
