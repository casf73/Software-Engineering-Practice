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
/** 兼容当前商品冻结: POST /api/seller/products/current/freeze */
class Api40Test extends SingleApiTestSupport {
 @Test @DisplayName("API-40-01 唯一商品冻结")
 void case01() throws Exception {
  var p=product();var i=intent(p);eq(post("/api/seller/products/current/freeze",s,null,200),"/status","MANUALLY_FROZEN");unchanged(i);
 }
 @Test @DisplayName("API-40-02 多商品冲突")
 void case02() throws Exception {
  var p=product();var q=product();post("/api/seller/products/current/freeze",s,null,409);unchanged(p);unchanged(q);
 }
 @Test @DisplayName("API-40-03 未登录冻结")
 void case03() throws Exception {
  var p=product();post("/api/seller/products/current/freeze",null,null,401);unchanged(p);
 }
 @Test @DisplayName("API-40-04 仅客户身份冻结")
 void case04() throws Exception {
  var p=product();post("/api/seller/products/current/freeze",b,null,401);unchanged(p);
 }
 @Test @DisplayName("API-40-05 下架历史商品不受影响")
 void case05() throws Exception {
  var p=product();var old=product("历史下架",ProductStatus.DELISTED,1);var r=post("/api/seller/products/current/freeze",s,null,200);eq(r,"/id",p.getId());eq(r,"/status","MANUALLY_FROZEN");unchanged(old);
 }
}
