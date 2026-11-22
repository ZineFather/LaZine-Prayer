package com.zine.prayer
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.ZoneId

private data class PrayerCity(val lat:Double,val lon:Double,val zone:String)

internal fun reschedulePrayerAlarms(context:Context){
 val prefs=context.getSharedPreferences("lenzin_prayer_settings",0)
 val city=when(prefs.getString("city","alex")){
  "cairo"->PrayerCity(30.0444,31.2357,"Africa/Cairo")
  "mecca"->PrayerCity(21.4225,39.8262,"Asia/Riyadh")
  else->PrayerCity(31.2001,29.9187,"Africa/Cairo")
 }
 val zone=ZoneId.of(city.zone)
 val method=prefs.getString("method","egypt")?:"egypt"
 val hanafi=prefs.getBoolean("hanafi",false)
 val prayerKeys=listOf("fajr","dhuhr","asr","maghrib","isha")
 val offsets=prayerKeys.associateWith{prefs.getInt("off_"+it,0)}
 val modes=prayerKeys.associateWith{prefs.getString("mode_"+it,"NOTIFICATION")?:"NOTIFICATION"}
 val today=LocalDate.now(zone)
 for(day in 0..3){
  val date=today.plusDays(day.toLong())
  val times=PrayerCalculator.calculate(date,city.lat,city.lon,zone,hanafi,offsets,method)
  scheduleAlarms(context,times,date,modes,city.zone)
 }
 val refreshAt=today.plusDays(1).atStartOfDay(zone).plusMinutes(2).toInstant().toEpochMilli()
 val intent=Intent(context,BootReceiver::class.java).setAction("com.zine.prayer.REFRESH")
 val pi=PendingIntent.getBroadcast(context,9012,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
 val alarm=context.getSystemService(AlarmManager::class.java)
 runCatching{
  if(Build.VERSION.SDK_INT>=31&&!alarm.canScheduleExactAlarms()) alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,refreshAt,pi)
  else alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,refreshAt,pi)
 }
}
class BootReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){reschedulePrayerAlarms(context)}
}
