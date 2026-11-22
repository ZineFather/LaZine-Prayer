package com.zine.prayer
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.*
data class PrayerTimes(val fajr:Double,val sunrise:Double,val dhuhr:Double,val asr:Double,val maghrib:Double,val isha:Double) {
 fun entries()=listOf("fajr" to fajr,"sunrise" to sunrise,"dhuhr" to dhuhr,"asr" to asr,"maghrib" to maghrib,"isha" to isha)
}
object PrayerCalculator {
 fun calculate(date:LocalDate,lat:Double,lon:Double,zone:ZoneId,hanafi:Boolean=false,offsets:Map<String,Int> = emptyMap(),method:String="egypt"):PrayerTimes {
  val jd=date.toEpochDay().toDouble()+2440587.5; val t=(jd-2451545.0)/36525.0
  val l0=norm(280.46646+t*(36000.76983+0.0003032*t)); val m=357.52911+t*(35999.05029-0.0001537*t)
  val e=0.016708634-t*(0.000042037+0.0000001267*t)
  val c=sin(rad(m))*(1.914602-t*(0.004817+0.000014*t))+sin(rad(2*m))*(0.019993-0.000101*t)+sin(rad(3*m))*0.000289
  val omega=125.04-1934.136*t; val lambda=l0+c-0.00569-0.00478*sin(rad(omega))
  val sec=21.448-t*(46.815+t*(0.00059-t*0.001813)); val obliq=23.0+(26.0+sec/60.0)/60.0+0.00256*cos(rad(omega))
  val decl=asin(sin(rad(obliq))*sin(rad(lambda))); val y=tan(rad(obliq/2)).pow(2)
  val eq=4.0*Math.toDegrees(y*sin(2*rad(l0))-2*e*sin(rad(m))+4*e*y*sin(rad(m))*cos(2*rad(l0))-0.5*y*y*sin(4*rad(l0))-1.25*e*e*sin(2*rad(m)))
  val offset=date.atTime(12,0).atZone(zone).offset.totalSeconds/3600.0; val noon=12+offset-lon/15.0-eq/60.0; val rlat=rad(lat)
  fun ha(alt:Double):Double { val v=((sin(rad(alt))-sin(rlat)*sin(decl))/(cos(rlat)*cos(decl))).coerceIn(-1.0,1.0); return Math.toDegrees(acos(v))/15.0 }
  val asrAlt=Math.toDegrees(atan(1.0/((if(hanafi)2.0 else 1.0)+tan(abs(rlat-decl)))))
  fun adj(k:String,v:Double)=v+(offsets[k]?:0)/60.0
  val fajrAngle=when(method){"mwl"->18.0;"isna"->15.0;else->19.5}
  val ishaAngle=when(method){"mwl"->17.0;"isna"->15.0;else->17.5}
  return PrayerTimes(adj("fajr",noon-ha(-fajrAngle)),adj("sunrise",noon-ha(-0.833)),adj("dhuhr",noon+2.0/60),adj("asr",noon+ha(asrAlt)),adj("maghrib",noon+ha(-0.833)),adj("isha",noon+ha(-ishaAngle)))
 }
 fun format(v:Double,language:String="en"):String { val total=(((v*60).roundToInt()%1440)+1440)%1440;val h=total/60;val h12=if(h%12==0)12 else h%12;val suffix=if(language=="ar"){if(h<12)"ص" else "م"}else{if(h<12)"AM" else "PM"};return if(language=="ar")"%02d:%02d %s".format(java.util.Locale("ar"),h12,total%60,suffix) else "%02d:%02d %s".format(java.util.Locale.US,h12,total%60,suffix) }
 private fun norm(v:Double)=((v%360)+360)%360
 private fun rad(v:Double)=Math.toRadians(v)
}