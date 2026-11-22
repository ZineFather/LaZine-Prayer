package com.zine.prayer
import android.app.*
import android.content.*
class PrayerAlarmReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  val key=intent.getStringExtra("prayer")?:return
  val prefs=context.getSharedPreferences("lenzin_prayer_settings",0)
  val mode=prefs.getString("mode_"+key,"NOTIFICATION")?:"NOTIFICATION"
  if(mode=="OFF")return
  val ar=prefs.getString("lang","ar")=="ar"
  val names=if(ar)mapOf("fajr" to "الفجر","dhuhr" to "الظهر","asr" to "العصر","maghrib" to "المغرب","isha" to "العشاء") else mapOf("fajr" to "Fajr","dhuhr" to "Dhuhr","asr" to "Asr","maghrib" to "Maghrib","isha" to "Isha")
  val title=if(ar)"حان موعد صلاة ${names[key]?:key}" else "It is time for ${names[key]?:key}"
  val channel=when(mode){"ADHAN"->"prayer_adhan";"SILENT"->"prayer_silent";else->"prayer_alerts"}
  val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val n=Notification.Builder(context,channel).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(title).setContentText("Lenzin Prayer").setContentIntent(open).setAutoCancel(true).setCategory(Notification.CATEGORY_ALARM).build()
  context.getSystemService(NotificationManager::class.java).notify(key.hashCode(),n)
 }
}