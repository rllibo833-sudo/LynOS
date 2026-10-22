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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
private val Panel = Color(0xFF202F43)
private val Line = Color(0xFF344960)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(24, 35, 51)
        window.navigationBarColor = android.graphics.Color.rgb(24, 35, 51)
        setContent {
            MaterialTheme {
                LynHome()
            }
        }
    }
}

@Composable
private fun LynHome() {
    val transition = rememberInfiniteTransition(label = "neural-network")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signal-phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF202F43), MidnightSlate, Color(0xFF131D2A))
                )
            )
    ) {
        // Quiet cyan bloom adds depth without a costly image or blur effect.
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(NeuralCyan.copy(alpha = 0.07f), Color.Transparent),
                    center = Offset(size.width * 0.92f, size.height * 0.18f),
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.92f, size.height * 0.18f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Header()
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("YOUR MIND, IN MOTION", color = NeuralCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
                Text("Knowledge,\nconnected.", color = SoftText, fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.7).sp)
                Text("One quiet space for everything you’re building.", color = MutedText, fontSize = 13.sp)
            }

            KnowledgePanel(phase = phase)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricPill("●", "LOCAL-FIRST", Modifier.weight(1f))
                MetricPill("↗", "LOW OVERHEAD", Modifier.weight(1f))
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("WORKSPACE", color = SoftText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("Your tools, one layer away", color = MutedText, fontSize = 11.sp)
                }
                Text("02 MODULES", color = NightBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                ModuleCard("01", "Mnemos", "Knowledge graph", "⌘", Modifier.weight(1f))
                ModuleCard("02", "Cuk Studio", "Native workspace", "{ }", Modifier.weight(1f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x66202F43), RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).background(NeuralCyan, CircleShape))
                Spacer(Modifier.width(9.dp))
                Text("LYNOS CORE", color = SoftText, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
                Spacer(Modifier.weight(1f))
                Text("LOCAL • PROTOTYPE 0.1", color = MutedText, fontSize = 9.sp, letterSpacing = 0.5.sp)
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(Brush.linearGradient(listOf(NeuralCyan.copy(alpha = 0.22f), NightBlue.copy(alpha = 0.22f))), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.size(23.dp)) {
                    val c = center
                    val points = listOf(
                        Offset(c.x, size.height * 0.12f),
                        Offset(size.width * 0.15f, size.height * 0.72f),
                        Offset(size.width * 0.85f, size.height * 0.72f),
                        Offset(c.x, c.y)
                    )
                    drawLine(NeuralCyan, points[0], points[3], 1.4.dp.toPx())
                    drawLine(NightBlue, points[1], points[3], 1.4.dp.toPx())
                    drawLine(NightBlue, points[2], points[3], 1.4.dp.toPx())
                    points.forEach { drawCircle(NeuralCyan, 2.1.dp.toPx(), it) }
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("LYNOS", color = SoftText, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.8.sp)
                Text("NEURAL INTERFACE", color = MutedText, fontSize = 8.sp, letterSpacing = 1.7.sp)
            }
        }
        Surface(color = Color(0xFF203B50), shape = RoundedCornerShape(50), border = androidx.compose.foundation.BorderStroke(1.dp, NeuralCyan.copy(alpha = 0.18f))) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(Modifier.size(5.dp).background(NeuralCyan, CircleShape))
                Text("SYSTEM ONLINE", color = NeuralCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            }
        }
    }
}

