package org.lynos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sin

private val MidnightSlate = Color(0xFF182333)
private val DeepNavy = Color(0xFF24364D)
private val NeuralCyan = Color(0xFF38CFFF)
private val NightBlue = Color(0xFF527DB8)
private val SoftText = Color(0xFFE7F0FC)
private val MutedText = Color(0xFF9EADC2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(24, 35, 51)
        window.navigationBarColor = android.graphics.Color.rgb(24, 35, 51)
        setContent { MaterialTheme { LynHome() } }
    }
}

@Composable
private fun LynHome() {
    val transition = rememberInfiniteTransition(label = "neural-field")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7200), repeatMode = RepeatMode.Restart),
        label = "field-phase"
    )
    BoxWithConstraints(Modifier.fillMaxSize().background(MidnightSlate)) {
        val availableWidth = maxWidth
        val availableHeight = maxHeight
        val landscape = availableWidth > availableHeight
        Box(Modifier.fillMaxSize()) {
            NeuralBackdrop(phase, Modifier.fillMaxSize())
            if (landscape) {
                Row(
                    Modifier.fillMaxSize().padding(start = 26.dp, end = 26.dp, top = 20.dp, bottom = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        Modifier.width(availableWidth * 0.36f).fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        BrandMark()
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Eyebrow("PERSONAL KNOWLEDGE SYSTEM")
                            Text("Ideas are\nalways connected.", color = SoftText, fontSize = 31.sp, lineHeight = 35.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp)
                            Text("A living space for thought.", color = MutedText, fontSize = 12.sp)
                        }
                        SystemStatus()
                    }
                    Box(Modifier.weight(1f).fillMaxSize()) {
                        NetworkLabels(Modifier.align(Alignment.Center), compact = false)
                        WorkspaceDock(Modifier.align(Alignment.BottomEnd).padding(bottom = 4.dp))
                    }
                }
            } else {
                Box(Modifier.fillMaxSize().padding(horizontal = 21.dp, vertical = 18.dp)) {
                    BrandMark(Modifier.align(Alignment.TopStart))
                    Column(
                        Modifier.align(Alignment.TopStart).padding(top = 104.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Eyebrow("PERSONAL KNOWLEDGE SYSTEM")
                        Text("Ideas are\nalways connected.", color = SoftText, fontSize = 29.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.8).sp)
                        Text("A living space for thought.", color = MutedText, fontSize = 12.sp)
                    }
                    NetworkLabels(Modifier.align(Alignment.Center).padding(top = 66.dp), compact = true)
                    Column(
                        Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        WorkspaceDock(Modifier.fillMaxWidth())
                        SystemStatus()
                    }
                }
            }
        }
    }
}

@Composable
private fun NeuralBackdrop(phase: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(Color(0xFF24364D), MidnightSlate, Color(0xFF121C29))))
        val glowCenter = Offset(size.width * 0.64f, size.height * 0.48f)
        drawCircle(
            Brush.radialGradient(listOf(NeuralCyan.copy(alpha = 0.12f), NightBlue.copy(alpha = 0.045f), Color.Transparent), glowCenter, size.maxDimension * 0.72f),
            size.maxDimension * 0.72f,
            glowCenter
        )
        val points = fieldNodes.map { Offset(it.x * size.width, it.y * size.height) }
        fieldEdges.forEachIndexed { i, edge ->
            val wave = (sin(phase * 6.283f + i * 0.73f) + 1f) * 0.5f
            drawLine(
                color = if (i % 5 == 0) NeuralCyan.copy(alpha = 0.17f + wave * 0.3f) else NightBlue.copy(alpha = 0.23f + wave * 0.24f),
                start = points[edge.first],
                end = points[edge.second],
                strokeWidth = if (i % 5 == 0) 1.25.dp.toPx() else 0.8.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        fieldNodes.forEachIndexed { i, node ->
            val pulse = 0.72f + (sin(phase * 6.283f * node.speed + i * 1.4f) + 1f) * 0.14f
            val p = points[i]
            val r = node.radius.dp.toPx()
            drawCircle(NeuralCyan.copy(alpha = 0.035f), r * 5.2f, p)
            drawCircle(NeuralCyan.copy(alpha = 0.07f), r * 3.1f * pulse, p)
            drawCircle(NightBlue.copy(alpha = 0.45f), r * 1.7f, p)
            drawCircle(if (node.core) Color(0xFFD4F7FF) else NeuralCyan.copy(alpha = 0.9f), r * pulse, p)
        }
        val scaled = phase * fieldEdges.size
        val activeIndex = scaled.toInt().coerceIn(0, fieldEdges.lastIndex)
        val progress = scaled - activeIndex
        val edge = fieldEdges[activeIndex]
        val a = points[edge.first]
        val b = points[edge.second]
        val signal = Offset(a.x + (b.x - a.x) * progress, a.y + (b.y - a.y) * progress)
        drawCircle(NeuralCyan.copy(alpha = 0.13f), 13.dp.toPx(), signal)
        drawCircle(NeuralCyan, 2.5.dp.toPx(), signal)
    }
}

