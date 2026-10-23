package org.lynos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lynos.data.KnowledgeNote
import org.lynos.data.MnemosDatabase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sin

private val Slate = Color(0xFF182333)
private val Navy = Color(0xFF24364D)
private val Cyan = Color(0xFF38CFFF)
private val Blue = Color(0xFF527DB8)
private val Ink = Color(0xFFE7F0FC)
private val Muted = Color(0xFF9EADC2)
private val Panel = Color(0xEE1D2C40)

class MainActivity : ComponentActivity() {
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.statusBarColor = android.graphics.Color.rgb(24,35,51)
  window.navigationBarColor = android.graphics.Color.rgb(24,35,51)
  if (Build.VERSION.SDK_INT >= 26) getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel("lynos_status","LynOS status",NotificationManager.IMPORTANCE_LOW))
  setContent { MaterialTheme(colorScheme=darkColorScheme(primary=Cyan,background=Slate,surface=Navy)) { LynOSApp(applicationContext) } }
 }
}
private enum class Page { HOME, MNEMOS, CUK, SETTINGS, STATUS }

@Composable
private fun LynOSApp(context: Context) {
 val db=remember { MnemosDatabase(context) }
 var page by remember { mutableStateOf(Page.HOME) }
 var search by remember { mutableStateOf("") }
 var title by remember { mutableStateOf("") }
 var body by remember { mutableStateOf("") }
 var editId by remember { mutableStateOf<Long?>(null) }
 var selectedId by remember { mutableStateOf<Long?>(null) }
 var notes by remember { mutableStateOf(emptyList<KnowledgeNote>()) }
 var message by remember { mutableStateOf("") }
 var code by remember { mutableStateOf("jancuk\nucap \"Halo dari Cuk Studio\"\nselesai jancuk") }
 var output by remember { mutableStateOf("Editor prototipe. Runtime .cuk penuh belum terhubung.") }
 var reduceMotion by remember { mutableStateOf(true) }
 val scope= rememberCoroutineScope()
 LaunchedEffect(search,page) { notes=withContext(Dispatchers.IO) { db.searchNotes(search) } }
 Box(Modifier.fillMaxSize().background(Slate)) {
  NeuralBackdrop(Modifier.fillMaxSize(),reduceMotion)
  Column(Modifier.fillMaxSize().padding(horizontal=17.dp,vertical=12.dp)) {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.clickable { page=Page.HOME }) {
     Text("LYNOS",color=Ink,fontSize=20.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=3.sp)
     Text("NEURAL WORKSPACE / 0.2",color=Muted,fontSize=8.sp,letterSpacing=1.3.sp)
    }
    Spacer(Modifier.weight(1f))
    Text(SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date()),color=Cyan,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
   }
   Spacer(Modifier.height(10.dp))
   Row(verticalAlignment=Alignment.CenterVertically) {
    Box(Modifier.size(6.dp).background(Cyan,CircleShape)); Spacer(Modifier.width(7.dp))
    Text(page.name.replace('_',' '),color=Muted,fontSize=9.sp,letterSpacing=1.6.sp)
    Spacer(Modifier.weight(1f)); Text("LOCAL-FIRST",color=Muted,fontSize=8.sp)
   }
   Spacer(Modifier.height(12.dp))
   when(page) {
    Page.HOME -> Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(10.dp)) {
     Text("PERSONAL KNOWLEDGE SYSTEM",color=Cyan,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.4.sp)
     Text("Thoughts form\na living network.",color=Ink,fontSize=30.sp,lineHeight=34.sp,fontWeight=FontWeight.SemiBold)
     Text("A quiet workspace for ideas, code, and connections.",color=Muted,fontSize=11.sp)
     NeuralLandscape(Modifier.fillMaxWidth().weight(1f).heightIn(min=150.dp))
     Text("MNEMOS FIELD  ·  "+notes.size+" notes stored on device",color=Ink,fontSize=10.sp)
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
      Tile("⌘","MNEMOS","Notes + graph",Modifier.weight(1f)) { page=Page.MNEMOS; search="" }
      Tile("{ }","CUK STUDIO","Code editor",Modifier.weight(1f)) { page=Page.CUK }
     }
     Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
      Tile("⌕","SEARCH","Find notes",Modifier.weight(1f)) { page=Page.MNEMOS; search="" }
      Tile("⚙","SETTINGS","Appearance",Modifier.weight(1f)) { page=Page.SETTINGS }
      Tile("◉","STATUS","Device info",Modifier.weight(1f)) { page=Page.STATUS }
     }
    }
    Page.MNEMOS -> {
     Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(7.dp)) {
      Text("MNEMOS / KNOWLEDGE GRAPH",color=Cyan,fontSize=10.sp,fontWeight=FontWeight.Bold)
      Field(search,{search=it},"Search notes…",true)
      Row(horizontalArrangement=Arrangement.spacedBy(5.dp)) {
       Button(onClick={title="";body="";editId=null;selectedId=null;message=""},contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)){Text("New",fontSize=11.sp)}
       Button(onClick={scope.launch {
        if(title.isBlank()) message="Judul catatan wajib diisi."
        else try { val id=withContext(Dispatchers.IO){db.saveNote(title,body,editId)}; editId=id;selectedId=id;notes=withContext(Dispatchers.IO){db.searchNotes(search)};message="Catatan tersimpan lokal." } catch(e:Exception){message=e.message ?: "Gagal menyimpan."}
       }},contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)){Text(if(editId==null)"Save" else "Update",fontSize=11.sp)}
       OutlinedButton(onClick={scope.launch {
        val from=selectedId; val to=notes.firstOrNull{it.id!=from}?.id
        if(from==null||to==null) message="Pilih catatan dan buat catatan kedua dahulu."
        else try{withContext(Dispatchers.IO){db.linkNotes(from,to)};message="Relasi antar-node tersimpan."}catch(e:Exception){message=e.message ?: "Relasi gagal."}
       }},contentPadding=PaddingValues(horizontal=8.dp,vertical=6.dp)){Text("Link",fontSize=11.sp)}
      }
      Field(title,{title=it},"Judul catatan",true)
      Field(body,{body=it},"Isi catatan / pengetahuan…",false,Modifier.fillMaxWidth().height(76.dp))
      if(message.isNotBlank()) Text(message,color=Cyan,fontSize=10.sp)
      Text("NOTES / "+notes.size,color=Muted,fontSize=9.sp,letterSpacing=1.2.sp)
      LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
       items(notes,key={it.id}) { n ->
        Column(Modifier.fillMaxWidth().border(1.dp,if(editId==n.id)Cyan.copy(alpha=.65f) else Blue.copy(alpha=.25f),RoundedCornerShape(11.dp)).background(Panel,RoundedCornerShape(11.dp)).clickable{title=n.title;body=n.body;editId=n.id;selectedId=n.id}.padding(10.dp)) {
         Text(n.title,color=Ink,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
         if(n.body.isNotBlank()) Text(n.body,color=Muted,fontSize=10.sp,maxLines=2)
         Text("NODE #"+n.id+" · "+SimpleDateFormat("MMM d, HH:mm",Locale.getDefault()).format(Date(n.updatedAt)),color=Cyan,fontSize=8.sp)
        }
       }
      }
      selectedId?.let { id ->
       val related by produceState(emptyList<KnowledgeNote>(),id) { value=withContext(Dispatchers.IO){db.relatedNotes(id)} }
       if(related.isNotEmpty()) Text("CONNECTED: "+related.joinToString(" · "){it.title},color=Cyan,fontSize=9.sp,maxLines=2)
      }
     }
    }
    Page.CUK -> Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(9.dp)) {
     Text("CUK STUDIO / EDITOR",color=Cyan,fontSize=10.sp,fontWeight=FontWeight.Bold)
     Text("Editor lokal prototipe; runtime bahasa penuh belum tersambung.",color=Muted,fontSize=10.sp)
     Box(Modifier.weight(1f).fillMaxWidth().border(1.dp,Blue.copy(alpha=.4f),RoundedCornerShape(13.dp)).background(Color(0xFF111D2A),RoundedCornerShape(13.dp)).padding(11.dp)) {
      BasicTextField(code,{code=it},Modifier.fillMaxSize(),textStyle=TextStyle(color=Color(0xFFBFEFFF),fontSize=12.sp,lineHeight=18.sp))
     }
     Button(onClick={
      output=if(code.trim().startsWith("jancuk")&&code.trim().endsWith("selesai jancuk")) "Wrapper source valid. Pemeriksaan struktur saja—bukan eksekusi runtime .cuk."
      else "Gagal: source harus diawali jancuk dan diakhiri selesai jancuk."
     },modifier=Modifier.fillMaxWidth()){Text("Check source")}
     Text(output,color=Ink,fontSize=11.sp)
    }
    Page.SETTINGS -> Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(13.dp)) {
     Text("SYSTEM SETTINGS",color=Cyan,fontWeight=FontWeight.Bold)
     Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(13.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
      Column(Modifier.weight(1f)){Text("Reduced motion",color=Ink,fontWeight=FontWeight.SemiBold);Text("Kurangi animasi untuk hemat daya",color=Muted,fontSize=10.sp)}
      Switch(reduceMotion,{reduceMotion=it})
     }
     StatusRow("Database","SQLite offline di perangkat","LOCAL")
     StatusRow("Cloud sync","Belum dikonfigurasi","OFF")
    }
    Page.STATUS -> {
     val battery=remember{(context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager).getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0,100)}
     Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(11.dp)) {
      Text("DEVICE STATUS",color=Cyan,fontWeight=FontWeight.Bold)
      StatusRow("Battery","Nilai yang dilaporkan Android",battery.toString()+"%")
      StatusRow("Knowledge storage","SQLite lokal","ON DEVICE")
      StatusRow("Notifications","LynOS tidak membaca notifikasi aplikasi lain tanpa Notification Listener access","PERMISSION")
      StatusRow("Clock", "Waktu lokal perangkat",SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date()))
     }
    }
   }
   Spacer(Modifier.height(7.dp))
   Row(Modifier.fillMaxWidth().border(1.dp,Blue.copy(alpha=.3f),RoundedCornerShape(14.dp)).background(Color(0xE51A293B),RoundedCornerShape(14.dp)).padding(4.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
    listOf(Page.HOME to "Home",Page.MNEMOS to "Mnemos",Page.CUK to "Cuk",Page.STATUS to "Status",Page.SETTINGS to "Settings").forEach { (p,label) ->
     Column(Modifier.weight(1f).background(if(page==p)Color(0x4438CFFF) else Color.Transparent,RoundedCornerShape(9.dp)).clickable{page=p}.padding(vertical=9.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(label,color=if(page==p)Cyan else Muted,fontSize=9.sp)}
    }
   }
  }
 }
}

