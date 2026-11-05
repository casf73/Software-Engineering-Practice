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
/** 下架商品: POST /api/seller/products/{id}/delist */
class Api27Test extends SingleApiTestSupport {
 @Test @DisplayName("API-27-01 正常下架")
 void case01() throws Exception {
  var p=product();post("/api/seller/products/"+p.getId()+"/delist",s,null,204);eq(get("/api/public/products",null,200),"/total",0);get("/api/public/products/"+p.getId(),null,404);
 }
 @Test @DisplayName("API-27-02 等待意向结束")
 void case02() throws Exception {
  var p=product();var i=intent(p);post("/api/seller/products/"+p.getId()+"/delist",s,null,204);assertEquals(IntentStatus.PRODUCT_DELISTED,reload(i).getStatus());assertNull(reload(i).getPasscode());get("/api/public/intents/"+i.getPasscode(),null,404);
 }
 @Test @DisplayName("API-27-03 商品之间隔离")
 void case03() throws Exception {
  var p=product();var q=product();var i=intent(q);post("/api/seller/products/"+p.getId()+"/delist",s,null,204);unchanged(q);unchanged(i);
 }
 @Test @DisplayName("API-27-04 商品不存在")
 void case04() throws Exception {
  post("/api/seller/products/"+MISSING+"/delist",s,null,404);
 }
 @Test @DisplayName("API-27-05 未登录下架")
 void case05() throws Exception {
  var p=product();post("/api/seller/products/"+p.getId()+"/delist",null,null,401);unchanged(p);
 }
}
