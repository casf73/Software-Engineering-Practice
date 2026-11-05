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
/** 商品图片上传: POST /api/seller/products/image */
class Api28Test extends SingleApiTestSupport {
 @Test @DisplayName("API-28-01 合法JPG")
 void case01() throws Exception {
  byte[] bytes=picture("jpg");var r=upload(s,"image/jpeg",bytes,200);imageSaved(r,bytes);var data=productBody();data.put("images",List.of(r.path("url").asText()));eq(post("/api/seller/products",s,data,201),"/images",List.of(r.path("url").asText()));
 }
 @Test @DisplayName("API-28-02 合法PNG")
 void case02() throws Exception {
  byte[] bytes=picture("png");imageSaved(upload(s,"image/png",bytes,200),bytes);
 }
 @Test @DisplayName("API-28-03 非法TXT")
 void case03() throws Exception {
  var r=upload(s,"text/plain","this is text".getBytes(StandardCharsets.UTF_8),400);assertTrue(r.path("url").isMissingNode());
 }
 @Test @DisplayName("API-28-04 大小超过上界")
 void case04() throws Exception {
  byte[] bytes=Arrays.copyOf(picture("png"),5*1024*1024+1);var r=upload(s,"image/png",bytes,400);assertTrue(r.path("url").isMissingNode());
 }
 @Test @DisplayName("API-28-05 未登录上传")
 void case05() throws Exception {
  upload(null,"image/png",picture("png"),401);
 }
}
