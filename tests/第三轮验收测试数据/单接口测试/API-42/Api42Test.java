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
/** 兼容当前商品下架: POST /api/seller/products/current/delist */
class Api42Test extends SingleApiTestSupport {
 @Test @DisplayName("API-42-01 唯一商品下架")
 void case01() throws Exception {
  var p=product();post("/api/seller/products/current/delist",s,null,204);eq(get("/api/public/products",null,200),"/total",0);assertEquals(ProductStatus.DELISTED,reload(p).getStatus());
 }
 @Test @DisplayName("API-42-02 多商品冲突")
 void case02() throws Exception {
  var p=product();var q=product();post("/api/seller/products/current/delist",s,null,409);unchanged(p);unchanged(q);
 }
 @Test @DisplayName("API-42-03 未登录下架")
 void case03() throws Exception {
  var p=product();post("/api/seller/products/current/delist",null,null,401);unchanged(p);
 }
 @Test @DisplayName("API-42-04 仅客户身份下架")
 void case04() throws Exception {
  var p=product();post("/api/seller/products/current/delist",b,null,401);unchanged(p);
 }
 @Test @DisplayName("API-42-05 下架结束等待")
 void case05() throws Exception {
  var p=product();var i=intent(p);post("/api/seller/products/current/delist",s,null,204);assertEquals(IntentStatus.PRODUCT_DELISTED,reload(i).getStatus());assertNull(reload(i).getPasscode());get("/api/public/intents/"+i.getPasscode(),null,404);
 }
}
