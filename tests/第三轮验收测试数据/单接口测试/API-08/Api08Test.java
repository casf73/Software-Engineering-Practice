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
/** 口令码联系信息修改: PUT /api/public/intents/{passcode} */
class Api08Test extends SingleApiTestSupport {
 @Test @DisplayName("API-08-01 等待者修改成功")
 void case01() throws Exception {
  var i=intent(product());var before=i.getQueueAt();var r=put("/api/public/intents/"+i.getPasscode(),null,body("name","新联系人","phone","13900000001"),200);eq(r,"/name","新联系人");eq(r,"/phone","13900000001");eq(r,"/position",1);assertEquals(i.getPasscode(),reload(i).getPasscode());assertEquals(before,reload(i).getQueueAt());
 }
 @Test @DisplayName("API-08-02 非法电话")
 void case02() throws Exception {
  var i=intent(product());put("/api/public/intents/"+i.getPasscode(),null,body("name","新人","phone","bad"),400);unchanged(i);
 }
 @Test @DisplayName("API-08-03 姓名为空")
 void case03() throws Exception {
  var i=intent(product());put("/api/public/intents/"+i.getPasscode(),null,body("name","","phone","13900000001"),400);unchanged(i);
 }
 @Test @DisplayName("API-08-04 交易中本人不可修改")
 void case04() throws Exception {
  var p=product();var i=intent(p);trading(p,i);put("/api/public/intents/"+i.getPasscode(),null,contact(),409);unchanged(i);
 }
 @Test @DisplayName("API-08-05 已失效口令码")
 void case05() throws Exception {
  var i=intent(product());delete("/api/public/intents/"+i.getPasscode(),null,204);var ended=reload(i);put("/api/public/intents/"+i.getPasscode(),null,contact(),404);unchanged(ended);
 }
}
