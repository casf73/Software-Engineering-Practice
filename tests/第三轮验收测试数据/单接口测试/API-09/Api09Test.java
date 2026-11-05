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
/** 口令码意向撤销: DELETE /api/public/intents/{passcode} */
class Api09Test extends SingleApiTestSupport {
 @Test @DisplayName("API-09-01 等待意向正常撤销")
 void case01() throws Exception {
  var i=intent(product());delete("/api/public/intents/"+i.getPasscode(),null,204);assertEquals(IntentStatus.CANCELLED,reload(i).getStatus());assertNull(reload(i).getPasscode());eq(get("/api/customer/orders",b,200),"/items/0/status","CANCELLED");get("/api/public/intents/"+i.getPasscode(),null,404);
 }
 @Test @DisplayName("API-09-02 同商品其他等待者可撤销")
 void case02() throws Exception {
  var p=product();var active=intent(p);var waiting=intent(p);trading(p,active);delete("/api/public/intents/"+waiting.getPasscode(),null,204);assertEquals(IntentStatus.CANCELLED,reload(waiting).getStatus());unchanged(active);assertEquals(ProductStatus.IN_TRADE,reload(p).getStatus());
 }
 @Test @DisplayName("API-09-03 交易中本人不可撤销")
 void case03() throws Exception {
  var p=product();var i=intent(p);trading(p,i);delete("/api/public/intents/"+i.getPasscode(),null,409);unchanged(i);unchanged(p);
 }
 @Test @DisplayName("API-09-04 重复撤销")
 void case04() throws Exception {
  var p=product();var i=intent(p);delete("/api/public/intents/"+i.getPasscode(),null,204);var ended=reload(i);delete("/api/public/intents/"+i.getPasscode(),null,404);unchanged(ended);unchanged(p);
 }
 @Test @DisplayName("API-09-05 不存在的口令码")
 void case05() throws Exception {
  intent(product());delete("/api/public/intents/UNKNOWN123",null,404);assertEquals(1,intents.count());
 }
}
