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
/** 创建分类: POST /api/seller/categories */
class Api29Test extends SingleApiTestSupport {
 @Test @DisplayName("API-29-01 创建一级分类")
 void case01() throws Exception {
  var r=post("/api/seller/categories",s,body("name","电子产品","parentId",null),201);eq(r,"/name","电子产品");eq(r,"/parentId",null);assertEquals(1,categories.count());
 }
 @Test @DisplayName("API-29-02 创建二级分类")
 void case02() throws Exception {
  var root=category("电子产品",null);var r=post("/api/seller/categories",s,body("name","键盘","parentId",root.getId()),201);eq(r,"/name","键盘");eq(r,"/parentId",root.getId());
 }
 @Test @DisplayName("API-29-03 同级重名")
 void case03() throws Exception {
  var root=category("电子产品",null);category("键盘",root);post("/api/seller/categories",s,body("name","键盘","parentId",root.getId()),409);assertEquals(2,categories.count());
 }
 @Test @DisplayName("API-29-04 禁止三级分类")
 void case04() throws Exception {
  var root=category("电子产品",null);var child=category("键盘",root);post("/api/seller/categories",s,body("name","三级","parentId",child.getId()),400);assertEquals(2,categories.count());
 }
 @Test @DisplayName("API-29-05 未登录创建")
 void case05() throws Exception {
  post("/api/seller/categories",null,body("name","电子产品","parentId",null),401);assertEquals(0,categories.count());
 }
}
