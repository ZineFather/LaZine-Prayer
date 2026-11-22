package com.zine.prayer
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.ZoneId
class BootReceiver:BroadcastReceiver(){
 override fun onReceive(context:Context,intent:Intent){
  val prefs=context.getSharedPreferences("lenzin_prayer_settings",0)
  val cityId=prefs.getString("city","alex")?:"alex"
  val city=when(cityId){"cairo"->CityData(30.0444,31.2357,"Africa/Cairo");"mecca"->CityData(21.4225,39.8262,"Asia/Riyadh");else->CityData(31.2001,29.9187,"Africa/Cairo")}
  val zone=ZoneId.of(city.zone);val method=prefs.getString("method","egypt")?:"egypt";val hanafi=prefs.getBoolean("hanafi",false)
  val keys=listOf("fajr","dhuhr","asr","maghrib","isha")
  val offsets=keys.associateWith{prefs.getInt("off_"+it,0)}
  val modes=keys.associateWith{prefs.getString("mode_"+it,"NOTIFICATION")?:"NOTIFICATION"}
  val today=LocalDate.now(zone)
  for(day in 0..2){val date=today.plusDays(day.toLong());val times=PrayerCalculator.calculate(date,city.lat,city.lon,zone,hanafi,offsets,method);scheduleAlarms(context,times,date,modes,city.zone)}
 }
 private data class CityData(val lat:Double,val lon:Double,val zone:String)
}