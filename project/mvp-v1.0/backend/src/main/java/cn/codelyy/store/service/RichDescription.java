package cn.codelyy.store.service;
import java.util.Set;
import java.util.regex.*;
public final class RichDescription {
 private RichDescription(){}
 private static final Set<String> TAGS=Set.of("p","br","strong","b","em","i","u","ul","ol","li","h2","h3","blockquote","div");
 private static final Pattern TOKENS=Pattern.compile("<[^>]*>");
 private static final Pattern TAG=Pattern.compile("<\\s*(/?)\\s*([a-zA-Z0-9]+)(?:\\s[^>]*)?/?>");
 private static final Pattern IMAGE=Pattern.compile("<img\\s+src=\"(/uploads/[a-zA-Z0-9_-]+\\.(?:png|jpg))\"(?:\\s+alt=\"商品描述图片\")?\\s*/?>",Pattern.CASE_INSENSITIVE);
 public static String clean(String input){
  if(input==null)return "";
  var out=new StringBuilder();var matcher=TOKENS.matcher(input);int end=0;
  while(matcher.find()){
   out.append(escape(input.substring(end,matcher.start())));
   var image=IMAGE.matcher(matcher.group());var tag=TAG.matcher(matcher.group());
   if(image.matches())out.append("<img src=\"").append(image.group(1)).append("\" alt=\"商品描述图片\">");
   else if(tag.matches() && TAGS.contains(tag.group(2).toLowerCase(java.util.Locale.ROOT)))
    out.append("<").append(tag.group(1)).append(tag.group(2).toLowerCase(java.util.Locale.ROOT)).append(">");
   end=matcher.end();
  }
  return out.append(escape(input.substring(end))).toString();
 }
 private static String escape(String s){return s.replaceAll("&(?!(?:amp|lt|gt|quot|#39);)","&amp;").replace("<","&lt;").replace(">","&gt;");}
}
