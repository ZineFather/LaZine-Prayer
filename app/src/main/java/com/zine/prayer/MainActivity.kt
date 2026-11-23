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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.*
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
private val Black=Color(0xFF050609)
private val Panel=Color(0xCC111A28)
private val Blue=Color(0xFF8CC7FF)
private val Glass=Color(0xB8121D2B)
private val GlassBorder=Color(0x35D9EBFF)
private val Muted=Color(0xFF9BAEC4)
private const val PREF="lenzin_prayer_settings"
private data class AdhanVoice(val id:String,val ar:String,val en:String,val note:String)
private val adhanVoices=listOf(AdhanVoice("adam","الأذان الأساسي","الأذان الأساسي","تسجيل آدم سيناجدا • CC0 1.0"),AdhanVoice("sabah","صباح فخري","صباح فخري","الملكية العامة • 2:38"),AdhanVoice("aqib","عاقب عزيز","عاقب عزيز","تسجيل عاقب عزيز • CC BY-SA 4.0"),AdhanVoice("andrewler","أندرو لير","أندرو لير","تسجيل أندرو لير • CC BY-SA 4.0"))
private data class City(val id:String,val ar:String,val en:String,val lat:Double,val lon:Double,val zone:String="Africa/Cairo")
private val cityList=listOf(City("alex","الإسكندرية","Alexandria",31.2001,29.9187),City("cairo","القاهرة","Cairo",30.0444,31.2357),City("mecca","مكة المكرمة","Makkah",21.4225,39.8262,"Asia/Riyadh"))
private val keys=listOf("fajr","dhuhr","asr","maghrib","isha")
private val arNames=mapOf("fajr" to "الفجر","sunrise" to "الشروق","dhuhr" to "الظهر","asr" to "العصر","maghrib" to "المغرب","isha" to "العشاء")
private val enNames=mapOf("fajr" to "Fajr","sunrise" to "Sunrise","dhuhr" to "Dhuhr","asr" to "Asr","maghrib" to "Maghrib","isha" to "Isha")
class MainActivity:ComponentActivity(){
 private val permission=registerForActivityResult(ActivityResultContracts.RequestPermission()){}
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState); createChannel(this)
  if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(this,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
  setContent {
   val scheme=darkColorScheme(
    primary=Blue,onPrimary=Color(0xFF07111D),background=Black,onBackground=Color(0xFFF2F6FC),
    surface=Glass,onSurface=Color(0xFFF2F6FC),surfaceVariant=Color(0xD91B293A),
    onSurfaceVariant=Muted,outline=Color(0x557FA9D4),outlineVariant=GlassBorder,
    secondary=Color(0xFFB5D9FF),onSecondary=Color(0xFF081421)
   )
   MaterialTheme(colorScheme=scheme){
    Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFF070B12),Color(0xFF050609),Color(0xFF091321))))) {
     Box(Modifier.size(360.dp).align(Alignment.TopEnd).offset(x=110.dp,y=(-100).dp).background(Brush.radialGradient(listOf(Color(0x243B8DCE),Color.Transparent))))
     Box(Modifier.size(300.dp).align(Alignment.BottomStart).offset(x=(-120).dp,y=80.dp).background(Brush.radialGradient(listOf(Color(0x182C5D9B),Color.Transparent))))
     Surface(color=Color.Transparent,contentColor=scheme.onBackground,modifier=Modifier.fillMaxSize()){
      App { url->runCatching{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(url))) } }
     }
    }
   }
  }
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
 var selectedPrayer by remember{mutableStateOf<String?>(null)}
 val selectPrayer = remember { { prayer: String -> selectedPrayer = prayer } }
 val resetSelectedPrayer = remember { { selectedPrayer = null } }
 BackHandler(enabled=page!=0){page=when(page){2->1;1->0;else->0}}
 LaunchedEffect(page){while(page==0){tick=System.currentTimeMillis();delay(1000)}}
 LaunchedEffect(Unit){if(!pref.getBoolean("mode_v3_migrated",false))pref.edit().putBoolean("mode_v3_migrated",true).apply()}
 LaunchedEffect(lang,cityId,method,hanafi,offsets,modes,adhanVoice){val e=pref.edit().putString("adhan_voice",adhanVoice).putString("lang",lang).putString("city",cityId).putString("method",method).putBoolean("hanafi",hanafi);offsets.forEach{(k,v)->e.putInt("off_"+k,v)};modes.forEach{(k,v)->e.putString("mode_"+k,v)};e.apply()}
 val city=cityList.first{it.id==cityId}; val zone=ZoneId.of(city.zone); val now=Instant.ofEpochMilli(tick); val clock=now.atZone(zone); val date=clock.toLocalDate()
 val times=remember(date,city,hanafi,offsets,method){PrayerCalculator.calculate(date,city.lat,city.lon,zone,hanafi,offsets,method)}
 val tomorrow=date.plusDays(1)
 val tomorrowTimes=remember(tomorrow,city,hanafi,offsets,method){PrayerCalculator.calculate(tomorrow,city.lat,city.lon,zone,hanafi,offsets,method)}
 val upcoming=(listOf(date to times,tomorrow to tomorrowTimes)).flatMap{(day,pt)->pt.entries().filter{it.first in keys}.map{(key,hour)->Triple(key,day.atStartOfDay(zone).toEpochSecond()*1000L+(hour*3_600_000.0).toLong(),hour)}}.filter{it.second>tick}.minByOrNull{it.second}!!
 val nextName=upcoming.first
 val remain=((upcoming.second-tick)/1000L).coerceAtLeast(0)
 val selectedTarget=if(selectedPrayer==null) upcoming.second else (listOf(date to times,tomorrow to tomorrowTimes)).flatMap{(day,pt)->pt.entries().filter{it.first==selectedPrayer}.map{(_,hour)->day.atStartOfDay(zone).toEpochSecond()*1000L+(hour*3_600_000.0).toLong()}}.filter{it>tick}.minOrNull()?:upcoming.second
 val countdownName=selectedPrayer?:nextName
 val countdownRemain=((selectedTarget-tick)/1000L).coerceAtLeast(0)
 val hijri=runCatching{HijrahDate.from(date).toString()}.getOrDefault("")
 LaunchedEffect(times,date,modes,city,hanafi,offsets,method){reschedulePrayerAlarms(ctx)}
 Column(Modifier.fillMaxSize().statusBarsPadding()){
  CompositionLocalProvider(LocalLayoutDirection provides if(lang=="ar")LayoutDirection.Rtl else LayoutDirection.Ltr){
  Row(Modifier.fillMaxWidth().padding(horizontal=18.dp, vertical=12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Column{Text("LaZine Prayer",fontSize=23.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface);Text(if(lang=="ar")"مواقيت الصلاة بدون إنترنت" else "Offline prayer times",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp)}
   TextButton(onClick={page=1}){Text("⚙  "+if(lang=="ar")"الإعدادات" else "Settings",color=MaterialTheme.colorScheme.primary,fontSize=15.sp)}
  }
  AnimatedContent(
   targetState=page,
   transitionSpec={
    (fadeIn(animationSpec=tween(180))+slideInHorizontally(animationSpec=tween(220)){it/12}) togetherWith
     (fadeOut(animationSpec=tween(120))+slideOutHorizontally(animationSpec=tween(180)){-it/12})
   },
   label="screen_navigation"
  ){ targetPage ->
  when(targetPage){
   0->Home(lang,city,date,hijri,times,selectedPrayer,countdownName,countdownRemain,nextName,selectPrayer,resetSelectedPrayer)
   1->SettingsScreen(lang,{lang=it},cityId,{cityId=it},method,{method=it},hanafi,{hanafi=it},offsets,{offsets=it},modes,{modes=it},
    {if(Build.VERSION.SDK_INT>=31)runCatching{ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))};if(Build.VERSION.SDK_INT>=33)runCatching{ctx.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,ctx.packageName).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))}},
    {page=2},{open("https://github.com/ZineFather/LaZine-Prayer")},{audioPicker.launch(arrayOf("audio/*"))},{customAdhanUri=null;pref.edit().remove("custom_adhan_uri").apply()},customAdhanUri,adhanVoice,{adhanVoice=it},{page=0})
   else->Developer(lang,{page=1})
  }
  }
  }
  Spacer(Modifier.weight(1f))
  NavigationBar(containerColor=Color(0xE60B111B),modifier=Modifier.navigationBarsPadding()){NavigationBarItem(page==0,{page=0},icon={Text("⌂")},label={Text(if(lang=="ar")"الرئيسية" else "Home")});NavigationBarItem(page!=0,{page=1},icon={Text("⚙")},label={Text(if(lang=="ar")"الإعدادات" else "Settings")})}
 }
}
@Composable private fun Home(lang:String,city:City,date:LocalDate,hijri:String,t:PrayerTimes,selected:String?,countdownName:String,left:Long,next:String,onSelect:(String)->Unit,onReset:()->Unit){
 val ar=lang=="ar"
 Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Card(shape=RoundedCornerShape(30.dp),colors=CardDefaults.cardColors(containerColor=Color(0xD9162232)),border=BorderStroke(1.dp,Color(0x557FB9F0)),modifier=Modifier.fillMaxWidth().animateContentSize()){
   Box(Modifier.background(Brush.linearGradient(listOf(Color(0x302E75B7),Color.Transparent,Color(0x18204D7A))),RoundedCornerShape(30.dp)).padding(22.dp)){
    Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(7.dp)){
     Text(if(ar)city.ar else city.en,color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.SemiBold,fontSize=14.sp)
     Text(if(ar)"مواقيت الصلاة" else "Prayer times",fontSize=25.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface)
     Text(date.format(DateTimeFormatter.ofPattern("EEEE d MMMM yyyy",if(ar)Locale("ar") else Locale.ENGLISH)),color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp)
     Text(if(ar)"التاريخ الهجري: "+hijri else "Hijri: "+hijri,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=11.sp)
     Spacer(Modifier.height(8.dp))
     Surface(shape=RoundedCornerShape(50),color=MaterialTheme.colorScheme.primary.copy(alpha=.14f)){Text(if(selected==null){if(ar)"الصلاة القادمة" else "NEXT PRAYER"}else{if(ar)"العد التنازلي للصلاة المختارة" else "SELECTED PRAYER"},modifier=Modifier.padding(horizontal=14.dp,vertical=7.dp),color=MaterialTheme.colorScheme.primary,fontSize=11.sp,fontWeight=FontWeight.Bold)}
     Text(if(ar)(arNames[countdownName]?:countdownName) else (enNames[countdownName]?:countdownName),fontSize=22.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface)
     Text("%02d:%02d:%02d".format(left/3600,(left%3600)/60,left%60),fontSize=42.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
     Text(if(ar)"الوقت المتبقي" else "TIME REMAINING",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=10.sp)
     if(selected!=null) TextButton(onClick=onReset){Text(if(ar)"↺ العودة للصلاة القادمة" else "↺ Back to next prayer",fontWeight=FontWeight.SemiBold)}
    }
   }
  }
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Column(verticalArrangement=Arrangement.spacedBy(2.dp)){Text(if(ar)"مواقيت اليوم" else "TODAY'S PRAYERS",color=MaterialTheme.colorScheme.primary,fontSize=12.sp,fontWeight=FontWeight.Bold);Text(if(ar)"اضغط على أي صلاة لعرض العد التنازلي لها" else "Tap any prayer to view its countdown",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=11.sp)}
   if(selected!=null) TextButton(onClick=onReset){Text(if(ar)"إعادة ضبط" else "Reset")}
  }
   PrayerRows(t,lang,selected,next,onSelect)
  Text(if(ar)"الحساب محلي وفق زوايا الهيئة المصرية للمساحة: الفجر 19.5° والعشاء 17.5°." else "Local calculation: Egyptian convention (Fajr 19.5°, Isha 17.5°).",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=10.sp)
 }
}
@Composable private fun PrayerRows(t:PrayerTimes,lang:String,selected:String?,next:String,onSelect:(String)->Unit){
 val ar=lang=="ar"
 t.entries().forEach{(k,v)->
  val isSelected=selected==k
  val isNext=selected==null&&k==next
  Card(onClick={onSelect(k)},shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=if(isSelected)Color(0xD52B5278) else if(isNext)Color(0xB51A2A3D) else Glass),border=BorderStroke(1.dp,if(isSelected)Blue.copy(alpha=.78f) else if(isNext)Blue.copy(alpha=.42f) else GlassBorder),modifier=Modifier.fillMaxWidth().animateContentSize()){
   Row(Modifier.fillMaxWidth().padding(horizontal=17.dp,vertical=15.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
     Box(Modifier.size(38.dp).clip(CircleShape).background(if(isSelected||isNext)MaterialTheme.colorScheme.primary.copy(alpha=.20f) else MaterialTheme.colorScheme.surfaceVariant),contentAlignment=Alignment.Center){Text(when(k){"fajr"->"☾";"sunrise"->"☀";"dhuhr"->"◉";"asr"->"◷";"maghrib"->"◒";else->"☽"},color=MaterialTheme.colorScheme.primary,fontSize=17.sp)}
     Column(verticalArrangement=Arrangement.spacedBy(2.dp)){Text(if(ar)arNames[k]?:k else enNames[k]?:k,fontWeight=if(isSelected||isNext)FontWeight.Bold else FontWeight.Medium,color=MaterialTheme.colorScheme.onSurface);if(isSelected)Text(if(ar)"محدد للعد التنازلي" else "Countdown selected",fontSize=10.sp,color=MaterialTheme.colorScheme.primary) else if(isNext)Text(if(ar)"القادمة" else "Next",fontSize=10.sp,color=MaterialTheme.colorScheme.primary)}
    }
    Text(PrayerCalculator.format(v,lang),color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold,fontSize=16.sp)
   }
  }
 }
}
@Composable private fun SettingsScreen(lang:String,setLang:(String)->Unit,city:String,setCity:(String)->Unit,method:String,setMethod:(String)->Unit,hanafi:Boolean,setHanafi:(Boolean)->Unit,offs:Map<String,Int>,setOffs:(Map<String,Int>)->Unit,modes:Map<String,String>,setModes:(Map<String,String>)->Unit,permissions:()->Unit,developer:()->Unit,sourceCode:()->Unit,pickAudio:()->Unit,clearAudio:()->Unit,customAudio:String?,adhanVoice:String,setAdhanVoice:(String)->Unit,developerBack:()->Unit){
 val ctx=androidx.compose.ui.platform.LocalContext.current
 val ar=lang=="ar"; Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal=16.dp,vertical=14.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
  Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text(if(ar)"الإعدادات" else "Settings",fontSize=26.sp,fontWeight=FontWeight.Bold);FilledTonalButton(onClick={developerBack()},shape=RoundedCornerShape(14.dp),contentPadding=PaddingValues(horizontal=16.dp,vertical=9.dp)){Text(if(ar)"رجوع  ‹" else "‹  Back",fontWeight=FontWeight.SemiBold)}}
  Card(shape=RoundedCornerShape(22.dp),colors=CardDefaults.cardColors(containerColor=Glass),border=BorderStroke(1.dp,GlassBorder),modifier=Modifier.fillMaxWidth().animateContentSize()){Column(Modifier.padding(14.dp)){Text(if(ar)"اللغة" else "Language");Row{FilterChip(lang=="ar",{setLang("ar")},{Text("العربية")});Spacer(Modifier.width(8.dp));FilterChip(lang=="en",{setLang("en")},{Text("English")})}}}
  Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.padding(14.dp)){Text(if(ar)"المدينة" else "City");cityList.forEach{c->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(city==c.id,{setCity(c.id)});Text(if(ar)c.ar else c.en)}}}}
  Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.padding(14.dp)){Text(if(ar)"طريقة حساب المواقيت" else "Calculation method");listOf("egypt" to (if(ar)"الهيئة المصرية" else "Egyptian Survey"),"mwl" to "Muslim World League","isna" to "ISNA").forEach{(id,label)->Row(verticalAlignment=Alignment.CenterVertically){RadioButton(method==id,{setMethod(id)});Text(label)}}}}
  Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.padding(14.dp)){Text(if(ar)"حساب العصر" else "Asr method");Row(verticalAlignment=Alignment.CenterVertically){RadioButton(!hanafi,{setHanafi(false)});Text(if(ar)"قياسي" else "Standard")};Row(verticalAlignment=Alignment.CenterVertically){RadioButton(hanafi,{setHanafi(true)});Text(if(ar)"حنفي" else "Hanafi")}}}
  Text(if(ar)"تعديل المواقيت بالدقائق" else "Minute adjustments",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
  keys.forEach{k->Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Row(Modifier.fillMaxWidth().padding(horizontal=13.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween){Text(if(ar)arNames[k]!! else enNames[k]!!);TextButton(onClick={setOffs(offs+(k to ((offs[k]?:0)-1)))}){Text("−")};Text((offs[k]?:0).toString(),color=MaterialTheme.colorScheme.primary);TextButton(onClick={setOffs(offs+(k to ((offs[k]?:0)+1)))}){Text("+")}}}}
  Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(ar)"اختيار صوت المؤذن" else "Choose Adhan voice",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text(if(ar)"كل التسجيلات مضبوطة على مستوى صوت متوازن وواضح. اختر الصوت ثم اضغط تجربة." else "Recordings are loudness-normalized for clear, balanced playback. Select a voice, then tap Test.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp);adhanVoices.forEach{voice->Row(Modifier.fillMaxWidth().clickable{setAdhanVoice(voice.id)},verticalAlignment=Alignment.CenterVertically){RadioButton(selected=adhanVoice==voice.id,onClick={setAdhanVoice(voice.id)});Column{Text(voice.ar,color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.SemiBold);Text(voice.note,color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=10.sp)}}};OutlinedButton(onClick={val test=Intent(ctx,PrayerAlarmReceiver::class.java).setAction("com.zine.prayer.TEST_VOICE").putExtra("prayer","fajr");ctx.sendBroadcast(test)},modifier=Modifier.fillMaxWidth()){Text(if(ar)"تجربة الصوت المختار" else "Test selected voice")}}}
  Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(ar)"ملف أذان مخصص" else "Custom Adhan audio",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold);Text(if(customAudio!=null)if(ar)"تم اختيار ملف صوتي خاص بك" else "Your custom audio file is selected" else if(ar)"سيستخدم التطبيق صوت الأذان المرفق." else "The bundled Adhan will be used.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp);Button(onClick=pickAudio,modifier=Modifier.fillMaxWidth()){Text(if(ar)"اختيار ملف صوتي (MP3 / M4A / OGG)" else "Choose audio file (MP3 / M4A / OGG)")};if(customAudio!=null)OutlinedButton(onClick=clearAudio,modifier=Modifier.fillMaxWidth()){Text(if(ar)"العودة للصوت الأصلي" else "Use bundled Adhan")}}}
  Text(if(ar)"تنبيه كل صلاة" else "Prayer notifications",color=MaterialTheme.colorScheme.primary,fontWeight=FontWeight.Bold)
  Text(if(ar)"اختر الأذان الكامل أو نغمة إشعار أو الصامت أو إيقاف التنبيه. استخدم تجربة للتأكد من الصوت." else "Choose full Adhan, a short notification tone, silent, or off. Use Test to verify.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=12.sp)
  keys.forEach{k->var expanded by remember(k){mutableStateOf(false)};Card(colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)){Column(Modifier.fillMaxWidth().padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(if(ar)arNames[k]!! else enNames[k]!!,color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.SemiBold);Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)){Box(Modifier.weight(1f)){OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text(when(modes[k]){"ADHAN"->if(ar)"أذان كامل" else "Full Adhan";"SHORT"->if(ar)"نغمة قصيرة" else "Short tone";"SILENT"->if(ar)"صامت" else "Silent";"OFF"->if(ar)"إيقاف" else "Off";else->if(ar)"إشعار عادي" else "Notification"})};DropdownMenu(expanded,{expanded=false}){listOf("ADHAN","SHORT","NOTIFICATION","SILENT","OFF").forEach{v->DropdownMenuItem(text={Text(when(v){"ADHAN"->if(ar)"أذان كامل" else "Full Adhan";"SHORT"->if(ar)"نغمة قصيرة" else "Short tone";"NOTIFICATION"->if(ar)"إشعار عادي" else "Notification";"SILENT"->if(ar)"صامت" else "Silent";else->if(ar)"إيقاف" else "Off"})},onClick={setModes(modes+(k to v));expanded=false})}}};val test=Intent(ctx,PrayerAlarmReceiver::class.java).setAction("com.zine.prayer.TEST").putExtra("prayer",k);OutlinedButton(onClick={ctx.sendBroadcast(test)}){Text(if(ar)"تجربة" else "Test")}};if(modes[k]=="ADHAN")Text(if(ar)"الأذان يعمل من ملف محلي مرفق داخل التطبيق." else "Adhan plays from an audio file bundled in the app.",color=MaterialTheme.colorScheme.onSurfaceVariant,fontSize=10.sp)}}}
  Button(onClick=permissions,modifier=Modifier.fillMaxWidth()){Text(if(ar)"إدارة أذونات الإشعارات والمنبهات" else "Notification & alarm permissions")}
  OutlinedButton(onClick=developer,modifier=Modifier.fillMaxWidth()){Text(if(ar)"About Developer • عن المطور" else "About Developer")}

  OutlinedButton(onClick=sourceCode,modifier=Modifier.fillMaxWidth()){Text(if(ar)"Source Code • الكود المصدري على GitHub" else "Source Code • GitHub")}
 }
}
@Composable private fun Developer(lang:String,back:()->Unit){
 val ar=lang=="ar"
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  TextButton(onClick=back){Text("‹  "+if(ar)"رجوع للإعدادات" else "Back to Settings")}
  Card(colors=CardDefaults.cardColors(containerColor=Color(0xD9162232)),border=BorderStroke(1.dp,GlassBorder),shape=RoundedCornerShape(28.dp),modifier=Modifier.fillMaxWidth()){
   Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(9.dp)){
    Text("ZineFather",fontSize=27.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.onSurface)
    Text("@ZineFather",fontSize=14.sp,color=MaterialTheme.colorScheme.primary)
    Text("Android Developer • ROM Maintainer",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  }
  Text(if(ar)"نبذة عن المطور" else "ABOUT THE DEVELOPER",fontSize=12.sp,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
  Card(colors=CardDefaults.cardColors(containerColor=Glass),border=BorderStroke(1.dp,GlassBorder),shape=RoundedCornerShape(22.dp),modifier=Modifier.fillMaxWidth()){
   Text(if(ar)"مطور رومات أندرويد ومؤسس ZineROM. يعمل على تطوير رومات Samsung وأدوات أندرويد ومواقع وتطبيقات، ويهتم بتحسين تجربة الاستخدام ودعم الأجهزة." else "Android ROM developer and founder of ZineROM. Focused on Samsung custom ROMs, Android tools, websites and apps, with an emphasis on usability and device support.",modifier=Modifier.padding(18.dp),fontSize=14.sp,lineHeight=22.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }

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
