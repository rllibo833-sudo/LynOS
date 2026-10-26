package org.lynos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
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
private enum class Page { HOME, APPS, MNEMOS, CUK, SETTINGS, STATUS }

@Composable
private fun LynOSApp(context: Context) {
 val db=remember { MnemosDatabase(context) }
 var page by remember { mutableStateOf(Page.HOME) }
 var search by remember { mutableStateOf("") }
 var appQuery by remember { mutableStateOf("") }
 val launchableApps = remember(context) {
  val pm = context.packageManager
  val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  @Suppress("DEPRECATION")
  pm.queryIntentActivities(intent, 0).mapNotNull { info ->
   val appInfo = info.activityInfo?.applicationInfo ?: return@mapNotNull null
   if (appInfo.packageName == context.packageName) null
   else LaunchableApp(info.loadLabel(pm).toString(), appInfo.packageName, info.loadIcon(pm))
  }.distinctBy { it.packageName }.sortedBy { it.label.lowercase(Locale.ROOT) }
 }
 var title by remember { mutableStateOf("") }
 var body by remember { mutableStateOf("") }
 var editId by remember { mutableStateOf<Long?>(null) }
 var selectedId by remember { mutableStateOf<Long?>(null) }
 var linkTargetId by remember { mutableStateOf<Long?>(null) }
 var linkMenuOpen by remember { mutableStateOf(false) }
 var notes by remember { mutableStateOf(emptyList<KnowledgeNote>()) }
 var allNotes by remember { mutableStateOf(emptyList<KnowledgeNote>()) }
 var linkCount by remember { mutableIntStateOf(0) }
 var memoryCount by remember { mutableIntStateOf(0) }
 var dataRevision by remember { mutableIntStateOf(0) }
 val scope= rememberCoroutineScope()
 var message by remember { mutableStateOf("") }
 var pendingExport by remember { mutableStateOf("") }
 val jsonExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
  if(uri == null) { message = "Ekspor JSON dibatalkan." }
  else scope.launch {
   try { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(pendingExport.toByteArray(Charsets.UTF_8)) } ?: error("Tidak dapat membuka berkas tujuan.") }; message = "Ekspor JSON berhasil disimpan." }
   catch(e: Exception) { message = "Ekspor gagal: ${e.message ?: "kesalahan tulis"}" }
  }
 }
 val jsonImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
  if (uri == null) message = "Impor JSON dibatalkan."
  else scope.launch {
   try {
    val snapshot = withContext(Dispatchers.IO) {
     context.contentResolver.openInputStream(uri)?.use { input ->
      val output = java.io.ByteArrayOutputStream()
      val buffer = ByteArray(8192)
      var total = 0
      while (true) {
       val count = input.read(buffer)
       if (count < 0) break
       total += count
       require(total <= 10 * 1024 * 1024) { "Berkas terlalu besar (maksimum 10 MiB)." }
       output.write(buffer, 0, count)
      }
      output.toString(Charsets.UTF_8.name())
     } ?: error("Tidak dapat membaca berkas sumber.")
    }
    val imported = withContext(Dispatchers.IO) { db.importJson(snapshot) }
    dataRevision++
    notes = withContext(Dispatchers.IO) { db.searchNotes(search) }
    allNotes = withContext(Dispatchers.IO) { db.searchNotes() }
    memoryCount = withContext(Dispatchers.IO) { db.countNotes() }
    linkCount = withContext(Dispatchers.IO) { db.countLinks() }
    message = "Restore selesai: ${imported.notesImported} catatan, ${imported.linksImported} relasi ditambahkan."
   } catch(e: Exception) { message = "Impor gagal; database tidak diubah: ${e.message ?: "berkas tidak valid"}" }
  }
 }
 val markdownExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
  if(uri == null) { message = "Ekspor Markdown dibatalkan." }
  else scope.launch {
   try { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri)?.use { it.write(pendingExport.toByteArray(Charsets.UTF_8)) } ?: error("Tidak dapat membuka berkas tujuan.") }; message = "Ekspor Markdown berhasil disimpan." }
   catch(e: Exception) { message = "Ekspor gagal: ${e.message ?: "kesalahan tulis"}" }
  }
 }
 var code by remember { mutableStateOf("jancuk\nhedeh \"Halo dari Cuk Studio\"\nselesai jancuk") }
 var output by remember { mutableStateOf("Terminal LynOS siap. Ketik `help` untuk perintah yang tersedia.") }
 var commandInput by remember { mutableStateOf("help") }
 var terminalHistory by remember { mutableStateOf(listOf("LYNOS CUK TERMINAL / local workspace commands only")) }
 val preferences = remember { context.getSharedPreferences("lynos_preferences", Context.MODE_PRIVATE) }
 var reduceMotion by remember { mutableStateOf(preferences.getBoolean("reduce_motion", false)) }
 var isDefaultHome by remember {
  mutableStateOf(
   Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
    context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
  )
 }
 val homeRoleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
  isDefaultHome = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
   context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
 }
 var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
 LaunchedEffect(Unit) {
  while (true) {
   nowMillis = System.currentTimeMillis()
   isDefaultHome = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
    context.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
   delay(1000)
  }
 }
 LaunchedEffect(search,page,dataRevision) {
  notes=withContext(Dispatchers.IO) { db.searchNotes(search) }
  allNotes=withContext(Dispatchers.IO) { db.searchNotes() }
  linkCount=withContext(Dispatchers.IO) { db.countLinks() }
  memoryCount=withContext(Dispatchers.IO) { db.countNotes() }
 }
 Box(Modifier.fillMaxSize().background(Slate)) {
  NeuralBackdrop(Modifier.fillMaxSize(),reduceMotion)
  Column(Modifier.fillMaxSize().padding(horizontal=17.dp,vertical=12.dp).imePadding()) {
   Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
    Column(Modifier.clickable { page=Page.HOME }) {
     Text("LYNOS",color=Ink,fontSize=20.sp,fontWeight=FontWeight.ExtraBold,letterSpacing=3.sp)
     Text("NEURAL WORKSPACE / 0.2",color=Muted,fontSize=8.sp,letterSpacing=1.3.sp)
    }
    Spacer(Modifier.weight(1f))
    Text(SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date(nowMillis)),color=Cyan,fontSize=15.sp,fontWeight=FontWeight.SemiBold)
   }
   Spacer(Modifier.height(10.dp))
   Row(verticalAlignment=Alignment.CenterVertically) {
    Box(Modifier.size(6.dp).background(Cyan,CircleShape)); Spacer(Modifier.width(7.dp))
    Text(page.name.replace('_',' '),color=Muted,fontSize=9.sp,letterSpacing=1.6.sp)
    Spacer(Modifier.weight(1f)); Text("LOCAL-FIRST",color=Muted,fontSize=8.sp)
   }
   Row(
    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
    horizontalArrangement=Arrangement.spacedBy(6.dp),
    verticalAlignment=Alignment.CenterVertically
   ) {
    Page.values().forEach { destination ->
     val active = page == destination
     Box(
      Modifier.border(1.dp, if(active) Cyan.copy(alpha=.72f) else Blue.copy(alpha=.28f), RoundedCornerShape(9.dp))
       .background(if(active) Cyan.copy(alpha=.12f) else Panel.copy(alpha=.72f), RoundedCornerShape(9.dp))
       .clickable { page = destination }
       .padding(horizontal=11.dp, vertical=8.dp)
     ) {
      Text(
       destination.name.replace('_',' '),
       color=if(active) Cyan else Muted,
       fontSize=9.sp,
       fontWeight=if(active) FontWeight.Bold else FontWeight.Medium,
       letterSpacing=.55.sp
      )
     }
    }
   }
   Spacer(Modifier.height(12.dp))
   Box(Modifier.weight(1f).fillMaxWidth()) {
    when(page) {
    Page.HOME -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)) {
     Text("PERSONAL KNOWLEDGE SYSTEM",color=Cyan,fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.4.sp)
     Text("Thoughts form\na living network.",color=Ink,fontSize=30.sp,lineHeight=34.sp,fontWeight=FontWeight.SemiBold)
     Text("A quiet workspace for ideas, code, and connections.",color=Muted,fontSize=11.sp)
     if (!isDefaultHome) {
      Button(
       onClick = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
         val roleManager = context.getSystemService(RoleManager::class.java)
         if (roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
          homeRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
         } else {
          context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
         }
        } else {
         context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
       },
       modifier = Modifier.fillMaxWidth(),
       colors = ButtonDefaults.buttonColors(containerColor = Cyan.copy(alpha = .16f), contentColor = Cyan)
      ) {
       Text("Jadikan launcher utama", fontWeight = FontWeight.Bold)
      }
     }
     NeuralLandscape(Modifier.fillMaxWidth().height(210.dp), reduceMotion)
     Text("MNEMOS FIELD  ·  "+memoryCount+" notes stored on device",color=Ink,fontSize=10.sp)
     Row(horizontalArrangement=Arrangement.spacedBy(7.dp)) {
      MetricCard("MEMORIES", memoryCount.toString(), "Saved locally", Modifier.weight(1f))
      MetricCard("RELATIONS", linkCount.toString(), "Knowledge links", Modifier.weight(1f))
      MetricCard("STORAGE", "LOCAL", "SQLite offline", Modifier.weight(1f))
     }
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
    Page.APPS -> Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(9.dp)) {
     Text("APP SPACE / ANDROID LAUNCHER",color=Cyan,fontSize=10.sp,fontWeight=FontWeight.Bold)
     Text("Buka aplikasi terpasang. Android tetap mengelola task dan multitasking.",color=Muted,fontSize=10.sp)
     Field(appQuery,{appQuery=it},"Cari aplikasi…",true)
     val filteredApps = launchableApps.filter { it.label.contains(appQuery,true) || it.packageName.contains(appQuery,true) }
     Text("INSTALLED APPS · ${filteredApps.size}",color=Muted,fontSize=9.sp)
     LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(6.dp)) {
      items(filteredApps,key={it.packageName}) { app ->
       Row(Modifier.fillMaxWidth().border(1.dp,Blue.copy(alpha=.32f),RoundedCornerShape(12.dp)).background(Panel,RoundedCornerShape(12.dp)).clickable {
        try {
         val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
         if(launch != null) context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) else message = "Android tidak menyediakan tombol buka untuk ${app.label}."
        } catch(e:Exception) { message="Gagal membuka ${app.label}: ${e.message ?: "kesalahan Android"}" }
       }.padding(10.dp),verticalAlignment=Alignment.CenterVertically) {
         val iconBitmap = remember(app.packageName) {
          Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).also { bitmap ->
           val canvas = AndroidCanvas(bitmap)
           app.icon.setBounds(0, 0, canvas.width, canvas.height)
           app.icon.draw(canvas)
          }.asImageBitmap()
         }
         androidx.compose.foundation.Image(bitmap=iconBitmap,contentDescription=app.label,modifier=Modifier.size(36.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
         Text(app.label,color=Ink,fontSize=12.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
         Text(app.packageName,color=Muted,fontSize=8.sp,maxLines=1)
        }
        Text("OPEN ↗",color=Cyan,fontSize=9.sp,fontWeight=FontWeight.Bold)
       }
      }
     }
     if(message.isNotBlank()) Text(message,color=Cyan,fontSize=10.sp)
    }
    Page.MNEMOS -> {
     Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(7.dp)) {
      Text("MNEMOS / KNOWLEDGE GRAPH",color=Cyan,fontSize=10.sp,fontWeight=FontWeight.Bold)
      Field(search,{search=it},"Search notes…",true)
      Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(5.dp)) {
       Button(onClick={title="";body="";editId=null;selectedId=null;linkTargetId=null;message=""},contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)){Text("New",fontSize=11.sp)}
       Button(onClick={scope.launch {
        if(title.isBlank()) message="Judul catatan wajib diisi."
        else try { val id=withContext(Dispatchers.IO){db.saveNote(title,body,editId)}; editId=id;selectedId=id;dataRevision++;notes=withContext(Dispatchers.IO){db.searchNotes(search)};allNotes=withContext(Dispatchers.IO){db.searchNotes()};message="Catatan tersimpan lokal." } catch(e:Exception){message=e.message ?: "Gagal menyimpan."}
       }},contentPadding=PaddingValues(horizontal=10.dp,vertical=6.dp)){Text(if(editId==null)"Save" else "Update",fontSize=11.sp)}
       OutlinedButton(onClick={scope.launch { try { pendingExport=withContext(Dispatchers.IO){db.exportJson()}; jsonExportLauncher.launch("mnemos-export.json") } catch(e:Exception){message="Ekspor JSON gagal: ${e.message ?: "kesalahan"}"} }},contentPadding=PaddingValues(horizontal=8.dp,vertical=6.dp)){Text("JSON ↓",fontSize=11.sp)}
       OutlinedButton(onClick={jsonImportLauncher.launch(arrayOf("application/json","text/*"))},contentPadding=PaddingValues(horizontal=8.dp,vertical=6.dp)){Text("JSON ↑",fontSize=11.sp)}
       OutlinedButton(onClick={scope.launch { try { pendingExport=withContext(Dispatchers.IO){db.exportMarkdown()}; markdownExportLauncher.launch("mnemos-export.md") } catch(e:Exception){message="Ekspor Markdown gagal: ${e.message ?: "kesalahan"}"} }},contentPadding=PaddingValues(horizontal=8.dp,vertical=6.dp)){Text("MD ↓",fontSize=11.sp)}
       Box {
        OutlinedButton(onClick={linkMenuOpen=true},contentPadding=PaddingValues(horizontal=8.dp,vertical=6.dp)){Text(allNotes.firstOrNull{it.id==linkTargetId}?.title?.take(9) ?: "Target ▾",fontSize=11.sp,maxLines=1)}
        DropdownMenu(expanded=linkMenuOpen,onDismissRequest={linkMenuOpen=false}) {
         allNotes.filter { it.id != selectedId }.forEach { candidate ->
          DropdownMenuItem(text={Text(candidate.title,maxLines=1)},onClick={linkTargetId=candidate.id;linkMenuOpen=false;message="Target relasi: ${candidate.title}"})
         }
        }
       }
       OutlinedButton(onClick={scope.launch {
        val from=selectedId; val to=linkTargetId
        if(from==null) message="Pilih catatan sumber dahulu."
        else if(to==null || to==from) message="Pilih target relasi yang berbeda dari catatan sumber."
        else try{withContext(Dispatchers.IO){db.linkNotes(from,to)};dataRevision++;message="Relasi tersimpan: #${from} → #${to}";notes=withContext(Dispatchers.IO){db.searchNotes(search)};allNotes=withContext(Dispatchers.IO){db.searchNotes()}}catch(e:Exception){message=e.message ?: "Relasi gagal."}
       }},contentPadding=PaddingValues(horizontal=8.dp,vertical=6.dp)){Text("Link",fontSize=11.sp)}
      }
      Field(title,{title=it},"Judul catatan",true)
      Field(body,{body=it},"Isi catatan / pengetahuan…",false,Modifier.fillMaxWidth().height(76.dp))
      if(message.isNotBlank()) Text(message,color=Cyan,fontSize=10.sp)
      Text("NOTES / "+notes.size,color=Muted,fontSize=9.sp,letterSpacing=1.2.sp)
      val selectedRelated by produceState(emptyList<KnowledgeNote>(),selectedId,dataRevision) {
       value = selectedId?.let { id -> withContext(Dispatchers.IO) { db.relatedNotes(id) } } ?: emptyList()
      }
      if(selectedRelated.isNotEmpty()) {
       Text("CONNECTED NOTES · tap to open",color=Muted,fontSize=8.sp,letterSpacing=.7.sp)
       Column(verticalArrangement=Arrangement.spacedBy(4.dp)) {
        selectedRelated.forEach { relatedNote ->
         Row(Modifier.fillMaxWidth().border(1.dp,Cyan.copy(alpha=.18f),RoundedCornerShape(9.dp)).background(Panel,RoundedCornerShape(9.dp)).padding(start=9.dp,end=4.dp,top=3.dp,bottom=3.dp),verticalAlignment=Alignment.CenterVertically) {
          Text(relatedNote.title,color=Cyan,fontSize=10.sp,maxLines=1,modifier=Modifier.weight(1f).clickable {
           title=relatedNote.title
           body=relatedNote.body
           editId=relatedNote.id
           selectedId=relatedNote.id
           linkTargetId=null
           message="Catatan terhubung dibuka."
          }.padding(vertical=7.dp))
          TextButton(onClick={scope.launch {
           try {
             val fromId = selectedId
             if (fromId == null) {
              message = "Pilih catatan sumber dahulu."
             } else {
              withContext(Dispatchers.IO) { db.unlinkNotes(fromId,relatedNote.id) }
            dataRevision++
            message="Relasi dilepas; kedua catatan tetap tersimpan."
             }
           } catch(e:Exception) { message=e.message ?: "Relasi gagal dilepas." }
          }},contentPadding=PaddingValues(horizontal=7.dp,vertical=2.dp)) { Text("Unlink",fontSize=9.sp) }
         }
        }
       }
      }
      if(notes.isEmpty()) {
       Column(Modifier.fillMaxWidth().padding(vertical=18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(5.dp)) {
        Text(if(search.isBlank()) "MNEMOS IS QUIET" else "NO MATCHING NODES",color=Cyan,fontSize=10.sp,fontWeight=FontWeight.Bold,letterSpacing=1.sp)
        Text(if(search.isBlank()) "Buat catatan pertama untuk mulai membangun jaringan pengetahuan." else "Coba kata lain atau hapus pencarian.",color=Muted,fontSize=10.sp)
        if(search.isNotBlank()) TextButton(onClick={search=""}) { Text("Clear search",color=Cyan,fontSize=10.sp) }
       }
      }
      LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
       items(notes,key={it.id}) { n ->
        Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(3.dp)) {
         Column(Modifier.fillMaxWidth().border(1.dp,if(editId==n.id)Cyan.copy(alpha=.65f) else Blue.copy(alpha=.25f),RoundedCornerShape(11.dp)).background(Panel,RoundedCornerShape(11.dp)).clickable{title=n.title;body=n.body;editId=n.id;selectedId=n.id;if(linkTargetId==n.id) linkTargetId=null;message=""}.padding(10.dp)) {
          Text(n.title,color=Ink,fontSize=12.sp,fontWeight=FontWeight.SemiBold)
          if(n.body.isNotBlank()) Text(n.body,color=Muted,fontSize=10.sp,maxLines=2)
          Text("NODE #"+n.id+" · "+SimpleDateFormat("MMM d, HH:mm",Locale.getDefault()).format(Date(n.updatedAt)),color=Cyan,fontSize=8.sp)
         }
         if(selectedId==n.id) {
          OutlinedButton(onClick={scope.launch {
           try {
            withContext(Dispatchers.IO){db.deleteNote(n.id)}
            dataRevision++
            if(linkTargetId==n.id) linkTargetId=null
            notes=withContext(Dispatchers.IO){db.searchNotes(search)}
            allNotes=withContext(Dispatchers.IO){db.searchNotes()}
            if(editId==n.id){title="";body="";editId=null}
            selectedId=null
            message="Catatan dihapus. Relasi terkait ikut dibersihkan."
           } catch(e:Exception) { message=e.message ?: "Catatan gagal dihapus." }
          }},modifier=Modifier.align(Alignment.End),contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp)) { Text("Hapus catatan",fontSize=10.sp) }
         }
        }
       }
      }
     }
    }
    Page.CUK -> Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(7.dp)) {
     Text("CUK STUDIO / TERMINAL",color=Cyan,fontSize=10.sp,fontWeight=FontWeight.Bold)
     Text("Editor .cuk + perintah workspace lokal. Ini bukan shell OS dan belum mengeksekusi source .cuk.",color=Muted,fontSize=9.sp,lineHeight=13.sp)
     Text("SOURCE EDITOR",color=Muted,fontSize=8.sp,fontWeight=FontWeight.Bold)
     Box(Modifier.weight(0.78f).fillMaxWidth().border(1.dp,Blue.copy(alpha=.4f),RoundedCornerShape(13.dp)).background(Color(0xFF111D2A),RoundedCornerShape(13.dp)).padding(9.dp)) {
      BasicTextField(code,{code=it},Modifier.fillMaxSize(),textStyle=TextStyle(color=Color(0xFFBFEFFF),fontSize=11.sp,lineHeight=16.sp))
     }
     Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
      Button(onClick={
       val lines = code.lines().map { it.trim() }.filter { it.isNotEmpty() }
       output = when {
        lines.size < 3 -> "Source ditolak: butuh wrapper dan minimal satu baris program."
        lines.first() != "jancuk" -> "Source ditolak: baris pertama harus jancuk."
        lines.last() != "selesai jancuk" -> "Source ditolak: baris terakhir harus selesai jancuk."
        lines.drop(1).dropLast(1).any { it == "jancuk" || it == "selesai jancuk" } -> "Wrapper berulang terdeteksi."
        else -> "Struktur wrapper valid. Source BELUM dieksekusi; runtime .cuk belum terhubung."
       }
       terminalHistory = (terminalHistory + "> check-source" + output).takeLast(60)
      },modifier=Modifier.weight(1f)){Text("Check source",fontSize=11.sp)}
      OutlinedButton(onClick={code="jancuk\nhedeh \"Halo dari Cuk Studio\"\nselesai jancuk";output="Template .cuk dimuat.";terminalHistory=terminalHistory+"> template halo"},modifier=Modifier.weight(1f)){Text("Template",fontSize=11.sp)}
     }
     Text("TERMINAL",color=Muted,fontSize=8.sp,fontWeight=FontWeight.Bold)
     LazyColumn(Modifier.weight(0.55f).fillMaxWidth().border(1.dp,Blue.copy(alpha=.35f),RoundedCornerShape(10.dp)).background(Color(0xFF0D1722),RoundedCornerShape(10.dp)).padding(8.dp),verticalArrangement=Arrangement.spacedBy(3.dp)) {
      items(terminalHistory.takeLast(12)) { line -> Text(line,color=if(line.startsWith(">")) Cyan else Ink,fontSize=9.sp,lineHeight=12.sp) }
     }
     Row(horizontalArrangement=Arrangement.spacedBy(6.dp),verticalAlignment=Alignment.CenterVertically) {
      Field(commandInput,{commandInput=it},"help / notes / find teks",true,Modifier.weight(1f))
      Button(onClick={scope.launch {
       val raw=commandInput.trim()
       val parts=raw.split(" ",limit=2)
       val result=try {
        when(parts.firstOrNull()?.lowercase(Locale.ROOT)) {
         "",null -> "Perintah kosong. Ketik help."
         "help" -> "Perintah: help, date, status, notes, links, find <teks>, note <judul> :: <isi>, clear. Perintah lokal LynOS; bukan shell OS."
         "date" -> SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.getDefault()).format(Date(System.currentTimeMillis()))
         "status" -> "LynOS local-first | notes=${withContext(Dispatchers.IO){db.countNotes()}} | links=${withContext(Dispatchers.IO){db.countLinks()}}"
         "notes" -> withContext(Dispatchers.IO){db.searchNotes()}.take(8).let { found -> if(found.isEmpty()) "Belum ada catatan." else found.joinToString("\n") { "#${it.id} ${it.title}" } }
         "links" -> "Relasi tersimpan: ${withContext(Dispatchers.IO){db.countLinks()}}"
         "find" -> if(parts.size<2 || parts[1].isBlank()) "Pakai: find <teks>" else withContext(Dispatchers.IO){db.searchNotes(parts[1])}.take(8).let { found -> if(found.isEmpty()) "Tidak ditemukan." else found.joinToString("\n") { "#${it.id} ${it.title}" } }
         "note" -> {
          val payload=raw.substringAfter(" ", "").split("::",limit=2)
          if(payload.size<2 || payload[0].isBlank()) "Pakai: note Judul :: isi catatan"
          else { val id=withContext(Dispatchers.IO){db.saveNote(payload[0].trim(),payload[1].trim())}; dataRevision++; "Catatan #$id disimpan ke Mnemos." }
         }
         "clear" -> { terminalHistory=emptyList(); "Terminal dibersihkan." }
         else -> "Perintah tidak dikenal: ${parts[0]}. Ketik help."
        }
       } catch(e:Exception) { "Gagal: ${e.message ?: "operasi tidak berhasil"}" }
       if(raw.lowercase(Locale.ROOT)=="clear") {
        terminalHistory=listOf("LYNOS CUK TERMINAL / local workspace commands only")
       } else {
        terminalHistory=(terminalHistory + "> $raw" + result).takeLast(60)
       }
       output=result
       commandInput=""
      }},modifier=Modifier.height(42.dp)){Text("Run")}
     }
    }
    Page.SETTINGS -> Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(13.dp)) {
     Text("SYSTEM SETTINGS",color=Cyan,fontWeight=FontWeight.Bold)
     Row(Modifier.fillMaxWidth().background(Panel,RoundedCornerShape(13.dp)).padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
      Column(Modifier.weight(1f)){Text("Reduced motion",color=Ink,fontWeight=FontWeight.SemiBold);Text("Kurangi animasi untuk hemat daya",color=Muted,fontSize=10.sp)}
      Switch(reduceMotion,{ enabled ->
       reduceMotion=enabled
       preferences.edit().putBoolean("reduce_motion",enabled).apply()
      })
     }
     StatusRow("Database","SQLite offline di perangkat","LOCAL")
     StatusRow("Cloud sync","Belum dikonfigurasi","OFF")
    }
    Page.STATUS -> {
     val battery by produceState(0) {
      value = withContext(Dispatchers.IO) {
       (context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager)
        .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0,100)
      }
     }
     Column(Modifier.fillMaxSize(),verticalArrangement=Arrangement.spacedBy(11.dp)) {
      Text("DEVICE STATUS",color=Cyan,fontWeight=FontWeight.Bold)
      StatusRow("Battery","Nilai yang dilaporkan Android",battery.toString()+"%")
      StatusRow("Knowledge storage","SQLite lokal","ON DEVICE")
      StatusRow("Notifications","LynOS tidak membaca notifikasi aplikasi lain tanpa Notification Listener access","PERMISSION")
      StatusRow("Clock", "Waktu lokal perangkat",SimpleDateFormat("HH:mm:ss",Locale.getDefault()).format(Date(nowMillis)))
     }
    }
   }
   }
   Spacer(Modifier.height(7.dp))
   Row(Modifier.fillMaxWidth().border(1.dp,Blue.copy(alpha=.3f),RoundedCornerShape(14.dp)).background(Color(0xE51A293B),RoundedCornerShape(14.dp)).padding(4.dp),horizontalArrangement=Arrangement.spacedBy(3.dp)) {
    listOf(Page.HOME to "Home",Page.APPS to "Apps",Page.MNEMOS to "Mnemos",Page.CUK to "Cuk",Page.STATUS to "Status",Page.SETTINGS to "Settings").forEach { (p,label) ->
     Column(Modifier.weight(1f).background(if(page==p)Color(0x4438CFFF) else Color.Transparent,RoundedCornerShape(9.dp)).clickable{page=p}.padding(vertical=9.dp),horizontalAlignment=Alignment.CenterHorizontally){Text(label,color=if(page==p)Cyan else Muted,fontSize=9.sp)}
    }
   }
  }
 }
}

