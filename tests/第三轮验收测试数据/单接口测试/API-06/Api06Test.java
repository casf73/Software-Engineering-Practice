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
/** 兼容当前商品意向提交: POST /api/public/intents */
class Api06Test extends SingleApiTestSupport {
 @Test @DisplayName("API-06-01 唯一商品正常提交")
 void case01() throws Exception {
  var p=product();var r=post("/api/public/intents",b,contact(),201);assertFalse(r.path("passcode").asText().isBlank());eq(r,"/position",1);var saved=intents.findAll().get(0);assertEquals(customer.getId(),saved.getCustomer().getId());assertEquals(p.getId(),saved.getProduct().getId());assertEquals("学校南门",saved.getMeetingLocation());assertEquals(p.getName(),saved.getProductNameSnapshot());assertEquals(0,p.getPrice().compareTo(saved.getPriceSnapshot()));
 }
 @Test @DisplayName("API-06-02 多商品上下文冲突")
 void case02() throws Exception {
  var p=product();var q=product();post("/api/public/intents",b,contact(),409);assertEquals(0,intents.count());unchanged(p);unchanged(q);
 }
 @Test @DisplayName("API-06-03 匿名提交被拒绝")
 void case03() throws Exception {
  var p=product();post("/api/public/intents",null,contact(),401);assertEquals(0,intents.count());unchanged(p);
 }
 @Test @DisplayName("API-06-04 仅卖家身份被拒绝")
 void case04() throws Exception {
  var p=product();post("/api/public/intents",s,contact(),401);assertEquals(0,intents.count());unchanged(p);
 }
 @Test @DisplayName("API-06-05 非法电话被拒绝")
 void case05() throws Exception {
  var p=product();var data=contact();data.put("phone","bad");post("/api/public/intents",b,data,400);assertEquals(0,intents.count());
 }
}