private data class FieldNode(val x: Float, val y: Float, val radius: Float, val speed: Float = 1f, val core: Boolean = false)
private val fieldNodes = listOf(
    FieldNode(.03f,.40f,1.7f), FieldNode(.08f,.18f,2.0f), FieldNode(.12f,.62f,2.2f),
    FieldNode(.17f,.34f,2.5f), FieldNode(.19f,.81f,1.7f), FieldNode(.24f,.12f,1.8f),
    FieldNode(.27f,.51f,3.1f), FieldNode(.30f,.72f,2.0f), FieldNode(.34f,.26f,2.2f),
    FieldNode(.38f,.43f,1.7f), FieldNode(.41f,.87f,2.1f), FieldNode(.45f,.16f,2.5f),
    FieldNode(.48f,.61f,4.1f,0.8f,true), FieldNode(.52f,.34f,2.2f), FieldNode(.56f,.78f,2.7f),
    FieldNode(.60f,.09f,1.8f), FieldNode(.63f,.48f,2.4f), FieldNode(.67f,.26f,2.0f),
    FieldNode(.70f,.67f,3.0f), FieldNode(.73f,.89f,1.8f), FieldNode(.77f,.40f,2.4f),
    FieldNode(.80f,.15f,2.1f), FieldNode(.84f,.57f,2.7f), FieldNode(.88f,.31f,1.8f),
    FieldNode(.91f,.76f,2.3f), FieldNode(.95f,.48f,2.0f), FieldNode(.98f,.18f,1.6f),
    FieldNode(.36f,.58f,1.5f), FieldNode(.58f,.56f,1.5f), FieldNode(.22f,.44f,1.6f),
    FieldNode(.75f,.54f,1.5f), FieldNode(.47f,.91f,1.6f)
)
private val fieldEdges = listOf(
    0 to 1,0 to 3,0 to 2,1 to 3,1 to 5,2 to 3,2 to 4,3 to 5,3 to 6,3 to 29,
    4 to 7,5 to 8,6 to 7,6 to 8,6 to 9,6 to 29,7 to 9,7 to 10,8 to 9,8 to 11,
    9 to 12,9 to 13,10 to 12,10 to 31,11 to 12,11 to 15,12 to 13,12 to 14,12 to 27,
    12 to 28,13 to 16,13 to 17,14 to 16,14 to 18,14 to 31,15 to 17,16 to 18,16 to 20,
    17 to 20,17 to 21,18 to 19,18 to 20,18 to 22,19 to 22,20 to 22,20 to 23,21 to 23,
    22 to 24,22 to 30,23 to 25,23 to 26,24 to 25,24 to 30,25 to 26,27 to 28,28 to 30
)

