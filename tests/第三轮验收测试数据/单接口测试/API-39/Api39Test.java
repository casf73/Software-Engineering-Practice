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
/** 兼容当前商品编辑: PUT /api/seller/products/current */
class Api39Test extends SingleApiTestSupport {
 @Test @DisplayName("API-39-01 正常编辑唯一商品")
 void case01() throws Exception {
  var p=product();var r=put("/api/seller/products/current",s,productBody(),200);eq(r,"/id",p.getId());eq(r,"/name","机械键盘");eq(r,"/stock",3);assertEquals(1,products.count());
 }
 @Test @DisplayName("API-39-02 多商品冲突")
 void case02() throws Exception {
  var p=product();var q=product();put("/api/seller/products/current",s,productBody(),409);unchanged(p);unchanged(q);
 }
 @Test @DisplayName("API-39-03 非法价格")
 void case03() throws Exception {
  var p=product();var data=productBody();data.put("price",0);put("/api/seller/products/current",s,data,400);unchanged(p);
 }
 @Test @DisplayName("API-39-04 交易中禁止编辑")
 void case04() throws Exception {
  var p=product();var i=intent(p);trading(p,i);put("/api/seller/products/current",s,productBody(),409);unchanged(p);unchanged(i);
 }
 @Test @DisplayName("API-39-05 未登录编辑")
 void case05() throws Exception {
  var p=product();put("/api/seller/products/current",null,productBody(),401);unchanged(p);
 }
}
