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
/** 兼容当前商品查询: GET /api/public/products/current */
class Api04Test extends SingleApiTestSupport {
 @Test @DisplayName("API-04-01 唯一有效商品")
 void case01() throws Exception {
  var p=product();var r=get("/api/public/products/current",s,200);eq(r,"/id",p.getId());eq(r,"/name",p.getName());eq(r,"/stock",2);
 }
 @Test @DisplayName("API-04-02 匿名访问唯一商品")
 void case02() throws Exception {
  var p=product();eq(get("/api/public/products/current",null,200),"/id",p.getId());
 }
 @Test @DisplayName("API-04-03 两个有效商品产生歧义")
 void case03() throws Exception {
  var p=product();var q=product();var r=get("/api/public/products/current",null,409);assertTrue(r.path("id").isMissingNode());unchanged(p);unchanged(q);
 }
 @Test @DisplayName("API-04-04 三个有效商品产生歧义")
 void case04() throws Exception {
  product();product();product();var r=get("/api/public/products/current",null,409);assertTrue(r.path("id").isMissingNode());
 }
 @Test @DisplayName("API-04-05 当前商品数据实时一致")
 void case05() throws Exception {
  var p=product();var data=productBody();var urls=images(1);data.put("name","新名称");data.put("price",99.50);data.put("images",urls);put("/api/seller/products/"+p.getId(),s,data,200);var r=get("/api/public/products/current",null,200);eq(r,"/id",p.getId());eq(r,"/name","新名称");eq(r,"/price",99.50);eq(r,"/images",urls);
 }
}