@Composable
private fun KnowledgePanel(phase: Float) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xE61D2B3D),
        shape = RoundedCornerShape(25.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x553F6582))
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("LIVING KNOWLEDGE", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.8.sp)
                    Text("Neural field", color = SoftText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
                Surface(color = Color(0xFF203B50), shape = RoundedCornerShape(9.dp)) {
                    Text("LIVE", modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), color = NeuralCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                }
            }
            Box(Modifier.fillMaxWidth().height(188.dp)) {
                NeuralGraph(phase = phase, modifier = Modifier.fillMaxSize())
                Column(
                    modifier = Modifier.align(Alignment.BottomStart).padding(bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("IDEAS FORM", color = SoftText.copy(alpha = 0.85f), fontSize = 9.sp, letterSpacing = 1.5.sp)
                    Text("CONNECTIONS", color = NeuralCyan, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0x662B4056), RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("VISUALIZATION", color = MutedText, fontSize = 8.sp, letterSpacing = 1.sp)
                    Text("Concept network", color = SoftText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(6.dp).background(NeuralCyan, CircleShape))
                    Text("SIGNAL ACTIVE", color = NeuralCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                }
            }
        }
    }
}

@Composable
private fun MetricPill(symbol: String, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color(0x442C4056), RoundedCornerShape(12.dp))
            .padding(horizontal = 11.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(symbol, color = NeuralCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(label, color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp)
    }
}

@Composable
private fun ModuleCard(number: String, title: String, subtitle: String, glyph: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xE624354A),
        shape = RoundedCornerShape(19.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Line.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(number, color = NightBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Box(
                    modifier = Modifier.size(32.dp).background(Color(0xFF203B50), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(glyph, color = NeuralCyan, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = SoftText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MutedText, fontSize = 10.sp, lineHeight = 14.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("OPEN MODULE", color = NeuralCyan, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.7.sp)
                Text("↗", color = NeuralCyan, fontSize = 11.sp)
            }
        }
    }
}

private data class Node(val x: Float, val y: Float, val radius: Float)

private val graphNodes = listOf(
    Node(0.08f, 0.48f, 3.0f),
    Node(0.23f, 0.25f, 4.0f),
    Node(0.31f, 0.72f, 3.2f),
    Node(0.49f, 0.48f, 5.0f),
    Node(0.63f, 0.20f, 3.6f),
    Node(0.72f, 0.72f, 4.0f),
    Node(0.91f, 0.42f, 3.4f),
    Node(0.88f, 0.84f, 2.8f)
)

private val graphEdges = listOf(
    0 to 1, 0 to 2, 1 to 2, 1 to 3, 2 to 3,
    3 to 4, 3 to 5, 4 to 6, 5 to 6, 5 to 7, 6 to 7
)

@Composable
private fun NeuralGraph(phase: Float, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val points = graphNodes.map { node -> Offset(node.x * size.width, node.y * size.height) }

        graphEdges.forEachIndexed { index, edge ->
            val shimmer = (sin((phase * 6.28f + index).toDouble()).toFloat() + 1f) / 2f
            drawLine(
                color = NightBlue.copy(alpha = 0.28f + 0.34f * shimmer),
                start = points[edge.first],
                end = points[edge.second],
                strokeWidth = 1.1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        val signalEdge = (phase * graphEdges.size).toInt().coerceIn(0, graphEdges.lastIndex)
        val signalProgress = (phase * graphEdges.size) - signalEdge
        val active = graphEdges[signalEdge]
        val a = points[active.first]
        val b = points[active.second]
        val signal = Offset(a.x + (b.x - a.x) * signalProgress, a.y + (b.y - a.y) * signalProgress)
        drawCircle(NeuralCyan.copy(alpha = 0.14f), radius = 10.dp.toPx(), center = signal)
        drawCircle(NeuralCyan, radius = 2.8.dp.toPx(), center = signal)

        graphNodes.forEachIndexed { index, node ->
            val point = points[index]
            val pulse = 0.84f + 0.16f * sin((phase * 6.28f + index * 0.8f).toDouble()).toFloat()
            drawCircle(NeuralCyan.copy(alpha = 0.09f), radius = node.radius * 3.0f * pulse, center = point)
            drawCircle(
                color = if (index == 3) Color(0xFFB4F0FF) else NeuralCyan.copy(alpha = 0.92f),
                radius = node.radius * pulse,
                center = point
            )
        }
    }
}
