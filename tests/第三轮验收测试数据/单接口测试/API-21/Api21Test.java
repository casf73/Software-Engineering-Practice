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
/** 发布商品: POST /api/seller/products */
class Api21Test extends SingleApiTestSupport {
 @Test @DisplayName("API-21-01 正常发布")
 void case01() throws Exception {
  var r=post("/api/seller/products",s,productBody(),201);eq(r,"/name","机械键盘");eq(r,"/price",88.50);eq(r,"/stock",3);eq(r,"/status","ON_SALE");assertTrue(r.path("id").asLong()>0);assertEquals(1,products.count());
 }
 @Test @DisplayName("API-21-02 名称为空")
 void case02() throws Exception {
  var data=productBody();data.put("name","");post("/api/seller/products",s,data,400);assertEquals(0,products.count());
 }
 @Test @DisplayName("API-21-03 负库存")
 void case03() throws Exception {
  var data=productBody();data.put("stock",-1);post("/api/seller/products",s,data,400);assertEquals(0,products.count());
 }
 @Test @DisplayName("API-21-04 图片数量上界")
 void case04() throws Exception {
  var urls=images(9);var data=productBody();data.put("images",urls);var r=post("/api/seller/products",s,data,201);eq(r,"/images",urls);
 }
 @Test @DisplayName("API-21-05 未登录发布")
 void case05() throws Exception {
  post("/api/seller/products",null,productBody(),401);assertEquals(0,products.count());
 }
}
