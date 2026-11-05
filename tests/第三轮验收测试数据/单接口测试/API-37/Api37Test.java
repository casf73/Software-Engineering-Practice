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
/** 卖家客户历史查询: GET /api/seller/customers/{id}/orders */
class Api37Test extends SingleApiTestSupport {
 @Test @DisplayName("API-37-01 正常查询并隐藏口令码")
 void case01() throws Exception {
  var i=intent(product());var r=get("/api/seller/customers/"+customer.getId()+"/orders",s,200);eq(r,"/total",1);eq(r,"/items/0/id",i.getId());eq(r,"/items/0/productName","测试商品");eq(r,"/items/0/price",88.50);eq(r,"/items/0/meetingLocation","学校南门");eq(r,"/items/0/status","WAITING");eq(r,"/items/0/passcode",null);
 }
 @Test @DisplayName("API-37-02 不同客户历史隔离")
 void case02() throws Exception {
  var p=product();intent(p);var other=customer("buyer02");var i=intent(p,other,"客户乙");var r=get("/api/seller/customers/"+other.getId()+"/orders",s,200);eq(r,"/total",1);eq(r,"/items/0/id",i.getId());eq(r,"/items/0/name","客户乙");eq(r,"/items/0/passcode",null);
 }
 @Test @DisplayName("API-37-03 客户无历史记录")
 void case03() throws Exception {
  var r=get("/api/seller/customers/"+customer.getId()+"/orders",s,200);eq(r,"/total",0);eq(r,"/items",List.of());
 }
 @Test @DisplayName("API-37-04 客户不存在")
 void case04() throws Exception {
  get("/api/seller/customers/"+MISSING+"/orders",s,404);
 }
 @Test @DisplayName("API-37-05 仅客户身份访问")
 void case05() throws Exception {
  intent(product());get("/api/seller/customers/"+customer.getId()+"/orders",b,401);
 }
}
