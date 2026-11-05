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
/** 客户购买意向提交: POST /api/customer/products/{id}/intents */
class Api15Test extends SingleApiTestSupport {
 @Test @DisplayName("API-15-01 合法提交本次地点")
 void case01() throws Exception {
  var p=product();var r=post("/api/customer/products/"+p.getId()+"/intents",b,contact(),201);assertFalse(r.path("passcode").asText().isBlank());eq(r,"/position",1);var saved=intents.findAll().get(0);assertEquals(customer.getId(),saved.getCustomer().getId());assertEquals(p.getId(),saved.getProduct().getId());assertEquals("图书馆",saved.getMeetingLocation());assertEquals(p.getName(),saved.getProductNameSnapshot());assertEquals(0,p.getPrice().compareTo(saved.getPriceSnapshot()));
 }
 @Test @DisplayName("API-15-02 未登录提交")
 void case02() throws Exception {
  var p=product();post("/api/customer/products/"+p.getId()+"/intents",null,contact(),401);assertEquals(0,intents.count());unchanged(p);
 }
 @Test @DisplayName("API-15-03 仅卖家身份提交")
 void case03() throws Exception {
  var p=product();post("/api/customer/products/"+p.getId()+"/intents",s,contact(),401);assertEquals(0,intents.count());unchanged(p);
 }
 @Test @DisplayName("API-15-04 本次地点为空")
 void case04() throws Exception {
  var p=product();var data=contact();data.put("meetingLocation","");post("/api/customer/products/"+p.getId()+"/intents",b,data,400);assertEquals(0,intents.count());
 }
 @Test @DisplayName("API-15-05 本次地点超过上界")
 void case05() throws Exception {
  var p=product();var data=contact();data.put("meetingLocation","地".repeat(201));post("/api/customer/products/"+p.getId()+"/intents",b,data,400);assertEquals(0,intents.count());
 }
}
