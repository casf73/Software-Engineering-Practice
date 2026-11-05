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
/** 冻结商品: POST /api/seller/products/{id}/freeze */
class Api24Test extends SingleApiTestSupport {
 @Test @DisplayName("API-24-01 正常手动冻结")
 void case01() throws Exception {
  var p=product();var i=intent(p);var r=post("/api/seller/products/"+p.getId()+"/freeze",s,null,200);eq(r,"/status","MANUALLY_FROZEN");unchanged(i);
 }
 @Test @DisplayName("API-24-02 冻结后拒绝新增")
 void case02() throws Exception {
  var p=product();var i=intent(p);post("/api/seller/products/"+p.getId()+"/freeze",s,null,200);post("/api/customer/products/"+p.getId()+"/intents",b,contact(),409);assertEquals(1,intents.count());unchanged(i);
 }
 @Test @DisplayName("API-24-03 其他商品不受影响")
 void case03() throws Exception {
  var p=product();var q=product();var i=intent(q);post("/api/seller/products/"+p.getId()+"/freeze",s,null,200);assertEquals(ProductStatus.MANUALLY_FROZEN,reload(p).getStatus());unchanged(q);unchanged(i);
 }
 @Test @DisplayName("API-24-04 未登录冻结")
 void case04() throws Exception {
  var p=product();post("/api/seller/products/"+p.getId()+"/freeze",null,null,401);unchanged(p);
 }
 @Test @DisplayName("API-24-05 商品不存在")
 void case05() throws Exception {
  post("/api/seller/products/"+MISSING+"/freeze",s,null,404);
 }
}
