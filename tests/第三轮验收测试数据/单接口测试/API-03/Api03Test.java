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
/** 分类列表查询: GET /api/public/categories */
class Api03Test extends SingleApiTestSupport {
 @Test @DisplayName("API-03-01 匿名读取分类")
 void case01() throws Exception {
  var root=category("电子产品",null);category("键盘",root);var r=get("/api/public/categories",null,200);assertTrue(r.isArray());assertEquals(2,r.size());
 }
 @Test @DisplayName("API-03-02 分类层级正确")
 void case02() throws Exception {
  var root=category("电子产品",null);var child=category("键盘",root);var r=get("/api/public/categories",null,200);eq(r,"/0/id",root.getId());eq(r,"/0/parentId",null);eq(r,"/1/id",child.getId());eq(r,"/1/parentId",root.getId());
 }
 @Test @DisplayName("API-03-03 空分类列表")
 void case03() throws Exception {
  eq(get("/api/public/categories",null,200),"",List.of());
 }
 @Test @DisplayName("API-03-04 中文名称保留")
 void case04() throws Exception {
  category("电子产品",null);category("USB键盘",null);var r=get("/api/public/categories",null,200);eq(r,"/0/name","电子产品");eq(r,"/1/name","USB键盘");
 }
 @Test @DisplayName("API-03-05 维护后读取最新数据")
 void case05() throws Exception {
  var root=category("电子产品",null);var child=category("键盘",root);var empty=category("空分类",null);put("/api/seller/categories/"+child.getId(),s,body("name","机械键盘","parentId",root.getId()),200);delete("/api/seller/categories/"+empty.getId(),s,204);var r=get("/api/public/categories",null,200);assertEquals(2,r.size());eq(r,"/1/name","机械键盘");assertFalse(r.toString().contains("空分类"));
 }
}
