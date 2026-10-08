package ir.hamed.trainwatch;
import android.content.*;
import android.app.*;
import android.os.Build;
import android.content.pm.PackageManager;
import androidx.work.*;
import org.json.*;
public class WatchWorker extends Worker {
 public WatchWorker(Context c,WorkerParameters p){super(c,p);}
 public Result doWork(){
  Context c=getApplicationContext(); android.content.SharedPreferences prefs=c.getSharedPreferences("prefs",0);
  if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=PackageManager.PERMISSION_GRANTED)return Result.success();
  try {
   JSONArray watches=new JSONArray(prefs.getString("watches","[]")); boolean failed=false;
   for(int i=0;i<watches.length();i++) {
    JSONObject w=watches.getJSONObject(i); if(!w.optBoolean("active"))continue;
    try {
     JSONObject q=w.getJSONObject("query");
     // Date is ISO Gregorian, derived from Android's Persian calendar picker.
     if(java.time.LocalDate.parse(q.getString("date")).isBefore(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Tehran")))) {w.put("active",false);continue;}
     JSONArray trains=Api.call(c,q).getJSONArray("trains");
     for(int j=0;j<trains.length();j++) {
      JSONObject t=trains.getJSONObject(j);
      if(t.getString("id").equals(w.getString("train_id")) && Api.available(t,q.getInt("passengers"))) {
       String link=Api.purchase(t); NotificationManager nm=c.getSystemService(NotificationManager.class);
       nm.createNotificationChannel(new NotificationChannel("capacity","ظرفیت قطار",NotificationManager.IMPORTANCE_HIGH));
       int id=w.getString("key").hashCode();
       Intent intent=new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(link));
       PendingIntent pi=PendingIntent.getActivity(c,id,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
       nm.notify(id,new Notification.Builder(c,"capacity").setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("ظرفیت قطار باز شد").setContentText(t.getString("name")+" — "+q.getString("origin")+" به "+q.getString("destination"))
        .setContentIntent(pi).setAutoCancel(true).build());
       w.put("active",false); break;
      }
     }
    }catch(Exception e){failed=true;}
   }
   // Avoid overwriting UI changes made while requests were in flight.
   synchronized(WatchWorker.class){
    JSONArray current=new JSONArray(prefs.getString("watches","[]"));
    for(int i=0;i<current.length();i++)for(int j=0;j<watches.length();j++){
     JSONObject a=current.getJSONObject(i),b=watches.getJSONObject(j);
     if(a.getString("key").equals(b.getString("key")) && a.optLong("revision")==b.optLong("revision") && !b.optBoolean("active"))a.put("active",false);
    }
    prefs.edit().putString("watches",current.toString()).apply();
   }
   return failed?Result.retry():Result.success();
  }catch(Exception e){return Result.retry();}
 }
}
