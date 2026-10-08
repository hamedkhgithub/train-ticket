package ir.hamed.trainwatch;
import android.app.*; import android.os.*; import android.content.*; import android.graphics.Color;
import android.view.*; import android.widget.*; import org.json.*; import androidx.work.*;
import java.util.concurrent.*;
public class MainActivity extends Activity {
 LinearLayout root,results; EditText server,token,origin,destination,count; Button dateButton,search; String date=""; JSONObject query;
 final ExecutorService executor=Executors.newSingleThreadExecutor();
 android.content.SharedPreferences prefs;
 protected void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("prefs",0);
  ScrollView scroll=new ScrollView(this); root=new LinearLayout(this);root.setOrientation(1);root.setPadding(32,40,32,32);root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);root.setBackgroundColor(Color.rgb(243,246,250));scroll.addView(root);setContentView(scroll);
  text(root,"قطاربان",26);text(root,"نسخه اولیه • اتصال واقعی رجا هنوز آماده نیست",15);
  text(root,"پایش گوشی حدود هر ۱۵ دقیقه یا دیرتر است. اعلان فوری نیاز به سرویس سرور دارد.",14);
  server=field("آدرس HTTPS سرویس",prefs.getString("server",""));token=field("توکن سرویس شخصی",prefs.getString("token",""));token.setInputType(129);
  origin=field("مبدأ (نام یا کد مطابق سرویس)","تهران");destination=field("مقصد","مشهد");count=field("تعداد مسافر","1");count.setInputType(2);
  dateButton=button(root,"انتخاب تاریخ");dateButton.setOnClickListener(v->{android.icu.util.Calendar pc=android.icu.util.Calendar.getInstance(android.icu.util.TimeZone.getTimeZone("Asia/Tehran"),java.util.Locale.forLanguageTag("fa-IR-u-ca-persian"));
   LinearLayout box=new LinearLayout(this);box.setOrientation(1);NumberPicker y=new NumberPicker(this),m=new NumberPicker(this),d=new NumberPicker(this);
   y.setMinValue(pc.get(android.icu.util.Calendar.YEAR));y.setMaxValue(y.getMinValue()+1);m.setMinValue(1);m.setMaxValue(12);m.setValue(pc.get(android.icu.util.Calendar.MONTH)+1);d.setMinValue(1);d.setMaxValue(31);d.setValue(pc.get(android.icu.util.Calendar.DAY_OF_MONTH));box.addView(y);box.addView(m);box.addView(d);
   new AlertDialog.Builder(this).setTitle("سال / ماه / روز شمسی").setView(box).setPositiveButton("انتخاب",(dialog,which)->{
    try{android.icu.util.Calendar chosen=android.icu.util.Calendar.getInstance(android.icu.util.TimeZone.getTimeZone("Asia/Tehran"),java.util.Locale.forLanguageTag("fa-IR-u-ca-persian"));chosen.setLenient(false);chosen.clear();chosen.set(y.getValue(),m.getValue()-1,d.getValue(),12,0);long millis=chosen.getTimeInMillis();
     java.time.LocalDate ld=java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.of("Asia/Tehran")).toLocalDate();
     if(ld.isBefore(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Tehran"))))throw new Exception();
     date=ld.toString();dateButton.setText(y.getValue()+"/"+m.getValue()+"/"+d.getValue());
    }catch(Exception e){toast("تاریخ معتبر انتخاب کنید");}
   }).setNegativeButton("بستن",null).show();});
  search=button(root,"جست‌وجوی قطارها");search.setOnClickListener(v->search());
  Button manage=button(root,"هشدارهای من");manage.setOnClickListener(v->manage());
  results=new LinearLayout(this);results.setOrientation(1);root.addView(results);
  if(Build.VERSION.SDK_INT>=33)requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},1);
  Constraints constraints=new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
  WorkManager.getInstance(this).enqueueUniquePeriodicWork("capacity",ExistingPeriodicWorkPolicy.KEEP,new PeriodicWorkRequest.Builder(WatchWorker.class,15,TimeUnit.MINUTES).setConstraints(constraints).build());
 }
 EditText field(String hint,String value){EditText e=new EditText(this);e.setHint(hint);e.setText(value);root.addView(e);return e;}
 void text(LinearLayout parent,String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(11,35,66));t.setPadding(0,12,0,12);parent.addView(t);}
 Button button(LinearLayout parent,String s){Button b=new Button(this);b.setText(s);parent.addView(b);return b;}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 void search(){try{
  int n=Integer.parseInt(count.getText().toString());if(n<1 || n>6 || date.isEmpty() || origin.getText().toString().trim().isEmpty() || destination.getText().toString().trim().isEmpty() || origin.getText().toString().trim().equals(destination.getText().toString().trim()))throw new Exception();
  prefs.edit().putString("server",server.getText().toString().trim()).putString("token",token.getText().toString().trim()).apply();
  JSONObject q=new JSONObject().put("origin",origin.getText().toString().trim()).put("destination",destination.getText().toString().trim()).put("date",date).put("passengers",n);query=q;
  results.removeAllViews();search.setEnabled(false);
  executor.execute(()->{try{JSONObject response=Api.call(this,q);runOnUiThread(()->{search.setEnabled(true);try{render(response,q);}catch(Exception e){text(results,"پاسخ سرویس معتبر نیست",16);}});}catch(Exception e){runOnUiThread(()->{search.setEnabled(true);text(results,e.getMessage(),16);});}});
 }catch(Exception e){toast("مبدأ، مقصد، تاریخ و تعداد ۱ تا ۶ مسافر را بررسی کنید");}}
 void render(JSONObject response,JSONObject q)throws Exception{
  text(results,"آخرین دریافت: "+response.getString("checked_at"),13);JSONArray trains=response.getJSONArray("trains");if(trains.length()==0)text(results,"قطاری برای این جست‌وجو دریافت نشد",16);
  for(int i=0;i<trains.length();i++){
   JSONObject t=trains.getJSONObject(i);LinearLayout card=new LinearLayout(this);card.setOrientation(1);card.setPadding(18,18,18,28);results.addView(card);
   text(card,t.getString("name")+" • "+t.getString("departure"),19);
   text(card,t.getLong("price_toman")+" تومان • ظرفیت: "+t.optInt("seats",0),15);
   if(Api.available(t,q.getInt("passengers"))){String link=Api.purchase(t);button(card,"خرید در رجا").setOnClickListener(v->startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(link))));}
   else if(t.optString("status").equals("sold_out") || t.optString("status").equals("available")){
    button(card,"ظرفیت کافی نیست • خبرم کن").setOnClickListener(v->watch(t,q));
   }else text(card,"وضعیت ظرفیت نامشخص؛ هشدار قابل فعال‌سازی نیست",14);
  }
 }
 void watch(JSONObject t,JSONObject q){if(Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){toast("ابتدا اجازه اعلان را در تنظیمات برنامه فعال کنید");return;}
  try{synchronized(WatchWorker.class){JSONArray a=new JSONArray(prefs.getString("watches","[]"));String key=q.toString()+":"+t.getString("id");
   JSONObject w=new JSONObject().put("key",key).put("train_id",t.getString("id")).put("name",t.getString("name")).put("query",q).put("active",true).put("revision",System.currentTimeMillis());
   boolean exists=false;for(int i=0;i<a.length();i++)if(a.getJSONObject(i).getString("key").equals(key)){a.put(i,w);exists=true;break;}if(!exists)a.put(w);
   prefs.edit().putString("watches",a.toString()).apply();}toast("هشدار ثبت شد؛ بررسی دوره‌ای گوشی ممکن است تأخیر داشته باشد");}catch(Exception e){toast("ثبت نشد");}}
 void manage(){try{JSONArray a=new JSONArray(prefs.getString("watches","[]"));LinearLayout box=new LinearLayout(this);box.setOrientation(1);
  for(int i=0;i<a.length();i++){JSONObject w=a.getJSONObject(i);if(!w.optBoolean("active"))continue;String key=w.getString("key");
   button(box,w.getString("name")+" • "+w.getJSONObject("query").getString("date")+" — لغو هشدار").setOnClickListener(v->{try{synchronized(WatchWorker.class){JSONArray current=new JSONArray(prefs.getString("watches","[]"));for(int j=0;j<current.length();j++)if(current.getJSONObject(j).getString("key").equals(key))current.getJSONObject(j).put("active",false).put("revision",System.currentTimeMillis());prefs.edit().putString("watches",current.toString()).apply();}v.setEnabled(false);}catch(Exception e){toast("لغو نشد");}});
  }ScrollView s=new ScrollView(this);s.addView(box);new AlertDialog.Builder(this).setTitle("هشدارهای فعال").setView(s).setPositiveButton("بستن",null).show();}catch(Exception e){toast("خطا");}}
 protected void onDestroy(){executor.shutdown();super.onDestroy();}
}
