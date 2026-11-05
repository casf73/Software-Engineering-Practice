package cn.codelyy.store.singleapi;

import cn.codelyy.store.domain.*;
import cn.codelyy.store.repository.*;
import cn.codelyy.store.service.*;
import cn.codelyy.store.config.SellerAuthInterceptor;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import java.math.BigDecimal;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/** HTTP requests use the real Spring MVC stack; only preconditions are seeded directly. */
@SpringBootTest(classes=cn.codelyy.store.StoreApplication.class, properties={
 "spring.datasource.url=jdbc:h2:mem:single-api-215;DB_CLOSE_DELAY=-1",
 "spring.jpa.hibernate.ddl-auto=create-drop",
 "app.upload-directory=target/single-api-uploads",
 "spring.jpa.show-sql=false", "logging.level.org.hibernate.SQL=WARN"
})
@AutoConfigureMockMvc
public abstract class SingleApiTestSupport {
 @Autowired protected MockMvc mvc;
 @Autowired protected ObjectMapper mapper;
 @Autowired protected ProductRepository products;
 @Autowired protected IntentRepository intents;
 @Autowired protected CustomerRepository customers;
 @Autowired protected CategoryRepository categories;
 @Autowired protected SellerAccountRepository accounts;
 private static final String SELLER_HASH=PasswordHasher.hash("123456");
 private static final String BUYER_HASH=PasswordHasher.hash("Buyer1234");
 protected static final long MISSING=Long.MAX_VALUE;
 protected MockHttpSession s,b;
 protected Customer customer;
 private int sequence;
 private final List<Map<String,Object>> requests=new ArrayList<>();
 private String caseId;
 @RegisterExtension final TestWatcher executionRecord=new TestWatcher(){
  public void testSuccessful(ExtensionContext c){writeRecord(c,"通过",null);}
  public void testFailed(ExtensionContext c,Throwable e){writeRecord(c,"失败",e);}
  public void testAborted(ExtensionContext c,Throwable e){writeRecord(c,"中止",e);}
 };
 private void writeRecord(ExtensionContext context,String result,Throwable error){
  try{
   String id=context.getDisplayName().split(" ",2)[0];
   Path root=Path.of(System.getProperty("single.api.results","target/single-api-results"));
   Files.createDirectories(root);
   Map<String,Object> record=new LinkedHashMap<>();
   record.put("id",id);record.put("name",context.getDisplayName());
   record.put("executedAt",java.time.OffsetDateTime.now().toString());
   record.put("result",result);record.put("requests",requests);
   if(error!=null)record.put("error",error.toString());
   Files.writeString(root.resolve(id+".json"),mapper.writerWithDefaultPrettyPrinter().writeValueAsString(record),StandardCharsets.UTF_8);
  }catch(Exception e){throw new IllegalStateException("无法写入单接口执行记录",e);}
 }
 @BeforeEach void reset(TestInfo info){
  requests.clear();caseId=info.getDisplayName().split(" ",2)[0];sequence=0;
  intents.deleteAll(); products.deleteAll(); customers.deleteAll();
  categories.findAll().stream().filter(c->c.getParent()!=null).forEach(categories::delete);
  categories.deleteAll();
  var seller=accounts.findById(1L).orElseThrow();
  seller.setPasswordHash(SELLER_HASH);seller.setSessionVersion(seller.getSessionVersion()+1);accounts.saveAndFlush(seller);
  s=new MockHttpSession();s.setAttribute(SellerAuthInterceptor.SESSION_KEY,seller.getSessionVersion());
  if(!caseId.startsWith("API-10-")){customer=customer("buyer01");b=buyer(customer);}
 }
 protected Customer customer(String username){
  Customer c=new Customer();c.setUsername(username);c.setUsernameKey(username.toLowerCase(Locale.ROOT));
  c.setPasswordHash(BUYER_HASH);c.setPhone("13800000001");c.setDefaultLocation("学校南门");
  return customers.saveAndFlush(c);
 }
 protected MockHttpSession buyer(Customer c){var session=new MockHttpSession();session.setAttribute(CustomerService.SESSION_KEY,c.getId());return session;}
 protected MockHttpSession dual(){b.setAttribute(SellerAuthInterceptor.SESSION_KEY,accounts.findById(1L).orElseThrow().getSessionVersion());return b;}
 protected Category category(String name,Category parent){Category c=new Category();c.setName(name);c.setParent(parent);return categories.saveAndFlush(c);}
 protected Product product(){return product("测试商品",ProductStatus.ON_SALE,2);}
 protected Product product(String name,ProductStatus state,int stock){
  Product p=new Product();p.setName(name);p.setPrice(new BigDecimal("88.50"));p.setDescription("<p>商品说明</p>");
  p.setStock(stock);p.setStatus(state);p.setImageUrls("");return products.saveAndFlush(p);
 }
 protected PurchaseIntent intent(Product p){return intent(p,customer,"联系人");}
 protected PurchaseIntent intent(Product p,Customer owner,String name){
  PurchaseIntent i=new PurchaseIntent();i.setProduct(p);i.setCustomer(owner);i.setName(name);i.setPhone("13800000001");
  i.setProductNameSnapshot(p.getName());i.setPriceSnapshot(p.getPrice());i.setMeetingLocation("学校南门");
  i.setStatus(IntentStatus.WAITING);i.setPasscode("TESTCODE"+(++sequence));
  // Compare persisted values: H2 timestamps have microsecond precision.
  intents.saveAndFlush(i);return intents.findById(i.getId()).orElseThrow();
 }
 protected void state(Product p,ProductStatus value){p.setStatus(value);products.saveAndFlush(p);}
 protected Product reload(Product p){return products.findById(p.getId()).orElseThrow();}
 protected PurchaseIntent reload(PurchaseIntent i){return intents.findById(i.getId()).orElseThrow();}
 protected void trading(Product p,PurchaseIntent i){state(p,ProductStatus.IN_TRADE);i.setStatus(IntentStatus.IN_TRADE);intents.saveAndFlush(i);}
 protected Map<String,Object> body(Object... pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
 protected Map<String,Object> productBody(){return body("name","机械键盘","price",88.50,"stock",3,"description","<p>商品说明</p>","images",List.of(),"categoryId",null);}
 protected Map<String,Object> registration(){return body("username","newbuyer","password","Buyer1234","phone","13800000001","defaultLocation","学校南门");}
 protected Map<String,Object> contact(){return body("name","联系人","phone","13900000001","meetingLocation","图书馆");}
 protected Map<String,Object> credentials(String username,String password){return body("username",username,"password",password);}
 protected JsonNode call(String method,String path,MockHttpSession session,Object body,int status)throws Exception{
  var request=request(org.springframework.http.HttpMethod.valueOf(method),java.net.URI.create(path));
  if(session!=null)request.session(session);
  if(body!=null)request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsBytes(body));
  return perform(request,method,path,status);
 }
 protected JsonNode perform(MockHttpServletRequestBuilder request,String method,String path,int status)throws Exception{
  MvcResult result=mvc.perform(request).andReturn();
  String text=result.getResponse().getContentAsString(StandardCharsets.UTF_8);
  requests.add(body("method",method,"path",path,"expectedStatus",status,"actualStatus",result.getResponse().getStatus(),"response",text));
  assertEquals(status,result.getResponse().getStatus(),caseId+" "+method+" "+path+" "+text);
  if(status==204)assertEquals("",text,"204响应不应有正文");
  return text.isBlank()?NullNode.instance:mapper.readTree(text);
 }
 protected JsonNode get(String path,MockHttpSession session,int status)throws Exception{return call("GET",path,session,null,status);}
 protected JsonNode post(String path,MockHttpSession session,Object value,int status)throws Exception{return call("POST",path,session,value,status);}
 protected JsonNode put(String path,MockHttpSession session,Object value,int status)throws Exception{return call("PUT",path,session,value,status);}
 protected JsonNode delete(String path,MockHttpSession session,int status)throws Exception{return call("DELETE",path,session,null,status);}
 protected void eq(JsonNode node,String pointer,Object value){
  JsonNode expected=mapper.valueToTree(value),actual=node.at(pointer);
  if(expected.isNumber()&&actual.isNumber())assertEquals(0,expected.decimalValue().compareTo(actual.decimalValue()),caseId+" "+pointer);
  else assertEquals(expected,actual,caseId+" "+pointer);
 }
 protected void absent(JsonNode node,String... pointers){for(String p:pointers)assertTrue(node.at(p).isMissingNode()||node.at(p).isNull(),"敏感字段 "+p);}
 protected void noSecrets(JsonNode node){absent(node,"/password","/passwordHash","/customer/password","/customer/passwordHash");}
 protected void unchanged(Product p){var now=reload(p);assertEquals(p.getName(),now.getName());assertEquals(p.getPrice(),now.getPrice());assertEquals(p.getStock(),now.getStock());assertEquals(p.getStatus(),now.getStatus());}
 protected void unchanged(PurchaseIntent i){var now=reload(i);assertEquals(i.getName(),now.getName());assertEquals(i.getPhone(),now.getPhone());assertEquals(i.getStatus(),now.getStatus());assertEquals(i.getPasscode(),now.getPasscode());assertEquals(i.getQueueAt(),now.getQueueAt());}
 protected void loggedOut(MockHttpSession session,String role)throws Exception{eq(get("/api/"+role+"/session",session,200),"/authenticated",false);}
 protected byte[] picture(String type)throws Exception{var buffer=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB),type,buffer);return buffer.toByteArray();}
 protected JsonNode upload(MockHttpSession session,String mime,byte[] bytes,int status)throws Exception{
  var request=multipart("/api/seller/products/image").file(new MockMultipartFile("file","sample"+(mime.equals("image/png")?".png":".jpg"),mime,bytes));
  if(session!=null)request.session(session);
  return perform(request,"POST","/api/seller/products/image",status);
 }
 protected List<String> images(int count)throws Exception{List<String> urls=new ArrayList<>();for(int i=0;i<count;i++)urls.add(upload(s,"image/png",picture("png"),200).path("url").asText());return urls;}
 protected void imageSaved(JsonNode response,byte[] expected)throws Exception{String url=response.path("url").asText();assertTrue(url.matches("/uploads/[a-zA-Z0-9_-]+\\.(jpg|png)"));assertArrayEquals(expected,Files.readAllBytes(Path.of("target/single-api-uploads").resolve(url.substring("/uploads/".length()))));}
}
