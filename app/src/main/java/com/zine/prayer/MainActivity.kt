package com.zine.prayer
import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.*
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
private val Black=Color(0xFF050609); private val Panel=Color(0xFF111722); private val Blue=Color(0xFF83BFFF)
private const val PREF="lenzin_prayer_settings"; private const val BUG="https://t.me/ZineROMProject/9"; private const val DEV="https://t.me/ZineFather"
private data class City(val id:String,val ar:String,val en:String,val lat:Double,val lon:Double,val zone:String="Africa/Cairo")
private val cityList=listOf(City("alex","الإسكندرية","Alexandria",31.2001,29.9187),City("cairo","القاهرة","Cairo",30.0444,31.2357),City("mecca","مكة المكرمة","Makkah",21.4225,39.8262,"Asia/Riyadh"))
private val keys=listOf("fajr","dhuhr","asr","maghrib","isha")
private val arNames=mapOf("fajr" to "الفجر","sunrise" to "الشروق","dhuhr" to "الظهر","asr" to "العصر","maghrib" to "المغرب","isha" to "العشاء")
private val enNames=mapOf("fajr" to "Fajr","sunrise" to "Sunrise","dhuhr" to "Dhuhr","asr" to "Asr","maghrib" to "Maghrib","isha" to "Isha")
class MainActivity:ComponentActivity(){
 private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); createChannel(this)
  if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
  setContent { MaterialTheme(colorScheme=darkColorScheme(primary=Blue,background=Black,surface=Panel,onSurface=Color.White,onBackground=Color.White)){ App { url->runCatching{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) } } } }
 }
}
@Composable private fun App(open:(String)->Unit){
 val ctx=androidx.compose.ui.platform.LocalContext.current; val pref=remember{ctx.getSharedPreferences(PREF,0)}
 var page by remember{mutableStateOf(0)}; var lang by remember{mutableStateOf(pref.getString("lang","ar")?:"ar")}
 var cityId by remember{mutableStateOf(pref.getString("city","alex")?:"alex")}; var method by remember{mutableStateOf(pref.getString("method","egypt")?:"egypt")}; var hanafi by remember{mutableStateOf(pref.getBoolean("hanafi",false))}
 var offsets by remember{mutableStateOf(keys.associateWith{pref.getInt("off_"+it,0)})}
 var modes by remember{mutableStateOf(keys.associateWith{pref.getString("mode_"+it,"NOTIFICATION")?:"NOTIFICATION"})}
 var tick by remember{mutableLongStateOf(System.currentTimeMillis())}
 LaunchedEffect(Unit){while(true){tick=System.currentTimeMillis();delay(1000)}}
 LaunchedEffect(lang,cityId,method,hanafi,offsets,modes){val e=pref.edit().putString("lang",lang).putString("city",cityId).putString("method",method).putBoolean("hanafi",hanafi);offsets.forEach{(k,v)->e.putInt("off_"+k,v)};modes.forEach{(k,v)->e.putString("mode_"+k,v)};e.apply()}
 val city=cityList.first{it.id==cityId}; val zone=ZoneId.of(city.zone); val date=Instant.ofEpochMilli(tick).atZone(zone).toLocalDate()
 val times=remember(date,city,hanafi,offsets,method){PrayerCalculator.calculate(date,city.lat,city.lon,zone,hanafi,offsets,method)}
 val clock=Instant.ofEpochMilli(tick).atZone(zone); val mins=clock.hour*60.0+clock.minute+clock.second/60.0
 val next=times.entries().filter{it.first!="sunrise"}.firstOrNull{it.second>mins}?:("fajr" to times.fajr+1440)
 val remain=((next.second-mins)*60).toLong().coerceAtLeast(0); val hijri=runCatching{HijrahDate.from(date).toString()}.getOrDefault("")
 LaunchedEffect(times,date,modes,city,hanafi,offsets,method){scheduleAlarms(ctx,times,date,modes,city.zone);val tomorrow=date.plusDays(1);val nextTimes=PrayerCalculator.calculate(tomorrow,city.lat,city.lon,zone,hanafi,offsets,method);scheduleAlarms(ctx,nextTimes,tomorrow,modes,city.zone)}
 Column(Modifier.fillMaxSize().background(Black)){
  Row(Modifier.fillMaxWidth().padding(18.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Column{Text("Lenzin Prayer",fontSize=24.sp,fontWeight=FontWeight.Bold);Text(if(lang=="ar")"مواقيت الصلاة بدون إنترنت" else "Offline prayer times",color=Color.Gray,fontSize=11.sp)}
   Box(Modifier.size(42.dp).clip(CircleShape).background(Panel),contentAlignment=Alignment.Center){Text("L",color=Blue,fontSize=24.sp,fontWeight=FontWeight.Black)}
  }
  when(page){
   0->Home(lang,city,date,hijri,times,next.first,remain)
   1->SettingsScreen(lang,{lang=it},cityId,{cityId=it},method,{method=it},hanafi,{hanafi=it},offsets,{offsets=it},modes,{modes=it},
    {if(Build.VERSION.SDK_INT>=31)runCatching{ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))};if(Build.VERSION.SDK_INT>=33)runCatching{ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,ctx.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}},
    {page=2},{open(BUG)})
   else->Developer(lang,{page=1},{open(DEV)},{open(BUG)})
  }
  Spacer(Modifier.weight(1f))
  NavigationBar(containerColor=Panel){NavigationBarItem(page==0,{page=0},icon={Text("⌂")},label={Text(if(lang=="ar")"الرئيسية" else "Home")});NavigationBarItem(page!=0,{page=1},icon={Text("⚙")},label={Text(if(lang=="ar")"الإعدادات" else "Settings")})}
 }
}
@Composable private fun Home(lang:String,city:City,date:LocalDate,hijri:String,t:PrayerTimes,next:String,left:Long){
 val ar=lang=="ar"; Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF101D2E)),shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()){
   Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){
    Text(if(ar)city.ar else city.en,color=Blue);Text(if(ar)"مواقيت الصلاة" else "Prayer times",fontSize=25.sp,fontWeight=FontWeight.Bold)
    Text(date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",if(ar)Locale("ar") else Locale.ENGLISH)),color=Color.LightGray,fontSize=12.sp)
    Text(if(ar)"التاريخ الهجري: "+hijri else "Hijri: "+hijri,color=Color.Gray,fontSize=11.sp)
    Spacer(Modifier.height(10.dp));Text(if(ar)"الصلاة القادمة: "+(arNames[next]?:next) else "Next prayer: "+(enNames[next]?:next),color=Blue)
    Text("%02d:%02d:%02d".format(left/3600,(left%3600)/60,left%60),fontSize=34.sp,fontWeight=FontWeight.Bold)
    Text(if(ar)"الوقت المتبقي" else "Remaining",color=Color.Gray,fontSize=11.sp)
   }
  }
  Text(if(ar)"مواقيت اليوم" else "TODAY",color=Blue,fontSize=11.sp,fontWeight=FontWeight.Bold)
  t.entries().forEach{(k,v)->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if(k==next)Color(0xFF14243A) else Panel).padding(17.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(if(ar)arNames[k]?:k else enNames[k]?:k,fontWeight=if(k==next)FontWeight.Bold else FontWeight.Normal);Text(PrayerCalculator.format(v,lang),color=Blue,fontWeight=FontWeight.Bold)}}
  Text(if(ar)"الحساب محلي وفق زوايا الهيئة المصرية للمساحة: الفجر 19.5° والعشاء 17.5°." else "Local calculation: Egyptian convention (Fajr 19.5°, Isha 17.5°).",color=Color.Gray,fontSize=10.sp)
 }
}
@Composable private fun SettingsScreen(lang:String,setLang:(String)->Unit,city:String,setCity:(String)->Unit,method:String,setMethod:(String)->Unit,hanafi:Boolean,setHanafi:(Boolean)->Unit,offs:Map<String,Int>,setOffs:(Map<String,Int>)->Unit,modes:Map<String,String>,setModes:(Map<String,String>)->Unit,permissions:()->Unit,developer:()->Unit,bug:()->Unit){
 val ar=lang=="ar"; Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text(if(ar)"الإعدادات" else "Settings",fontSize=26.sp,fontWeight=FontWeight.Bold)
  Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(14.dp)){Text(if(ar)"اللغة" else "Language");Row{FilterChip(lang=="ar",{setLang("ar")},{Text("العربية")});Spacer(Modifier.width(8.dp));FilterChip(lang=="en",{setLang("en")},{Text("English")})}}}
  Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(14.dp)){Text(if(ar)"المدينة" else "City");cityList.forEach{c->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(city==c.id,{setCity(c.id)});Text(if(ar)c.ar else c.en)}}}}
  Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(14.dp)){Text(if(ar)"طريقة حساب المواقيت" else "Calculation method");listOf("egypt" to (if(ar)"الهيئة المصرية" else "Egyptian Survey"),"mwl" to "Muslim World League","isna" to "ISNA").forEach{(id,label)->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(method==id,{setMethod(id)});Text(label)}}}}
  Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(14.dp)){Text(if(ar)"حساب العصر" else "Asr method");Row(verticalAlignment=Alignment.CenterVertically){RadioButton(!hanafi,{setHanafi(false)});Text(if(ar)"قياسي" else "Standard")};Row(verticalAlignment=Alignment.CenterVertically){RadioButton(hanafi,{setHanafi(true)});Text(if(ar)"حنفي" else "Hanafi")}}}
  Text(if(ar)"تعديل المواقيت بالدقائق" else "Minute adjustments",color=Blue,fontWeight=FontWeight.Bold)
  keys.forEach{k->Card(colors=CardDefaults.cardColors(containerColor=Panel)){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(if(ar)arNames[k]!! else enNames[k]!!);TextButton(onClick={setOffs(offs+(k to ((offs[k]?:0)-1)))}){Text("−")};Text((offs[k]?:0).toString(),color=Blue);TextButton(onClick={setOffs(offs+(k to ((offs[k]?:0)+1)))}){Text("+")}}}}
  Text(if(ar)"تنبيه كل صلاة" else "Prayer notifications",color=Blue,fontWeight=FontWeight.Bold)
  keys.forEach{k->var expanded by remember(k){mutableStateOf(false)};Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(12.dp)){Text(if(ar)arNames[k]!! else enNames[k]!!);Box{OutlinedButton(onClick={expanded=true}){Text(modes[k]?: "NOTIFICATION")};DropdownMenu(expanded,{expanded=false}){listOf("ADHAN","NOTIFICATION","SILENT","OFF").forEach{v->DropdownMenuItem(text={Text(v)},onClick={setModes(modes+(k to v));expanded=false})}}};if(modes[k]=="ADHAN")Text(if(ar)"تنبيه النظام حاليًا؛ صوت أذان مخصص غير مرفق بعد." else "System alert tone for now; custom adhan audio is not bundled.",color=Color.Gray,fontSize=10.sp)}}}
  Button(onClick=permissions,modifier=Modifier.fillMaxWidth()){Text(if(ar)"إدارة أذونات الإشعارات والمنبهات" else "Notification & alarm permissions")}
  OutlinedButton(onClick=developer,modifier=Modifier.fillMaxWidth()){Text(if(ar)"About Developer • عن المطور" else "About Developer")}
  OutlinedButton(onClick=bug,modifier=Modifier.fillMaxWidth()){Text(if(ar)"Report a Bug • الإبلاغ عن خطأ" else "Report a bug")}
 }
}
@Composable private fun Developer(lang:String,back:()->Unit,telegram:()->Unit,bug:()->Unit){
 val ar=lang=="ar";Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(14.dp)){
  TextButton(onClick=back){Text(if(ar)"رجوع" else "Back")};Box(Modifier.size(124.dp).clip(CircleShape).background(Color(0xFF14243A)),contentAlignment=Alignment.Center){Text("ZF",fontSize=42.sp,color=Blue,fontWeight=FontWeight.Bold)}
  Text("ZineFather",fontSize=28.sp,fontWeight=FontWeight.Bold);Text("@ZineFather",color=Blue)
  Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(18.dp)){Text(if(ar)"عن المطور" else "About Developer",color=Blue,fontWeight=FontWeight.Bold);Text(if(ar)"مطور رومات أندرويد ومؤسس مشروع ZineROM، يعمل على تطوير برمجيات أجهزة Samsung والرومات المخصصة وأدوات أندرويد والمواقع والتطبيقات، ويشارك في مشاريع ومجتمعات تقنية مستقلة." else "Android ROM developer and creator of ZineROM, focused on Samsung software, custom ROMs, Android tools, websites and applications, and independent developer communities.")}}
  Button(onClick=telegram,modifier=Modifier.fillMaxWidth()){Text("Telegram @ZineFather")};OutlinedButton(onClick=bug,modifier=Modifier.fillMaxWidth()){Text(if(ar)"الإبلاغ عن مشكلة" else "Report a bug")}
 }
}
private fun createChannel(c:Context){if(Build.VERSION.SDK_INT>=26){val nm=c.getSystemService(NotificationManager::class.java)
 val alarm=NotificationChannel("prayer_adhan","Adhan alerts",NotificationManager.IMPORTANCE_HIGH)
 alarm.description="Alarm sound for prayer time";alarm.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).build())
 val normal=NotificationChannel("prayer_alerts","Prayer notifications",NotificationManager.IMPORTANCE_HIGH)
 val silent=NotificationChannel("prayer_silent","Silent prayer notifications",NotificationManager.IMPORTANCE_LOW);silent.setSound(null,null);silent.enableVibration(false)
 nm.createNotificationChannel(alarm);nm.createNotificationChannel(normal);nm.createNotificationChannel(silent)}}
internal fun scheduleAlarms(c:Context,t:PrayerTimes,d:LocalDate,modes:Map<String,String>,zoneName:String){val am=c.getSystemService(AlarmManager::class.java);val zone=ZoneId.of(zoneName);val now=System.currentTimeMillis();t.entries().filter{it.first in keys}.forEach{(k,v)->if(modes[k]=="OFF")return@forEach;val at=d.atStartOfDay(zone).toEpochSecond()*1000+(v*3600000.0).toLong();if(at<=now)return@forEach;val i=Intent(c,PrayerAlarmReceiver::class.java).putExtra("prayer",k);val pi=PendingIntent.getBroadcast(c,d.toEpochDay().toInt()*7+k.hashCode(),i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);runCatching{if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi) else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)}}}
