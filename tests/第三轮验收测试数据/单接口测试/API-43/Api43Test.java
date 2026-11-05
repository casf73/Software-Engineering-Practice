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
/** 兼容当前商品开始交易: POST /api/seller/trades/start */
class Api43Test extends SingleApiTestSupport {
 @Test @DisplayName("API-43-01 唯一商品队首交易")
 void case01() throws Exception {
  var p=product();var first=intent(p);var second=intent(p);var r=post("/api/seller/trades/start",s,null,200);eq(r,"/id",first.getId());eq(r,"/status","IN_TRADE");assertEquals(ProductStatus.IN_TRADE,reload(p).getStatus());unchanged(second);
 }
 @Test @DisplayName("API-43-02 多商品冲突")
 void case02() throws Exception {
  var p=product();var first=intent(p);var q=product();var other=intent(q);post("/api/seller/trades/start",s,null,409);unchanged(p);unchanged(q);unchanged(first);unchanged(other);
 }
 @Test @DisplayName("API-43-03 唯一商品无等待者")
 void case03() throws Exception {
  var p=product();post("/api/seller/trades/start",s,null,409);unchanged(p);assertEquals(0,intents.count());
 }
 @Test @DisplayName("API-43-04 未登录开始交易")
 void case04() throws Exception {
  var p=product();var i=intent(p);post("/api/seller/trades/start",null,null,401);unchanged(p);unchanged(i);
 }
 @Test @DisplayName("API-43-05 仅客户身份开始交易")
 void case05() throws Exception {
  var p=product();var i=intent(p);post("/api/seller/trades/start",b,null,401);unchanged(p);unchanged(i);
 }
}