@Composable
private fun BrandMark(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(42.dp).border(1.dp, NeuralCyan.copy(alpha = 0.42f), RoundedCornerShape(14.dp)).background(Color(0x55243F55), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(28.dp)) {
                val c = center
                val pts = listOf(Offset(c.x, size.height * .12f), Offset(size.width * .14f, size.height * .72f), Offset(size.width * .86f, size.height * .72f), c, Offset(size.width * .84f, size.height * .18f))
                listOf(0 to 3, 1 to 3, 2 to 3, 3 to 4).forEach { drawLine(NightBlue, pts[it.first], pts[it.second], 1.4.dp.toPx(), cap = StrokeCap.Round) }
                pts.forEachIndexed { i, p -> drawCircle(if (i == 3) Color(0xFFD4F7FF) else NeuralCyan, if (i == 3) 3.dp.toPx() else 2.dp.toPx(), p) }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("LYNOS", color = SoftText, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 3.4.sp)
            Text("NEURAL INTERFACE  /  01", color = MutedText, fontSize = 8.sp, letterSpacing = 1.5.sp)
        }
    }
}

@Composable
private fun Eyebrow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.width(19.dp).height(1.dp).background(NeuralCyan))
        Text(text, color = NeuralCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
    }
}

@Composable
private fun NetworkLabels(modifier: Modifier = Modifier, compact: Boolean) {
    Box(modifier.fillMaxWidth().height(if (compact) 190.dp else 250.dp)) {
        Column(Modifier.align(Alignment.TopEnd).padding(top = 6.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("SYNAPTIC FIELD", color = SoftText.copy(alpha = 0.78f), fontSize = 8.sp, letterSpacing = 1.8.sp)
            Text("32 NODES  /  49 LINKS", color = NeuralCyan.copy(alpha = 0.85f), fontSize = 7.sp, letterSpacing = 1.2.sp)
        }
        Column(Modifier.align(Alignment.BottomStart).padding(bottom = 7.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("KNOWLEDGE IN MOTION", color = SoftText.copy(alpha = 0.88f), fontSize = 9.sp, fontWeight = FontWeight.Medium, letterSpacing = 1.7.sp)
            Text("Every thought finds a path.", color = MutedText, fontSize = 10.sp)
        }
    }
}

@Composable
private fun WorkspaceDock(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.border(1.dp, Color(0x554D7897), RoundedCornerShape(20.dp)).background(Brush.horizontalGradient(listOf(Color(0xDD1A2A3D), Color(0xCC243B51))), RoundedCornerShape(20.dp)).padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DockItem("⌘", "MNEMOS", "KNOWLEDGE")
        Box(Modifier.width(1.dp).height(31.dp).background(Color(0x664D7897)))
        DockItem("{ }", "CUK STUDIO", "NATIVE TOOLS")
        Spacer(Modifier.weight(1f, fill = false))
        Box(Modifier.size(27.dp).border(1.dp, NeuralCyan.copy(alpha = 0.36f), CircleShape), contentAlignment = Alignment.Center) {
            Text("↗", color = NeuralCyan, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun DockItem(glyph: String, title: String, subtitle: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(31.dp).background(Color(0x442C536D), RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
            Text(glyph, color = NeuralCyan, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = SoftText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
            Text(subtitle, color = MutedText, fontSize = 7.sp, letterSpacing = 0.7.sp)
        }
    }
}

@Composable
private fun SystemStatus() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(5.dp).background(NeuralCyan, CircleShape))
        Text("CORE ONLINE", color = NeuralCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp)
        Box(Modifier.width(1.dp).height(10.dp).background(Color(0x665D7893)))
        Text("LOCAL-FIRST", color = MutedText, fontSize = 8.sp, letterSpacing = 1.sp)
        Spacer(Modifier.weight(1f))
        Text("LYNOS  /  PROTOTYPE 0.1", color = MutedText.copy(alpha = 0.72f), fontSize = 7.sp, letterSpacing = 0.5.sp)
    }
}
