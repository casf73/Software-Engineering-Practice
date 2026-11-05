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
/** 商品详情查询: GET /api/public/products/{id} */
class Api02Test extends SingleApiTestSupport {
 @Test @DisplayName("API-02-01 查询在售商品")
 void case01() throws Exception {
  var c=category("电子产品",null);var p=product();p.setCategory(c);products.saveAndFlush(p);var r=get("/api/public/products/"+p.getId(),null,200);eq(r,"/id",p.getId());eq(r,"/name",p.getName());assertEquals(0,p.getPrice().compareTo(r.path("price").decimalValue()));eq(r,"/stock",2);eq(r,"/categoryId",c.getId());eq(r,"/description",p.getDescription());
 }
 @Test @DisplayName("API-02-02 多图顺序展示")
 void case02() throws Exception {
  var p=product();var urls=images(9);p.setImageUrls(String.join("|",urls));products.saveAndFlush(p);var r=get("/api/public/products/"+p.getId(),null,200);eq(r,"/images",urls);
 }
 @Test @DisplayName("API-02-03 查询无图商品")
 void case03() throws Exception {
  var p=product();var r=get("/api/public/products/"+p.getId(),null,200);eq(r,"/images",List.of());eq(r,"/id",p.getId());eq(r,"/stock",2);
 }
 @Test @DisplayName("API-02-04 下架商品不可公开")
 void case04() throws Exception {
  var p=product("下架",ProductStatus.DELISTED,1);var r=get("/api/public/products/"+p.getId(),null,404);assertTrue(r.path("id").isMissingNode());
 }
 @Test @DisplayName("API-02-05 商品不存在")
 void case05() throws Exception {
  product();var r=get("/api/public/products/"+MISSING,null,404);assertTrue(r.path("id").isMissingNode());
 }
}
