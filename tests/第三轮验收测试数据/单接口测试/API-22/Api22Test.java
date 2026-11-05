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
/** 编辑商品: PUT /api/seller/products/{id} */
class Api22Test extends SingleApiTestSupport {
 @Test @DisplayName("API-22-01 正常修改商品")
 void case01() throws Exception {
  var p=product();var q=product();var data=productBody();data.put("name","新名称");data.put("price",99.50);var r=put("/api/seller/products/"+p.getId(),s,data,200);eq(r,"/id",p.getId());eq(r,"/name","新名称");eq(r,"/price",99.50);eq(r,"/description","<p>商品说明</p>");unchanged(q);
 }
 @Test @DisplayName("API-22-02 价格低于下界")
 void case02() throws Exception {
  var p=product();var data=productBody();data.put("price",0);put("/api/seller/products/"+p.getId(),s,data,400);unchanged(p);
 }
 @Test @DisplayName("API-22-03 库存改为0")
 void case03() throws Exception {
  var p=product();var data=productBody();data.put("stock",0);var r=put("/api/seller/products/"+p.getId(),s,data,200);eq(r,"/stock",0);eq(r,"/status","DELISTED");get("/api/public/products/"+p.getId(),null,404);
 }
 @Test @DisplayName("API-22-04 交易中拒绝编辑")
 void case04() throws Exception {
  var p=product();var i=intent(p);trading(p,i);put("/api/seller/products/"+p.getId(),s,productBody(),409);unchanged(p);unchanged(i);
 }
 @Test @DisplayName("API-22-05 商品不存在")
 void case05() throws Exception {
  product();put("/api/seller/products/"+MISSING,s,productBody(),404);
 }
}
