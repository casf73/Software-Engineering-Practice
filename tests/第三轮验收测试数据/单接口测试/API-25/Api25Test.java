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
/** 解冻商品: POST /api/seller/products/{id}/unfreeze */
class Api25Test extends SingleApiTestSupport {
 @Test @DisplayName("API-25-01 手动冻结后解冻")
 void case01() throws Exception {
  var p=product();state(p,ProductStatus.MANUALLY_FROZEN);var i=intent(p);eq(post("/api/seller/products/"+p.getId()+"/unfreeze",s,null,200),"/status","ON_SALE");unchanged(i);eq(get("/api/public/intents/"+i.getPasscode(),null,200),"/position",1);
 }
 @Test @DisplayName("API-25-02 不能解冻交易冻结")
 void case02() throws Exception {
  var p=product();var i=intent(p);trading(p,i);post("/api/seller/products/"+p.getId()+"/unfreeze",s,null,409);unchanged(p);unchanged(i);
 }
 @Test @DisplayName("API-25-03 未登录解冻")
 void case03() throws Exception {
  var p=product();state(p,ProductStatus.MANUALLY_FROZEN);post("/api/seller/products/"+p.getId()+"/unfreeze",null,null,401);unchanged(p);
 }
 @Test @DisplayName("API-25-04 商品不存在")
 void case04() throws Exception {
  post("/api/seller/products/"+MISSING+"/unfreeze",s,null,404);
 }
 @Test @DisplayName("API-25-05 解冻后恢复接单")
 void case05() throws Exception {
  var p=product();state(p,ProductStatus.MANUALLY_FROZEN);var i=intent(p);post("/api/seller/products/"+p.getId()+"/unfreeze",s,null,200);eq(post("/api/customer/products/"+p.getId()+"/intents",b,contact(),201),"/position",2);unchanged(i);
 }
}
