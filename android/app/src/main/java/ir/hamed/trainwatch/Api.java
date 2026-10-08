package ir.hamed.trainwatch;
import android.content.Context;
import org.json.*;
import java.net.*;
import java.io.*;
class Api {
 static JSONObject call(Context c, JSONObject query) throws Exception {
  String base=c.getSharedPreferences("prefs",0).getString("server","");
  if(!base.startsWith("https://")) throw new IOException("آدرس HTTPS سرویس را وارد کنید");
  StringBuilder url=new StringBuilder(base.replaceAll("/+$","")+"/search?");
  for(String key:new String[]{"origin","destination","date","passengers"}) url.append(key).append("=").append(URLEncoder.encode(query.getString(key),"UTF-8")).append("&");
  HttpURLConnection h=(HttpURLConnection)new URL(url.toString()).openConnection();
  h.setConnectTimeout(15000); h.setReadTimeout(20000); h.setInstanceFollowRedirects(false);
  String token=c.getSharedPreferences("prefs",0).getString("token","");
  h.setRequestProperty("Authorization","Bearer "+token);
  try {
   if(h.getResponseCode()!=200) throw new IOException("سرویس پاسخ نداد: "+h.getResponseCode());
   ByteArrayOutputStream b=new ByteArrayOutputStream(); byte[] buf=new byte[4096]; int n;
   try(InputStream s=h.getInputStream()){while((n=s.read(buf))!=-1){b.write(buf,0,n); if(b.size()>1000000) throw new IOException("پاسخ نامعتبر");}}
   JSONObject result=new JSONObject(b.toString("UTF-8"));
   if(result.optBoolean("demo",true)) throw new IOException("سرویس در حالت آزمایشی است؛ پایش واقعی فعال نمی‌شود");
   return result;
  } finally { h.disconnect(); }
 }
 static boolean available(JSONObject t,int passengers){return t.optString("status").equals("available") && t.optInt("seats",0)>=passengers;}
 static String purchase(JSONObject t) throws Exception {
  URI u=new URI(t.getString("purchase_url")); String host=u.getHost();
  if(!"https".equals(u.getScheme()) || host==null || !(host.equals("raja.ir") || host.endsWith(".raja.ir"))) throw new IOException("لینک خرید معتبر رجا دریافت نشد");
  return u.toString();
 }
}
