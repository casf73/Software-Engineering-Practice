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
/** 客户注册: POST /api/customer/register */
class Api10Test extends SingleApiTestSupport {
 @Test @DisplayName("API-10-01 合法注册自动登录")
 void case01() throws Exception {
  var data=registration();data.put("username","buyer01");var r=post("/api/customer/register",null,data,201);eq(r,"/authenticated",true);eq(r,"/customer/username","buyer01");eq(r,"/customer/phone","13800000001");eq(r,"/customer/defaultLocation","学校南门");noSecrets(r);assertTrue(customers.existsByUsernameKey("buyer01"));
 }
 @Test @DisplayName("API-10-02 用户名低于下界")
 void case02() throws Exception {
  var data=registration();data.put("username","ab");long before=customers.count();post("/api/customer/register",null,data,400);assertEquals(before,customers.count());
 }
 @Test @DisplayName("API-10-03 用户名超过上界")
 void case03() throws Exception {
  var data=registration();data.put("username","x".repeat(33));long before=customers.count();post("/api/customer/register",null,data,400);assertEquals(before,customers.count());
 }
 @Test @DisplayName("API-10-04 密码低于下界")
 void case04() throws Exception {
  var data=registration();data.put("password","a".repeat(7));long before=customers.count();post("/api/customer/register",null,data,400);assertEquals(before,customers.count());
 }
 @Test @DisplayName("API-10-05 大小写重复账号")
 void case05() throws Exception {
  customer("buyer01");var data=registration();data.put("username","BUYER01");long before=customers.count();post("/api/customer/register",null,data,409);assertEquals(before,customers.count());
 }
}
