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
/** 本人购买历史查询: GET /api/customer/orders */
class Api14Test extends SingleApiTestSupport {
 @Test @DisplayName("API-14-01 本人记录及快照")
 void case01() throws Exception {
  var p=product();var i=intent(p);p.setName("新名称");p.setPrice(new BigDecimal("999.00"));products.saveAndFlush(p);var r=get("/api/customer/orders",b,200);eq(r,"/total",1);eq(r,"/items/0/id",i.getId());eq(r,"/items/0/productName","测试商品");eq(r,"/items/0/price",88.50);eq(r,"/items/0/meetingLocation","学校南门");
 }
 @Test @DisplayName("API-14-02 不同客户隔离")
 void case02() throws Exception {
  intent(product());var other=customer("buyer02");var r=get("/api/customer/orders",buyer(other),200);eq(r,"/total",0);eq(r,"/items",List.of());
 }
 @Test @DisplayName("API-14-03 未登录访问")
 void case03() throws Exception {
  intent(product());var r=get("/api/customer/orders",null,401);assertTrue(r.path("items").isMissingNode());
 }
 @Test @DisplayName("API-14-04 合法空页")
 void case04() throws Exception {
  intent(product());var r=get("/api/customer/orders?page=1&size=1",b,200);eq(r,"/items",List.of());eq(r,"/total",1);eq(r,"/page",1);eq(r,"/size",1);
 }
 @Test @DisplayName("API-14-05 非法分页大小")
 void case05() throws Exception {
  get("/api/customer/orders?size=0",b,400);
 }
}
