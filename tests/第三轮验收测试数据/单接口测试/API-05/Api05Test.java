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
/** 兼容指定商品意向提交: POST /api/public/products/{id}/intents */
class Api05Test extends SingleApiTestSupport {
 @Test @DisplayName("API-05-01 有效客户正常提交")
 void case01() throws Exception {
  var p=product();var r=post("/api/public/products/"+p.getId()+"/intents",b,contact(),201);assertFalse(r.path("passcode").asText().isBlank());eq(r,"/position",1);var saved=intents.findAll().get(0);assertEquals(customer.getId(),saved.getCustomer().getId());assertEquals(p.getId(),saved.getProduct().getId());assertEquals("学校南门",saved.getMeetingLocation());assertEquals(p.getName(),saved.getProductNameSnapshot());assertEquals(0,p.getPrice().compareTo(saved.getPriceSnapshot()));
 }
 @Test @DisplayName("API-05-02 匿名提交被拒绝")
 void case02() throws Exception {
  var p=product();post("/api/public/products/"+p.getId()+"/intents",null,contact(),401);assertEquals(0,intents.count());unchanged(p);
 }
 @Test @DisplayName("API-05-03 仅卖家身份被拒绝")
 void case03() throws Exception {
  var p=product();post("/api/public/products/"+p.getId()+"/intents",s,contact(),401);assertEquals(0,intents.count());unchanged(p);
 }
 @Test @DisplayName("API-05-04 非法电话被拒绝")
 void case04() throws Exception {
  var p=product();var data=contact();data.put("phone","bad");post("/api/public/products/"+p.getId()+"/intents",b,data,400);assertEquals(0,intents.count());
 }
 @Test @DisplayName("API-05-05 冻结商品拒绝新意向")
 void case05() throws Exception {
  var p=product();state(p,ProductStatus.MANUALLY_FROZEN);var i=intent(p);post("/api/public/products/"+p.getId()+"/intents",b,contact(),409);assertEquals(1,intents.count());unchanged(i);
 }
}
