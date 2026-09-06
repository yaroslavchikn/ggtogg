package com.yaroslav.calcvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CalculatorScreen(onSecretTriggered: () -> Unit) {
    var display by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.End
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        // Дисплей
        Text(
            text = display.ifEmpty { "0" },
            fontSize = 48.sp,
            color = Color.White,
            fontWeight = FontWeight.Light,
            modifier = Modifier.padding(end = 16.dp, bottom = 8.dp)
        )
        Text(
            text = result,
            fontSize = 32.sp,
            color = Color.Gray,
            modifier = Modifier.padding(end = 16.dp, bottom = 32.dp)
        )

        // Кнопки
        val buttons = listOf(
            listOf("C", "±", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "-"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=")
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            buttons.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { btn ->
                        val isOperator = btn in listOf("÷", "×", "-", "+", "=")
                        val isZero = btn == "0"
                        
                        CalcButton(
                            text = btn,
                            modifier = Modifier
                                .weight(if (isZero) 2f else 1f)
                                .height(72.dp),
                            isOperator = isOperator,
                            onClick = { 
                                handleButtonClick(btn, display) { newDisplay, newResult ->
                                    display = newDisplay
                                    result = newResult
                                    if (btn == "=" && display == "67+67") {
                                        onSecretTriggered()
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CalcButton(text: String, modifier: Modifier, isOperator: Boolean, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(36.dp))
            .background(if (isOperator) Color(0xFFBB86FC) else Color(0xFF333333))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            color = Color.White,
            fontWeight = if (isOperator) FontWeight.Bold else FontWeight.Normal
        )
    }
}

fun handleButtonClick(btn: String, currentDisplay: String, onUpdate: (String, String) -> Unit) {
    var newDisplay = currentDisplay
    var newResult = ""

    when (btn) {
        "C" -> { newDisplay = ""; newResult = "" }
        "=" -> {
            // Проверяем секретную комбинацию ДО вычисления
            if (currentDisplay == "67+67") {
                onUpdate("67+67", "134") // Показываем результат, но триггерим переход в MainActivity
                return
            }
            newResult = evaluate(currentDisplay)
        }
        else -> {
            if (btn == "÷") newDisplay += "/"
            else if (btn == "×") newDisplay += "*"
            else newDisplay += btn
        }
    }
    onUpdate(newDisplay, newResult)
}

// Простой вычислитель математических выражений
fun evaluate(expression: String): String {
    return try {
        // Заменяем символы для JS-движка или используем простой парсер
        val expr = expression.replace("÷", "/").replace("×", "*")
        val result = org.mozilla.javascript.Context.enter().use { context ->
            context.evaluationStrategy = org.mozilla.javascript.Context.FEATURE_LOCATION
            val scope = context.initStandardObjects()
            context.evaluateString(scope, expr, "calc", 1, null).toString()
        }
        // Убираем .0 для целых чисел
        if (result.endsWith(".0")) result.dropLast(2) else result
    } catch (e: Exception) {
        "Ошибка"
    }
}
