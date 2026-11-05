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
/** 重新上架商品: POST /api/seller/products/{id}/relist */
class Api26Test extends SingleApiTestSupport {
 @Test @DisplayName("API-26-01 有库存下架商品上架")
 void case01() throws Exception {
  var p=product("下架",ProductStatus.DELISTED,3);eq(post("/api/seller/products/"+p.getId()+"/relist",s,null,200),"/status","ON_SALE");var r=get("/api/public/products",null,200);eq(r,"/total",1);eq(r,"/items/0/id",p.getId());
 }
 @Test @DisplayName("API-26-02 库存为0不可上架")
 void case02() throws Exception {
  var p=product("无库存",ProductStatus.DELISTED,0);post("/api/seller/products/"+p.getId()+"/relist",s,null,409);unchanged(p);
 }
 @Test @DisplayName("API-26-03 未登录上架")
 void case03() throws Exception {
  var p=product("下架",ProductStatus.DELISTED,3);post("/api/seller/products/"+p.getId()+"/relist",null,null,401);unchanged(p);
 }
 @Test @DisplayName("API-26-04 商品不存在")
 void case04() throws Exception {
  post("/api/seller/products/"+MISSING+"/relist",s,null,404);
 }
 @Test @DisplayName("API-26-05 不恢复已结束意向")
 void case05() throws Exception {
  var p=product();var i=intent(p);post("/api/seller/products/"+p.getId()+"/delist",s,null,204);var ended=reload(i);p=reload(p);p.setStock(3);products.saveAndFlush(p);eq(post("/api/seller/products/"+p.getId()+"/relist",s,null,200),"/status","ON_SALE");unchanged(ended);assertEquals(IntentStatus.PRODUCT_DELISTED,reload(i).getStatus());assertNull(reload(i).getPasscode());get("/api/public/intents/"+i.getPasscode(),null,404);
 }
}