private data class LaunchableApp(val label:String,val packageName:String,val icon:android.graphics.drawable.Drawable)

@Composable private fun MetricCard(label:String,value:String,description:String,modifier:Modifier) {
 Column(modifier.border(1.dp,Blue.copy(alpha=.32f),RoundedCornerShape(12.dp))
  .background(Panel,RoundedCornerShape(12.dp)).padding(9.dp),
  verticalArrangement=Arrangement.spacedBy(4.dp)) {
  Text(label,color=Muted,fontSize=8.sp,fontWeight=FontWeight.Bold,letterSpacing=.8.sp,maxLines=1)
  Text(value,color=Cyan,fontSize=16.sp,fontWeight=FontWeight.Bold,maxLines=1)
  Text(description,color=Muted,fontSize=8.sp,maxLines=1)
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
 val phase by transition.animateFloat(0f,if(reduced)0f else 1f,androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(if(reduced)26000 else 12000,easing=androidx.compose.animation.core.LinearEasing)),label="phase")
 Canvas(modifier){
  drawRect(Brush.verticalGradient(listOf(Navy,Slate,Color(0xFF111A27))))
  val center=Offset(size.width*.67f,size.height*.48f)
  drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha=.1f),Blue.copy(alpha=.035f),Color.Transparent),center,size.maxDimension*.75f),size.maxDimension*.75f,center)
  val pts=nodePoints.map{Offset(it.first*size.width,it.second*size.height)}
  nodeEdges.forEachIndexed{i,e->val wave=if(reduced).5f else (sin(phase*6.283f+i*.48f)+1f)/2f;drawLine(if(i%4==0)Cyan.copy(alpha=.12f+wave*.13f)else Blue.copy(alpha=.15f+wave*.08f),pts[e.first],pts[e.second],if(i%4==0)1.dp.toPx() else .65.dp.toPx(),cap=StrokeCap.Round)}
  pts.forEachIndexed{i,p->val pulse=if(reduced)1f else .8f+.2f*sin(phase*6.283f+i);drawCircle(Cyan.copy(alpha=.04f),8.dp.toPx()*pulse,p);drawCircle(if(i%7==0)Color(0xFFD4F7FF)else Cyan.copy(alpha=.8f),(1.2f+(i%3)*.4f).dp.toPx()*pulse,p)}
 }
}
@Composable private fun NeuralLandscape(modifier:Modifier,reduced:Boolean){
 val transition=androidx.compose.animation.core.rememberInfiniteTransition(label="landscape")
 val phase by transition.animateFloat(0f,if(reduced)0f else 1f,androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(if(reduced)30000 else 15000,easing=androidx.compose.animation.core.LinearEasing)),label="landscapePhase")
 Canvas(modifier){
  val w=size.width; val h=size.height
  val corner=androidx.compose.ui.geometry.CornerRadius(22.dp.toPx())
  drawRoundRect(Brush.verticalGradient(listOf(Color(0xEE263A50),Color(0xD8172638),Color(0xF0111C2A))),cornerRadius=corner)
  drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha=.12f),Blue.copy(alpha=.045f),Color.Transparent),Offset(w*.49f,h*.44f),w*.52f),w*.52f,Offset(w*.49f,h*.44f))
  drawCircle(Brush.radialGradient(listOf(Blue.copy(alpha=.11f),Color.Transparent),Offset(w*.88f,h*.72f),w*.34f),w*.34f,Offset(w*.88f,h*.72f))
  for(i in 0..7) drawLine(Blue.copy(alpha=.075f),Offset(w*.5f,h*.58f),Offset(w*(i/7f),h*.98f),.55.dp.toPx())
  for(i in 0..5){val t=i/5f;val y=h*(.66f+t*t*.30f);drawLine(Blue.copy(alpha=.065f+t*.025f),Offset(w*.02f,y),Offset(w*.98f,y),.55.dp.toPx())}
  val pts=nodePoints.mapIndexed{index,node->
   val angle=phase*6.283f+index*.83f
   val driftX=if(reduced)0f else sin(angle)*2.2.dp.toPx()
   val driftY=if(reduced)0f else sin(angle*.73f+1.1f)*1.8.dp.toPx()
   Offset(w*(.04f+node.first*.92f)+driftX,h*(.08f+node.second*.84f)+driftY)
  }
  nodeEdges.forEachIndexed{i,e->
   val a=pts[e.first];val b=pts[e.second];val dx=b.x-a.x;val dy=b.y-a.y
   val bend=Offset((a.x+b.x)/2f-dy*.10f,(a.y+b.y)/2f+dx*.10f)
   val path=androidx.compose.ui.graphics.Path().apply{moveTo(a.x,a.y);quadraticTo(bend.x,bend.y,b.x,b.y)}
   val important=i%5==0;val wave=if(reduced).45f else (.5f+.5f*sin(phase*6.283f+i*.31f))
   drawPath(path,if(important)Cyan.copy(alpha=.22f+wave*.23f)else Blue.copy(alpha=.14f+wave*.12f),style=androidx.compose.ui.graphics.drawscope.Stroke(width=if(important)1.05.dp.toPx() else .6.dp.toPx(),cap=StrokeCap.Round))
  }
  val hubs=listOf(Offset(w*.24f,h*.34f),Offset(w*.51f,h*.50f),Offset(w*.76f,h*.65f))
  hubs.forEachIndexed{i,p->
   val pulse=if(reduced)1f else .88f+.12f*sin(phase*6.283f+i)
   drawCircle(Brush.radialGradient(listOf(Cyan.copy(alpha=.16f),Cyan.copy(alpha=.035f),Color.Transparent),p,23.dp.toPx()*pulse),23.dp.toPx()*pulse,p)
   drawCircle(Cyan.copy(alpha=.10f),9.dp.toPx()*pulse,p)
  }
  pts.forEachIndexed{i,p->
   val near=i%6==0;val pulse=if(reduced)1f else .88f+.12f*sin(phase*6.283f+i*.7f)
   if(near)drawCircle(Cyan.copy(alpha=.13f),8.dp.toPx()*pulse,p)
   drawCircle(if(near)Color(0xFFD9F8FF)else Cyan.copy(alpha=.62f),if(near)1.8.dp.toPx() else .8.dp.toPx(),p)
  }
  repeat(9){i->
   val drift=if(reduced)0f else ((phase+i*.173f)%1f-.5f)*w*.035f
   val x=w*(.08f+i*.105f)+drift;val y=h*(.16f+((i*37)%67)/100f)
   val alpha=if(reduced).20f else .12f+.12f*sin(phase*6.283f+i)
   drawCircle(Cyan.copy(alpha=alpha),(1.1f+(i%3)*.35f).dp.toPx(),Offset(x,y))
  }
  drawRoundRect(Color.White.copy(alpha=.04f),topLeft=Offset(1.dp.toPx(),1.dp.toPx()),size=androidx.compose.ui.geometry.Size(w-2.dp.toPx(),h-2.dp.toPx()),cornerRadius=corner,style=androidx.compose.ui.graphics.drawscope.Stroke(width=1.dp.toPx()))
 }
}
private val nodePoints=listOf(.02f to .40f,.06f to .15f,.09f to .73f,.14f to .31f,.16f to .55f,.20f to .86f,.23f to .11f,.26f to .39f,.29f to .65f,.32f to .22f,.35f to .49f,.38f to .82f,.41f to .12f,.44f to .34f,.47f to .62f,.50f to .87f,.53f to .21f,.56f to .46f,.59f to .72f,.62f to .10f,.65f to .36f,.68f to .57f,.71f to .84f,.74f to .22f,.77f to .43f,.80f to .66f,.83f to .12f,.86f to .34f,.89f to .56f,.92f to .81f,.96f to .25f,.99f to .49f)
private val nodeEdges=listOf(0 to 1,0 to 2,0 to 3,1 to 3,1 to 6,2 to 4,2 to 5,3 to 4,3 to 6,3 to 7,4 to 7,4 to 8,5 to 8,6 to 7,6 to 9,7 to 8,7 to 9,7 to 10,8 to 10,8 to 11,9 to 10,9 to 12,10 to 11,10 to 13,10 to 14,11 to 14,11 to 15,12 to 13,12 to 16,13 to 14,13 to 16,14 to 15,14 to 16,14 to 17,15 to 18,15 to 19,16 to 17,16 to 19,17 to 18,17 to 20,18 to 19,18 to 21,18 to 22,19 to 22,20 to 21,20 to 23,21 to 22,21 to 23,21 to 24,22 to 24,22 to 25,22 to 26,23 to 24,23 to 26,24 to 25,24 to 27,25 to 26,25 to 28,26 to 28,26 to 29,27 to 28,27 to 30,28 to 29,28 to 30,29 to 31,30 to 31)