@Composable private fun Tile(glyph:String,label:String,desc:String,modifier:Modifier,click:()->Unit) {
 Column(modifier.border(1.dp,Blue.copy(alpha=.35f),RoundedCornerShape(15.dp)).background(Brush.verticalGradient(listOf(Color(0xEE263A50),Color(0xEE1B2A3D))),RoundedCornerShape(15.dp)).clickable(onClick=click).padding(9.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){
  Text(glyph,color=Cyan,fontSize=18.sp);Text(label,color=Ink,fontSize=9.sp,fontWeight=FontWeight.Bold,maxLines=1);Text(desc,color=Muted,fontSize=8.sp,maxLines=1)
 }
}
@Composable private fun Field(value:String,change:(String)->Unit,hint:String,single:Boolean,modifier:Modifier=Modifier){
 Box(modifier.fillMaxWidth().border(1.dp,Blue.copy(alpha=.4f),RoundedCornerShape(11.dp)).background(Color(0xCC172536),RoundedCornerShape(11.dp)).padding(horizontal=11.dp,vertical=9.dp)){
  if(value.isEmpty()) Text(hint,color=Muted,fontSize=12.sp)
  BasicTextField(value,change,Modifier.fillMaxWidth(),singleLine=single,textStyle=TextStyle(color=Ink,fontSize=12.sp,lineHeight=17.sp))
 }
}
@Composable private fun StatusRow(title:String,description:String,value:String){
 Row(Modifier.fillMaxWidth().border(1.dp,Blue.copy(alpha=.3f),RoundedCornerShape(13.dp)).background(Panel,RoundedCornerShape(13.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically){
  Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)){Text(title,color=Ink,fontWeight=FontWeight.SemiBold,fontSize=12.sp);Text(description,color=Muted,fontSize=10.sp)}
  Text(value,color=Cyan,fontSize=9.sp,fontWeight=FontWeight.Bold)
 }
}
@Composable private fun NeuralBackdrop(modifier:Modifier,reduced:Boolean){
 val transition=androidx.compose.animation.core.rememberInfiniteTransition(label="neural")
 val phase by transition.animateFloat(0f,1f,androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(if(reduced)26000 else 12000)),label="phase")
 Canvas(modifier){
  drawRect(Brush.verticalGradient(listOf(Navy,Slate,Color(0xFF111A27))))
  val center=Offset(size.width*.67f,size.height*.48f)
  drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha=.1f),Blue.copy(alpha=.035f),Color.Transparent),center,size.maxDimension*.75f),size.maxDimension*.75f,center)
  val pts=nodePoints.map{Offset(it.first*size.width,it.second*size.height)}
  nodeEdges.forEachIndexed{i,e->val wave=if(reduced).5f else (sin(phase*6.283f+i*.48f)+1f)/2f;drawLine(if(i%4==0)Cyan.copy(alpha=.12f+wave*.13f)else Blue.copy(alpha=.15f+wave*.08f),pts[e.first],pts[e.second],if(i%4==0)1.dp.toPx() else .65.dp.toPx(),cap=StrokeCap.Round)}
  pts.forEachIndexed{i,p->val pulse=if(reduced)1f else .8f+.2f*sin(phase*6.283f+i);drawCircle(Cyan.copy(alpha=.04f),8.dp.toPx()*pulse,p);drawCircle(if(i%7==0)Color(0xFFD4F7FF)else Cyan.copy(alpha=.8f),(1.2f+(i%3)*.4f).dp.toPx()*pulse,p)}
 }
}
@Composable private fun NeuralLandscape(modifier:Modifier){NeuralBackdrop(modifier,true)}
private val nodePoints=listOf(.02f to .40f,.06f to .15f,.09f to .73f,.14f to .31f,.16f to .55f,.20f to .86f,.23f to .11f,.26f to .39f,.29f to .65f,.32f to .22f,.35f to .49f,.38f to .82f,.41f to .12f,.44f to .34f,.47f to .62f,.50f to .87f,.53f to .21f,.56f to .46f,.59f to .72f,.62f to .10f,.65f to .36f,.68f to .57f,.71f to .84f,.74f to .22f,.77f to .43f,.80f to .66f,.83f to .12f,.86f to .34f,.89f to .56f,.92f to .81f,.96f to .25f,.99f to .49f)
private val nodeEdges=listOf(0 to 1,0 to 2,0 to 3,1 to 3,1 to 6,2 to 4,2 to 5,3 to 4,3 to 6,3 to 7,4 to 7,4 to 8,5 to 8,6 to 7,6 to 9,7 to 8,7 to 9,7 to 10,8 to 10,8 to 11,9 to 10,9 to 12,10 to 11,10 to 13,10 to 14,11 to 14,11 to 15,12 to 13,12 to 16,13 to 14,13 to 16,14 to 15,14 to 16,14 to 17,15 to 18,15 to 19,16 to 17,16 to 19,17 to 18,17 to 20,18 to 19,18 to 21,18 to 22,19 to 22,20 to 21,20 to 23,21 to 22,21 to 23,21 to 24,22 to 24,22 to 25,22 to 26,23 to 24,23 to 26,24 to 25,24 to 27,25 to 26,25 to 28,26 to 28,26 to 29,27 to 28,27 to 30,28 to 29,28 to 30,29 to 31,30 to 31)
