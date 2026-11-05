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
/** 商品列表查询: GET /api/public/products */
class Api01Test extends SingleApiTestSupport {
 @Test @DisplayName("API-01-01 匿名查询公开商品")
 void case01() throws Exception {
  var p=product();product("下架",ProductStatus.DELISTED,1);var r=get("/api/public/products?page=0&size=10",null,200);eq(r,"/total",1);eq(r,"/items/0/id",p.getId());eq(r,"/page",0);eq(r,"/size",10);
 }
 @Test @DisplayName("API-01-02 关键字忽略大小写")
 void case02() throws Exception {
  var p=product("Keyboard键盘",ProductStatus.ON_SALE,2);var r=get("/api/public/products?q=keyboard",null,200);eq(r,"/total",1);eq(r,"/items/0/id",p.getId());
 }
 @Test @DisplayName("API-01-03 一级分类包含子分类")
 void case03() throws Exception {
  var root=category("电子产品",null);var child=category("键盘",root);var p=product();p.setCategory(child);products.saveAndFlush(p);var r=get("/api/public/products?categoryId="+root.getId(),null,200);eq(r,"/total",1);eq(r,"/items/0/id",p.getId());
 }
 @Test @DisplayName("API-01-04 关键字无匹配结果")
 void case04() throws Exception {
  product();var r=get("/api/public/products?q=nomatch_unique",null,200);eq(r,"/items",List.of());eq(r,"/total",0);eq(r,"/page",0);eq(r,"/size",20);
 }
 @Test @DisplayName("API-01-05 非法分页下界")
 void case05() throws Exception {
  product();var r=get("/api/public/products?page=-1&size=10",null,400);assertTrue(r.path("items").isMissingNode());
 }
}
