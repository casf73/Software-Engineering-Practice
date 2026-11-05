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
/** 删除分类: DELETE /api/seller/categories/{id} */
class Api31Test extends SingleApiTestSupport {
 @Test @DisplayName("API-31-01 删除空分类")
 void case01() throws Exception {
  var c=category("空分类",null);delete("/api/seller/categories/"+c.getId(),s,204);assertFalse(categories.existsById(c.getId()));eq(get("/api/public/categories",null,200),"",List.of());
 }
 @Test @DisplayName("API-31-02 被商品引用")
 void case02() throws Exception {
  var c=category("电子产品",null);var p=product();p.setCategory(c);products.saveAndFlush(p);delete("/api/seller/categories/"+c.getId(),s,409);assertTrue(categories.existsById(c.getId()));eq(get("/api/public/products/"+p.getId(),null,200),"/categoryId",c.getId());
 }
 @Test @DisplayName("API-31-03 被子分类引用")
 void case03() throws Exception {
  var c=category("电子产品",null);var child=category("键盘",c);delete("/api/seller/categories/"+c.getId(),s,409);assertEquals(2,categories.count());eq(get("/api/public/categories",null,200),"/1/parentId",c.getId());
 }
 @Test @DisplayName("API-31-04 分类不存在")
 void case04() throws Exception {
  delete("/api/seller/categories/"+MISSING,s,404);
 }
 @Test @DisplayName("API-31-05 未登录删除")
 void case05() throws Exception {
  var c=category("空分类",null);delete("/api/seller/categories/"+c.getId(),null,401);assertTrue(categories.existsById(c.getId()));
 }
}
