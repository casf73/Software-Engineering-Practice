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
/** 开始交易: POST /api/seller/products/{id}/trades/start */
class Api32Test extends SingleApiTestSupport {
 @Test @DisplayName("API-32-01 队首开始交易")
 void case01() throws Exception {
  var p=product();var first=intent(p);var second=intent(p);var r=post("/api/seller/products/"+p.getId()+"/trades/start",s,null,200);eq(r,"/id",first.getId());eq(r,"/status","IN_TRADE");assertEquals(IntentStatus.IN_TRADE,reload(first).getStatus());assertEquals(ProductStatus.IN_TRADE,reload(p).getStatus());unchanged(second);
 }
 @Test @DisplayName("API-32-02 空队列拒绝开始")
 void case02() throws Exception {
  var p=product();post("/api/seller/products/"+p.getId()+"/trades/start",s,null,409);unchanged(p);assertEquals(0,intents.count());
 }
 @Test @DisplayName("API-32-03 已有交易不能重复开始")
 void case03() throws Exception {
  var p=product();var i=intent(p);trading(p,i);var waiting=intent(p);post("/api/seller/products/"+p.getId()+"/trades/start",s,null,409);unchanged(i);unchanged(waiting);unchanged(p);
 }
 @Test @DisplayName("API-32-04 未登录开始交易")
 void case04() throws Exception {
  var p=product();var i=intent(p);post("/api/seller/products/"+p.getId()+"/trades/start",null,null,401);unchanged(i);unchanged(p);
 }
 @Test @DisplayName("API-32-05 商品不存在")
 void case05() throws Exception {
  post("/api/seller/products/"+MISSING+"/trades/start",s,null,404);
 }
}
