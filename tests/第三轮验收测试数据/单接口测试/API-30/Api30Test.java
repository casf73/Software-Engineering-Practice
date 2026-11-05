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
/** 编辑分类: PUT /api/seller/categories/{id} */
class Api30Test extends SingleApiTestSupport {
 @Test @DisplayName("API-30-01 正常改名")
 void case01() throws Exception {
  var root=category("电子产品",null);var child=category("键盘",root);var p=product();p.setCategory(child);products.saveAndFlush(p);var r=put("/api/seller/categories/"+child.getId(),s,body("name","机械键盘","parentId",root.getId()),200);eq(r,"/name","机械键盘");eq(get("/api/public/products/"+p.getId(),null,200),"/categoryName","机械键盘");
 }
 @Test @DisplayName("API-30-02 拒绝循环")
 void case02() throws Exception {
  var root=category("电子产品",null);var child=category("键盘",root);put("/api/seller/categories/"+root.getId(),s,body("name","电子产品","parentId",child.getId()),400);eq(get("/api/public/categories",null,200),"/0/parentId",null);
 }
 @Test @DisplayName("API-30-03 拒绝三级结构")
 void case03() throws Exception {
  var root=category("电子产品",null);var child=category("键盘",root);var other=category("鼠标",null);put("/api/seller/categories/"+other.getId(),s,body("name","鼠标","parentId",child.getId()),400);eq(get("/api/public/categories",null,200),"/2/parentId",null);
 }
 @Test @DisplayName("API-30-04 同级重名修改")
 void case04() throws Exception {
  var root=category("电子产品",null);category("键盘",root);var other=category("鼠标",root);put("/api/seller/categories/"+other.getId(),s,body("name","键盘","parentId",root.getId()),409);assertEquals("鼠标",categories.findById(other.getId()).orElseThrow().getName());
 }
 @Test @DisplayName("API-30-05 分类不存在")
 void case05() throws Exception {
  put("/api/seller/categories/"+MISSING,s,body("name","键盘","parentId",null),404);
 }
}
