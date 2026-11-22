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
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
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
private data class AdhanVoice(val id:String,val ar:String,val en:String,val note:String)
private val adhanVoices=listOf(AdhanVoice("adam","الأذان الأساسي","Original Adhan","Adam-synagda • CC0"),AdhanVoice("sabah","صباح فخري","Sabah Fakhri","Public-domain recording • 1985"),AdhanVoice("aishatu","أذان بصوت Aishatu98","Aishatu98 Adhan","CC0 recording"))
private data class City(val id:String,val ar:String,val en:String,val lat:Double,val lon:Double,val zone:String="Africa/Cairo")
private val cityList=listOf(City("alex","الإسكندرية","Alexandria",31.2001,29.9187),City("cairo","القاهرة","Cairo",30.0444,31.2357),City("mecca","مكة المكرمة","Makkah",21.4225,39.8262,"Asia/Riyadh"))
private val keys=listOf("fajr","dhuhr","asr","maghrib","isha")
private val arNames=mapOf("fajr" to "الفجر","sunrise" to "الشروق","dhuhr" to "الظهر","asr" to "العصر","maghrib" to "المغرب","isha" to "العشاء")
private val enNames=mapOf("fajr" to "Fajr","sunrise" to "Sunrise","dhuhr" to "Dhuhr","asr" to "Asr","maghrib" to "Maghrib","isha" to "Isha")
class MainActivity:ComponentActivity(){
 private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); createChannel(this)
  if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
  setContent { MaterialTheme(colorScheme=darkColorScheme(primary=Blue,background=Black,surface=Panel,onSurface=Color.White,onBackground=Color.White)){ Surface(color=Black,contentColor=Color.White,modifier=Modifier.fillMaxSize()){ App { url->runCatching{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) } } } } }
 }
}
@Composable private fun App(open:(String)->Unit){
 val ctx=androidx.compose.ui.platform.LocalContext.current; val pref=remember{ctx.getSharedPreferences(PREF,0)}
 var page by remember{mutableStateOf(0)}; var customAdhanUri by remember{mutableStateOf(pref.getString("custom_adhan_uri",null))}; var adhanVoice by remember{mutableStateOf(pref.getString("adhan_voice","adam")?:"adam")}; val audioPicker=androidx.activity.compose.rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){runCatching{ctx.contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)};customAdhanUri=uri.toString();pref.edit().putString("custom_adhan_uri",customAdhanUri).apply()}}; var lang by remember{mutableStateOf(pref.getString("lang","ar")?:"ar")}
 var cityId by remember{mutableStateOf(pref.getString("city","alex")?:"alex")}; var method by remember{mutableStateOf(pref.getString("method","egypt")?:"egypt")}; var hanafi by remember{mutableStateOf(pref.getBoolean("hanafi",false))}
 var offsets by remember{mutableStateOf(keys.associateWith{pref.getInt("off_"+it,0)})}
 var modes by remember{mutableStateOf(keys.associateWith{
  val saved=pref.getString("mode_"+it,null)
  if(!pref.getBoolean("mode_v3_migrated",false) && saved=="NOTIFICATION") "ADHAN" else (saved?:"ADHAN")
 })}
 var tick by remember{mutableLongStateOf(System.currentTimeMillis())}
 LaunchedEffect(Unit){while(true){tick=System.currentTimeMillis();delay(1000)}}
 LaunchedEffect(Unit){if(!pref.getBoolean("mode_v3_migrated",false))pref.edit().putBoolean("mode_v3_migrated",true).apply()}
 LaunchedEffect(lang,cityId,method,hanafi,offsets,modes,adhanVoice){val e=pref.edit().putString("adhan_voice",adhanVoice).putString("lang",lang).putString("city",cityId).putString("method",method).putBoolean("hanafi",hanafi);offsets.forEach{(k,v)->e.putInt("off_"+k,v)};modes.forEach{(k,v)->e.putString("mode_"+k,v)};e.apply()}
 val city=cityList.first{it.id==cityId}; val zone=ZoneId.of(city.zone); val now=Instant.ofEpochMilli(tick); val clock=now.atZone(zone); val date=clock.toLocalDate()
 val times=remember(date,city,hanafi,offsets,method){PrayerCalculator.calculate(date,city.lat,city.lon,zone,hanafi,offsets,method)}
 val tomorrow=date.plusDays(1)
 val tomorrowTimes=remember(tomorrow,city,hanafi,offsets,method){PrayerCalculator.calculate(tomorrow,city.lat,city.lon,zone,hanafi,offsets,method)}
 val upcoming=(listOf(date to times,tomorrow to tomorrowTimes)).flatMap{(day,pt)->pt.entries().filter{it.first in keys}.map{(key,hour)->Triple(key,day.atStartOfDay(zone).plusNanos((hour*3_600_000_000_000.0).toLong()),hour)}}.filter{it.second.toInstant().isAfter(now)}.minByOrNull{it.second.toInstant()}!!
 val nextName=upcoming.first
 val remain=Duration.between(now,upcoming.second.toInstant()).seconds.coerceAtLeast(0)
 val hijri=runCatching{HijrahDate.from(date).toString()}.getOrDefault("")
 LaunchedEffect(times,date,modes,city,hanafi,offsets,method){reschedulePrayerAlarms(ctx)}
 Column(Modifier.fillMaxSize().background(Black).statusBarsPadding()){
  CompositionLocalProvider(LocalLayoutDirection provides if(lang=="ar")LayoutDirection.Rtl else LayoutDirection.Ltr){
  Row(Modifier.fillMaxWidth().padding(horizontal=18.dp, vertical=12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Column{Text("LaZine Prayer",fontSize=23.sp,fontWeight=FontWeight.Bold,color=Color.White);Text(if(lang=="ar")"مواقيت الصلاة بدون إنترنت" else "Offline prayer times",color=Color.LightGray,fontSize=12.sp)}
   TextButton(onClick={page=1}){Text("⚙  "+if(lang=="ar")"الإعدادات" else "Settings",color=Blue,fontSize=15.sp)}
  }
  when(page){
   0->Home(lang,city,date,hijri,times,nextName,remain)
   1->SettingsScreen(lang,{lang=it},cityId,{cityId=it},method,{method=it},hanafi,{hanafi=it},offsets,{offsets=it},modes,{modes=it},
    {if(Build.VERSION.SDK_INT>=31)runCatching{ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))};if(Build.VERSION.SDK_INT>=33)runCatching{ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,ctx.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}},
    {page=2},{open(BUG)},{open("https://github.com/ZineFather/LaZine-Prayer")},{audioPicker.launch(arrayOf("audio/*"))},{customAdhanUri=null;pref.edit().remove("custom_adhan_uri").apply()},customAdhanUri,adhanVoice,{adhanVoice=it})
   else->Developer(lang,{page=1},{open(DEV)},{open(BUG)})
  }
  }
  Spacer(Modifier.weight(1f))
  NavigationBar(containerColor=Panel,modifier=Modifier.navigationBarsPadding()){NavigationBarItem(page==0,{page=0},icon={Text("⌂")},label={Text(if(lang=="ar")"الرئيسية" else "Home")});NavigationBarItem(page!=0,{page=1},icon={Text("⚙")},label={Text(if(lang=="ar")"الإعدادات" else "Settings")})}
 }
}
@Composable private fun Home(lang:String,city:City,date:LocalDate,hijri:String,t:PrayerTimes,next:String,left:Long){
 val ar=lang=="ar"; Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF101D2E)),shape=RoundedCornerShape(24.dp),modifier=Modifier.fillMaxWidth()){
   Column(Modifier.fillMaxWidth().padding(20.dp),horizontalAlignment=Alignment.CenterHorizontally){
    Text(if(ar)city.ar else city.en,color=Blue);Text(if(ar)"مواقيت الصلاة" else "Prayer times",fontSize=25.sp,fontWeight=FontWeight.Bold,color=Color.White)
    Text(date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",if(ar)Locale("ar") else Locale.ENGLISH)),color=Color.LightGray,fontSize=12.sp)
    Text(if(ar)"التاريخ الهجري: "+hijri else "Hijri: "+hijri,color=Color.Gray,fontSize=11.sp)
    Spacer(Modifier.height(10.dp));Text(if(ar)"الصلاة القادمة: "+(arNames[next]?:next) else "Next prayer: "+(enNames[next]?:next),color=Blue)
    Text("%02d:%02d:%02d".format(left/3600,(left%3600)/60,left%60),fontSize=34.sp,fontWeight=FontWeight.Bold)
    Text(if(ar)"الوقت المتبقي" else "Remaining",color=Color.Gray,fontSize=11.sp)
   }
  }
  Text(if(ar)"مواقيت اليوم" else "TODAY",color=Blue,fontSize=11.sp,fontWeight=FontWeight.Bold)
  t.entries().forEach{(k,v)->Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if(k==next)Color(0xFF14243A) else Panel).padding(17.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(if(ar)arNames[k]?:k else enNames[k]?:k,fontWeight=if(k==next)FontWeight.Bold else FontWeight.Normal,color=Color.White);Text(PrayerCalculator.format(v,lang),color=Blue,fontWeight=FontWeight.Bold)}}
  Text(if(ar)"الحساب محلي وفق زوايا الهيئة المصرية للمساحة: الفجر 19.5° والعشاء 17.5°." else "Local calculation: Egyptian convention (Fajr 19.5°, Isha 17.5°).",color=Color.Gray,fontSize=10.sp)
 }
}
@Composable private fun SettingsScreen(lang:String,setLang:(String)->Unit,city:String,setCity:(String)->Unit,method:String,setMethod:(String)->Unit,hanafi:Boolean,setHanafi:(Boolean)->Unit,offs:Map<String,Int>,setOffs:(Map<String,Int>)->Unit,modes:Map<String,String>,setModes:(Map<String,String>)->Unit,permissions:()->Unit,developer:()->Unit,bug:()->Unit,sourceCode:()->Unit,pickAudio:()->Unit,clearAudio:()->Unit,customAudio:String?,adhanVoice:String,setAdhanVoice:(String)->Unit){
 val ctx=androidx.compose.ui.platform.LocalContext.current
 val ar=lang=="ar"; Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  Text(if(ar)"الإعدادات" else "Settings",fontSize=26.sp,fontWeight=FontWeight.Bold)
  Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(14.dp)){Text(if(ar)"اللغة" else "Language");Row{FilterChip(lang=="ar",{setLang("ar")},{Text("العربية")});Spacer(Modifier.width(8.dp));FilterChip(lang=="en",{setLang("en")},{Text("English")})}}}
  Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(14.dp)){Text(if(ar)"المدينة" else "City");cityList.forEach{c->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(city==c.id,{setCity(c.id)});Text(if(ar)c.ar else c.en)}}}}
  Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(14.dp)){Text(if(ar)"طريقة حساب المواقيت" else "Calculation method");listOf("egypt" to (if(ar)"الهيئة المصرية" else "Egyptian Survey"),"mwl" to "Muslim World League","isna" to "ISNA").forEach{(id,label)->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(method==id,{setMethod(id)});Text(label)}}}}
  Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.padding(14.dp)){Text(if(ar)"حساب العصر" else "Asr method");Row(verticalAlignment=Alignment.CenterVertically){RadioButton(!hanafi,{setHanafi(false)});Text(if(ar)"قياسي" else "Standard")};Row(verticalAlignment=Alignment.CenterVertically){RadioButton(hanafi,{setHanafi(true)});Text(if(ar)"حنفي" else "Hanafi")}}}
  Text(if(ar)"تعديل المواقيت بالدقائق" else "Minute adjustments",color=Blue,fontWeight=FontWeight.Bold)
  keys.forEach{k->Card(colors=CardDefaults.cardColors(containerColor=Panel)){Row(Modifier.fillMaxWidth().padding(10.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(if(ar)arNames[k]!! else enNames[k]!!);TextButton(onClick={setOffs(offs+(k to ((offs[k]?:0)-1)))}){Text("−")};Text((offs[k]?:0).toString(),color=Blue);TextButton(onClick={setOffs(offs+(k to ((offs[k]?:0)+1)))}){Text("+")}}}}
  Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(ar)"اختيار صوت المؤذن" else "Choose Adhan voice",color=Blue,fontWeight=FontWeight.Bold);Text(if(ar)"كل التسجيلات مضبوطة على مستوى صوت متوازن وواضح. اختر الصوت ثم اضغط تجربة." else "Recordings are loudness-normalized for clear, balanced playback. Select a voice, then tap Test.",color=Color.LightGray,fontSize=12.sp);adhanVoices.forEach{voice->Row(Modifier.fillMaxWidth().clickable{setAdhanVoice(voice.id)},verticalAlignment=Alignment.CenterVertically){RadioButton(selected=adhanVoice==voice.id,onClick={setAdhanVoice(voice.id)});Column{Text(if(ar)voice.ar else voice.en,color=Color.White,fontWeight=FontWeight.SemiBold);Text(voice.note,color=Color.Gray,fontSize=10.sp)}}};OutlinedButton(onClick={val test=Intent(ctx,PrayerAlarmReceiver::class.java).setAction("com.zine.prayer.TEST").putExtra("prayer","fajr");ctx.sendBroadcast(test)},modifier=Modifier.fillMaxWidth()){Text(if(ar)"تجربة الصوت المختار" else "Test selected voice")}}}
  Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(ar)"ملف أذان مخصص" else "Custom Adhan audio",color=Blue,fontWeight=FontWeight.Bold);Text(if(customAudio!=null)if(ar)"تم اختيار ملف صوتي خاص بك" else "Your custom audio file is selected" else if(ar)"سيستخدم التطبيق صوت الأذان المرفق." else "The bundled Adhan will be used.",color=Color.LightGray,fontSize=12.sp);Button(onClick=pickAudio,modifier=Modifier.fillMaxWidth()){Text(if(ar)"اختيار ملف صوتي (MP3 / M4A / OGG)" else "Choose audio file (MP3 / M4A / OGG)")};if(customAudio!=null)OutlinedButton(onClick=clearAudio,modifier=Modifier.fillMaxWidth()){Text(if(ar)"العودة للصوت الأصلي" else "Use bundled Adhan")}}}
  Text(if(ar)"تنبيه كل صلاة" else "Prayer notifications",color=Blue,fontWeight=FontWeight.Bold)
  Text(if(ar)"اختر الأذان الكامل أو نغمة إشعار أو الصامت أو إيقاف التنبيه. استخدم تجربة للتأكد من الصوت." else "Choose full Adhan, a short notification tone, silent, or off. Use Test to verify.",color=Color.LightGray,fontSize=12.sp)
  keys.forEach{k->var expanded by remember(k){mutableStateOf(false)};Card(colors=CardDefaults.cardColors(containerColor=Panel)){Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(ar)arNames[k]!! else enNames[k]!!,color=Color.White,fontWeight=FontWeight.SemiBold);Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text(when(modes[k]){"ADHAN"->if(ar)"أذان كامل" else "Full Adhan";"SHORT"->if(ar)"نغمة قصيرة" else "Short tone";"SILENT"->if(ar)"صامت" else "Silent";"OFF"->if(ar)"إيقاف" else "Off";else->if(ar)"إشعار عادي" else "Notification"})};DropdownMenu(expanded,{expanded=false}){listOf("ADHAN","SHORT","NOTIFICATION","SILENT","OFF").forEach{v->DropdownMenuItem(text={Text(when(v){"ADHAN"->if(ar)"أذان كامل" else "Full Adhan";"SHORT"->if(ar)"نغمة قصيرة" else "Short tone";"NOTIFICATION"->if(ar)"إشعار عادي" else "Notification";"SILENT"->if(ar)"صامت" else "Silent";else->if(ar)"إيقاف" else "Off"})},onClick={setModes(modes+(k to v));expanded=false})}}};val test=Intent(ctx,PrayerAlarmReceiver::class.java).setAction("com.zine.prayer.TEST").putExtra("prayer",k);OutlinedButton(onClick={ctx.sendBroadcast(test)}){Text(if(ar)"تجربة" else "Test")}};if(modes[k]=="ADHAN")Text(if(ar)"الأذان يعمل من ملف محلي مرفق داخل التطبيق." else "Adhan plays from an audio file bundled in the app.",color=Color.Gray,fontSize=10.sp)}}}
  Button(onClick=permissions,modifier=Modifier.fillMaxWidth()){Text(if(ar)"إدارة أذونات الإشعارات والمنبهات" else "Notification & alarm permissions")}
  OutlinedButton(onClick=developer,modifier=Modifier.fillMaxWidth()){Text(if(ar)"About Developer • عن المطور" else "About Developer")}
  OutlinedButton(onClick=bug,modifier=Modifier.fillMaxWidth()){Text(if(ar)"Report a Bug • الإبلاغ عن خطأ" else "Report a bug")}
  OutlinedButton(onClick=sourceCode,modifier=Modifier.fillMaxWidth()){Text(if(ar)"Source Code • الكود المصدري على GitHub" else "Source Code • GitHub")}
 }
}
@Composable private fun Developer(lang:String,back:()->Unit,telegram:()->Unit,bug:()->Unit){
 val ar=lang=="ar"
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp, vertical=12.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  TextButton(onClick=back,contentPadding=PaddingValues(horizontal=8.dp,vertical=6.dp)){Text("‹  "+if(ar)"رجوع للإعدادات" else "Back to Settings",color=Blue,fontSize=14.sp)}
  Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF14243A)),shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth()){
   Column(Modifier.fillMaxWidth().padding(vertical=26.dp,horizontal=20.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(9.dp)){
    Box(contentAlignment=Alignment.BottomEnd){
     Image(painter=painterResource(id=R.drawable.zinefather),contentDescription="ZineFather developer profile",modifier=Modifier.size(116.dp).clip(CircleShape),contentScale=ContentScale.Crop)
     Surface(shape=CircleShape,color=Color(0xFF173F35),modifier=Modifier.size(25.dp)){Box(contentAlignment=Alignment.Center){Text("✓",color=Color(0xFF8BE0B2),fontSize=13.sp,fontWeight=FontWeight.Bold)}}
    }
    Text("ZineFather",fontSize=27.sp,fontWeight=FontWeight.Bold,color=Color.White)
    Text("@ZineFather",fontSize=14.sp,color=Blue)
    Surface(shape=RoundedCornerShape(50),color=Color(0xFF203752)){Text(if(ar)"Android Developer • ROM Maintainer" else "Android Developer • ROM Maintainer",modifier=Modifier.padding(horizontal=13.dp,vertical=7.dp),fontSize=11.sp,color=Color(0xFFD8E9FF))}
   }
  }
  Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
   Text(if(ar)"نبذة عن المطور" else "ABOUT THE DEVELOPER",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Blue)
   Card(colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(20.dp),modifier=Modifier.fillMaxWidth()){
    Text(if(ar)"مطور رومات أندرويد ومؤسس ZineROM. يعمل على تطوير رومات Samsung وأدوات أندرويد ومواقع وتطبيقات، ويهتم بتحسين تجربة الاستخدام ودعم الأجهزة." else "Android ROM developer and founder of ZineROM. Focused on Samsung custom ROMs, Android tools, websites and apps, with an emphasis on usability and device support.",modifier=Modifier.padding(18.dp),fontSize=14.sp,lineHeight=22.sp,color=Color(0xFFE0E6EF))
   }
  }
  Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
   Text(if(ar)"التواصل والدعم" else "CONTACT & SUPPORT",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Blue)
   Button(onClick=telegram,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp),contentPadding=PaddingValues(vertical=14.dp)){Text("Telegram  •  @ZineFather",fontWeight=FontWeight.SemiBold)}
   OutlinedButton(onClick=bug,modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(15.dp),contentPadding=PaddingValues(vertical=14.dp)){Text(if(ar)"الإبلاغ عن مشكلة" else "Report a bug")}
  }
  Text("LaZine Prayer",modifier=Modifier.fillMaxWidth(),textAlign=androidx.compose.ui.text.style.TextAlign.Center,color=Color.Gray,fontSize=11.sp)
 }
}
private fun createChannel(c:Context){if(Build.VERSION.SDK_INT>=26){val nm=c.getSystemService(NotificationManager::class.java)
 val alarm=NotificationChannel("prayer_adhan_v3","Full Adhan",NotificationManager.IMPORTANCE_LOW)
 alarm.description="Full offline Adhan playback; stop it from the notification"
 alarm.setSound(null,null);alarm.enableVibration(false)
 val normal=NotificationChannel("prayer_alerts_v2","Prayer notification sound",NotificationManager.IMPORTANCE_HIGH)
 normal.description="Normal Android notification sound at prayer time"
 normal.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).build())
 val short=NotificationChannel("prayer_short_v2","Short prayer tone",NotificationManager.IMPORTANCE_HIGH)
 short.description="Short Android notification tone at prayer time"
 short.setSound(android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION).build())
 val silent=NotificationChannel("prayer_silent_v2","Silent prayer notifications",NotificationManager.IMPORTANCE_LOW);silent.setSound(null,null);silent.enableVibration(false)
 nm.createNotificationChannel(alarm);nm.createNotificationChannel(normal);nm.createNotificationChannel(short);nm.createNotificationChannel(silent)}}
internal fun scheduleAlarms(c:Context,t:PrayerTimes,d:LocalDate,modes:Map<String,String>,zoneName:String){val am=c.getSystemService(AlarmManager::class.java);val zone=ZoneId.of(zoneName);val now=System.currentTimeMillis();t.entries().filter{it.first in keys}.forEach{(k,v)->if(modes[k]=="OFF")return@forEach;val at=d.atStartOfDay(zone).toEpochSecond()*1000+(v*3600000.0).toLong();if(at<=now)return@forEach;val i=Intent(c,PrayerAlarmReceiver::class.java).putExtra("prayer",k);val pi=PendingIntent.getBroadcast(c,d.toEpochDay().toInt()*10+keys.indexOf(k)+1,i,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);runCatching{if(Build.VERSION.SDK_INT>=31&&!am.canScheduleExactAlarms())am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi) else am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,pi)}}}
