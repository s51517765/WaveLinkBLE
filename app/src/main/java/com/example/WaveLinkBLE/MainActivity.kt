package com.example.WaveLinkBLE

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BleGraphApp()
        }
    }
}

@Composable
fun BleGraphApp(viewModel: BleViewModel = viewModel()) {
    // 全て viewModel. を介して取得
    val isConnected = viewModel.isConnected

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "ESP32 BLE Monitor",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 接続・切断ボタン
        Button(
            onClick = {
                if (isConnected) {
                    viewModel.disconnectFromEsp32() // ViewModel内の関数を呼ぶ
                } else {
                    viewModel.connectToEsp32()    // ViewModel内の関数を呼ぶ
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isConnected) Color(0xFFE57373) else MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isConnected) "ESP32を切断" else "ESP32に接続")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 数値表示エリア
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ValueDisplay("Data 1", viewModel.dataPoints1.lastOrNull() ?: 0f, Color(0xFF2196F3))
                ValueDisplay("Data 2", viewModel.dataPoints2.lastOrNull() ?: 0f, Color(0xFF4CAF50))
                ValueDisplay("Data 3", viewModel.dataPoints3.lastOrNull() ?: 0f, Color(0xFFFF9800))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text("受信データグラフ", style = MaterialTheme.typography.headlineSmall)

        // グラフエリア
        val maxCount = maxOf(
            viewModel.dataPoints1.size,
            viewModel.dataPoints2.size,
            viewModel.dataPoints3.size
        ).coerceAtLeast(1)

        MultiLineChart(
            points1 = viewModel.dataPoints1,
            points2 = viewModel.dataPoints2,
            points3 = viewModel.dataPoints3,
            totalCount = maxCount
        )

        Spacer(modifier = Modifier.weight(1f))
    }
}

// --- 以下、ValueDisplay, MultiLineChart, drawDataPath はそのまま ---
@Composable
fun ValueDisplay(label: String, value: Float, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = color)
        Text(
            text = value.toInt().toString(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun MultiLineChart(
    points1: List<Float>,
    points2: List<Float>,
    points3: List<Float>,
    totalCount: Int
) {
    val fixedMaxVal = 1000f
    val displayMaxPoints = 30
    val gridLines = 5

    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(modifier = Modifier.height(240.dp)) {
            Column(
                modifier = Modifier.fillMaxHeight().padding(end = 8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                for (i in gridLines downTo 0) {
                    Text((fixedMaxVal * i / gridLines).toInt().toString(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }

            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Canvas(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
                    val spacing = size.width / (displayMaxPoints - 1)
                    for (i in 0..gridLines) {
                        val y = size.height * i / gridLines
                        drawLine(Color.LightGray.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), 1f)
                    }
                    drawDataPath(points1.takeLast(displayMaxPoints), spacing, fixedMaxVal, Color(0xFF2196F3))
                    drawDataPath(points2.takeLast(displayMaxPoints), spacing, fixedMaxVal, Color(0xFF4CAF50))
                    drawDataPath(points3.takeLast(displayMaxPoints), spacing, fixedMaxVal, Color(0xFFFF9800))
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Spacer(modifier = Modifier.width(36.dp))
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween) {
                val startCount = if (totalCount <= displayMaxPoints) 0 else totalCount - displayMaxPoints
                Text(startCount.toString(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text((startCount + 10).toString(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text((startCount + 20).toString(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text((if (totalCount < displayMaxPoints) displayMaxPoints - 1 else totalCount - 1).toString(), style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            }
        }
    }
}

fun DrawScope.drawDataPath(points: List<Float>, spacing: Float, maxVal: Float, color: Color) {
    if (points.isEmpty()) return
    val path = Path()
    points.forEachIndexed { index, value ->
        val x = index * spacing
        val y = size.height - (value.coerceIn(0f, maxVal) / maxVal * size.height)
        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    drawPath(path, color, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}