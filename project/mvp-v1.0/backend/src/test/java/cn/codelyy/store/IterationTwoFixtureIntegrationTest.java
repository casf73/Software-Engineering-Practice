package cn.codelyy.store;
import cn.codelyy.store.domain.SellerAccount;
import cn.codelyy.store.repository.*;
import cn.codelyy.store.service.PasswordHasher;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:iteration-two-a1;DB_CLOSE_DELAY=-1","spring.jpa.hibernate.ddl-auto=create-drop","app.upload-directory=target/iteration-two-a1-uploads"})
@AutoConfigureMockMvc
class IterationTwoFixtureIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper;
 @Autowired IntentRepository intents; @Autowired ProductRepository products;
 @Autowired CategoryRepository categories; @Autowired SellerAccountRepository accounts;
 @BeforeEach void reset(){
  intents.deleteAll();products.deleteAll();
  var all=categories.findAll();all.stream().filter(c->c.getParent()!=null).forEach(categories::delete);
  categories.deleteAll();
  SellerAccount account=accounts.findById(1L).orElseThrow();account.setPasswordHash(PasswordHasher.hash("123456"));account.setSessionVersion(account.getSessionVersion()+1);accounts.saveAndFlush(account);
 }
 static Stream<Path> fixtures() throws Exception {
  Path dir=Path.of("").toAbsolutePath();while(dir!=null){Path data=dir.resolve("tests/第二轮验收测试数据");
   if(Files.isDirectory(data)){try(var walk=Files.walk(data)){return walk.filter(p->p.getFileName().toString().equals("instance.json")).sorted().toList().stream();}}dir=dir.getParent();}
  throw new IllegalStateException("第二轮测试实例目录不存在，请运行 tests/create-iteration2-fixtures.py");
 }
 @ParameterizedTest(name="{index} {0}") @MethodSource("fixtures")
 void runFixture(Path file) throws Exception {
  JsonNode fixture=mapper.readTree(Files.readString(file,StandardCharsets.UTF_8));
  Map<String,JsonNode> variables=new HashMap<>();Map<String,MockHttpSession> sessions=new HashMap<>();
  int index=0;
  for(JsonNode step:fixture.path("steps")){
   index++;String label=fixture.path("id").asText()+" 第"+index+"步";
   String path=resolve(step.path("path"),variables).asText();String method=step.path("method").asText();
   MockHttpServletRequestBuilder request;
   if(method.equals("UPLOAD")){
    Path upload=file.getParent().resolve(step.path("file").asText()).normalize();assertTrue(upload.startsWith(file.getParent()));
    request=multipart(path).file(new MockMultipartFile("file",upload.getFileName().toString(),step.path("mime").asText(),Files.readAllBytes(upload)));
   } else request=request(org.springframework.http.HttpMethod.valueOf(method),path);
   if(step.has("session")){String name=step.path("session").asText();assertTrue(sessions.containsKey(name),label+" 会话不存在");request.session(sessions.get(name));}
   if(step.has("bodyFile")){
    Path data=file.getParent().resolve(step.path("bodyFile").asText()).normalize();assertTrue(data.startsWith(file.getParent()));
    JsonNode body=resolve(mapper.readTree(Files.readString(data,StandardCharsets.UTF_8)),variables);
    request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body));
   }
   MvcResult result=mvc.perform(request).andReturn();
   assertEquals(step.path("status").asInt(),result.getResponse().getStatus(),label+" "+result.getResponse().getContentAsString(StandardCharsets.UTF_8));
   if(step.has("saveSession"))sessions.put(step.path("saveSession").asText(),(MockHttpSession)result.getRequest().getSession(false));
   String response=result.getResponse().getContentAsString(StandardCharsets.UTF_8);
   JsonNode body=response.isBlank()?NullNode.instance:mapper.readTree(response);
   if(step.has("assertions")){
    var fields=step.path("assertions").fields();while(fields.hasNext()){var field=fields.next();assertEquals(resolve(field.getValue(),variables),body.at(field.getKey()),label+" "+field.getKey());}
   }
   if(step.has("capture")){
    var fields=step.path("capture").fields();while(fields.hasNext()){var field=fields.next();var value=body.at(field.getValue().asText());assertFalse(value.isMissingNode(),label);variables.put(field.getKey(),value);}
   }
  }
 }
 private JsonNode resolve(JsonNode value,Map<String,JsonNode> vars){
  if(value.isObject()){ObjectNode node=mapper.createObjectNode();value.fields().forEachRemaining(e->node.set(e.getKey(),resolve(e.getValue(),vars)));return node;}
  if(value.isArray()){ArrayNode node=mapper.createArrayNode();value.forEach(x->node.add(resolve(x,vars)));return node;}
  if(!value.isTextual())return value;
  String text=value.asText();Matcher m=Pattern.compile("\\$\\{([a-zA-Z0-9]+)\\}").matcher(text);StringBuilder out=new StringBuilder();
  while(m.find()){
   JsonNode v=vars.get(m.group(1));assertNotNull(v,"缺少捕获变量 "+m.group(1));
   if(m.start()==0&&m.end()==text.length())return v;
   m.appendReplacement(out,Matcher.quoteReplacement(v.asText()));
  }
  m.appendTail(out);return TextNode.valueOf(out.toString());
 }
}
