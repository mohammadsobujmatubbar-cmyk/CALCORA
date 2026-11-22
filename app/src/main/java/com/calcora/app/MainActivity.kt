package com.calcora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF101522)
private val Panel = Color(0xFF1B2535)
private val Accent = Color(0xFF70D6C5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalcoraApp()
        }
    }
}

@Composable
fun CalcoraApp() {
    var expression by remember { mutableStateOf("0") }

    val buttons = listOf(
        listOf("AC", "⌫", "%", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "−"),
        listOf("1", "2", "3", "+"),
        listOf("📒", "0", ".", "=")
    )

    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("CALCORA", color = Color.White, fontSize = 25.sp)
                Text("History  ⋮", color = Accent, fontSize = 15.sp)
            }

            Spacer(Modifier.height(30.dp))

            Text("Smart Calculator", color = Color.LightGray)
            Spacer(Modifier.weight(1f))

            Text(
                expression,
                color = Color.White,
                fontSize = 42.sp,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { label ->
                        Button(
                            onClick = {
                                expression = when (label) {
                                    "AC" -> "0"
                                    "⌫" -> expression.dropLast(1).ifEmpty { "0" }
                                    "📒" -> "Notebook coming soon"
                                    "=" -> expression
                                    else -> if (expression == "0") label
                                            else expression + label
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(66.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (
                                    label in listOf("÷", "×", "−", "+", "=")
                                ) Accent else Panel,
                                contentColor = if (
                                    label in listOf("÷", "×", "−", "+", "=")
                                ) Background else Color.White
                            )
                        ) {
                            Text(label, fontSize = 21.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Calculate • Write • Remember",
                color = Color.Gray,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}
