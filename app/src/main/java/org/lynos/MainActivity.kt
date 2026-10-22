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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF202F43), MidnightSlate, Color(0xFF151F2D))
                )
            )
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("LYNOS", color = SoftText, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text("NEURAL INTERFACE", color = MutedText, fontSize = 10.sp, letterSpacing = 2.sp)
            }
            Surface(
                color = Color(0xFF203B50),
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    "LOCAL SYSTEM",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    color = NeuralCyan,
                    fontSize = 10.sp,
                    letterSpacing = 1.sp
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            color = DeepNavy.copy(alpha = 0.88f),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Living Knowledge", color = SoftText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text("A network that moves with your ideas", color = MutedText, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                NeuralGraph(phase = phase, modifier = Modifier.fillMaxSize())
            }
        }

        Text("YOUR SPACE", color = MutedText, fontSize = 10.sp, letterSpacing = 2.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomeTile("Mnemos", "Knowledge graph", Modifier.weight(1f))
            HomeTile("Cuk Studio", "Native workspace", Modifier.weight(1f))
        }
        Text(
            "LynOS • local-first • lightweight by design",
            modifier = Modifier.fillMaxWidth(),
            color = MutedText,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun HomeTile(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color(0xE625354A),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = NeuralCyan, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = MutedText, fontSize = 11.sp)
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
        val points = graphNodes.map { node ->
            Offset(node.x * size.width, node.y * size.height)
        }

        graphEdges.forEachIndexed { index, edge ->
            val start = points[edge.first]
            val end = points[edge.second]
            drawLine(
                color = NightBlue.copy(alpha = 0.42f + 0.18f * sin(phase * 6.28f + index).toFloat().let { (it + 1f) / 2f }),
                start = start,
                end = end,
                strokeWidth = 1.2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // One moving signal at a time keeps the effect intentionally inexpensive.
        val signalEdge = (phase * graphEdges.size).toInt().coerceIn(0, graphEdges.lastIndex)
        val signalProgress = (phase * graphEdges.size) - signalEdge
        val active = graphEdges[signalEdge]
        val a = points[active.first]
        val b = points[active.second]
        val signal = Offset(
            x = a.x + (b.x - a.x) * signalProgress,
            y = a.y + (b.y - a.y) * signalProgress
        )
        drawCircle(color = NeuralCyan.copy(alpha = 0.13f), radius = 11.dp.toPx(), center = signal)
        drawCircle(color = NeuralCyan, radius = 3.dp.toPx(), center = signal)

        graphNodes.forEachIndexed { index, node ->
            val point = points[index]
            val pulse = 0.82f + 0.18f * sin(phase * 6.28f + index * 0.8f).toFloat()
            drawCircle(
                color = NeuralCyan.copy(alpha = 0.10f),
                radius = node.radius * 3.2f * pulse,
                center = point
            )
            drawCircle(
                color = if (index == 3) Color(0xFFB4F0FF) else NeuralCyan.copy(alpha = 0.88f),
                radius = node.radius * pulse,
                center = point
            )
        }
    }
}
