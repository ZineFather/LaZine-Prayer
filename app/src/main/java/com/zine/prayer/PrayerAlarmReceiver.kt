package com.zine.prayer
import android.app.*
import android.content.*
import android.os.Build
class PrayerAlarmReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  val key=intent.getStringExtra("prayer")?:return
  if(intent.action=="com.zine.prayer.STOP_ADHAN"){
   context.stopService(Intent(context,AdhanPlaybackService::class.java))
   context.getSystemService(NotificationManager::class.java).cancel(8100)
   context.getSystemService(NotificationManager::class.java).cancel(key.hashCode())
   return
  }
  val prefs=context.getSharedPreferences("lenzin_prayer_settings",0)
  val mode=prefs.getString("mode_"+key,"ADHAN")?:"ADHAN"
  if(mode=="OFF")return
  val ar=prefs.getString("lang","ar")=="ar"
  val names=if(ar)mapOf("fajr" to "الفجر","dhuhr" to "الظهر","asr" to "العصر","maghrib" to "المغرب","isha" to "العشاء") else mapOf("fajr" to "Fajr","dhuhr" to "Dhuhr","asr" to "Asr","maghrib" to "Maghrib","isha" to "Isha")
  val title=if(ar)"حان موعد صلاة ${names[key]?:key}" else "It is time for ${names[key]?:key}"
  if(mode=="ADHAN"){
   val play=Intent(context,AdhanPlaybackService::class.java).putExtra("prayer",key)
   if(Build.VERSION.SDK_INT>=26)context.startForegroundService(play) else context.startService(play)
   return
  }
  val channel=when(mode){"SHORT"->"prayer_short_v2";"SILENT"->"prayer_silent_v2";else->"prayer_alerts_v2"}
  val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val stopIntent=Intent(context,PrayerAlarmReceiver::class.java).setAction("com.zine.prayer.STOP_ADHAN").putExtra("prayer",key)
  val stop=PendingIntent.getBroadcast(context,key.hashCode()+1,stopIntent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val stopLabel=if(ar)"إيقاف الأذان" else "Stop Adhan"
  val builder=if(Build.VERSION.SDK_INT>=26)Notification.Builder(context,channel) else Notification.Builder(context)
  val notification=builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
   .setContentTitle(title).setContentText(if(ar)"Lenzin Prayer • مواقيت الصلاة" else "Lenzin Prayer • Prayer time")
   .setContentIntent(open).addAction(android.R.drawable.ic_media_pause,stopLabel,stop)
   .setAutoCancel(false).setCategory(Notification.CATEGORY_ALARM).build()
  context.getSystemService(NotificationManager::class.java).notify(key.hashCode(),notification)
 }
}
