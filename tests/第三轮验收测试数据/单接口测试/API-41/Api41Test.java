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
/** 兼容当前商品解冻: POST /api/seller/products/current/unfreeze */
class Api41Test extends SingleApiTestSupport {
 @Test @DisplayName("API-41-01 唯一手动冻结商品解冻")
 void case01() throws Exception {
  var p=product();state(p,ProductStatus.MANUALLY_FROZEN);var i=intent(p);eq(post("/api/seller/products/current/unfreeze",s,null,200),"/status","ON_SALE");unchanged(i);eq(get("/api/public/intents/"+i.getPasscode(),null,200),"/position",1);
 }
 @Test @DisplayName("API-41-02 多商品冲突")
 void case02() throws Exception {
  var p=product();state(p,ProductStatus.MANUALLY_FROZEN);var q=product();state(q,ProductStatus.MANUALLY_FROZEN);post("/api/seller/products/current/unfreeze",s,null,409);unchanged(p);unchanged(q);
 }
 @Test @DisplayName("API-41-03 不能解除交易冻结")
 void case03() throws Exception {
  var p=product();var i=intent(p);trading(p,i);post("/api/seller/products/current/unfreeze",s,null,409);unchanged(p);unchanged(i);
 }
 @Test @DisplayName("API-41-04 未登录解冻")
 void case04() throws Exception {
  var p=product();state(p,ProductStatus.MANUALLY_FROZEN);post("/api/seller/products/current/unfreeze",null,null,401);unchanged(p);
 }
 @Test @DisplayName("API-41-05 仅客户身份解冻")
 void case05() throws Exception {
  var p=product();state(p,ProductStatus.MANUALLY_FROZEN);post("/api/seller/products/current/unfreeze",b,null,401);unchanged(p);
 }
}
