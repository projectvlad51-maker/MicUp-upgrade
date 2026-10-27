package com.micplugin.lite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.micplugin.service.SpeakerBoostState

private val LiteColors = darkColorScheme(primary = Color(0xFF71C7FF), background = Color(0xFF101521),
    surface = Color(0xFF1B2435), onSurface = Color(0xFFECF2FF), onSurfaceVariant = Color(0xFFB5C4DB))

@Composable
fun LiteScreen(
    settings: LiteSettings, audio: LiteRunState, devices: LiteDevices, boost: SpeakerBoostState,
    onToggle: () -> Unit, onMicrophone: (Float) -> Unit, onOutput: (Float) -> Unit,
    onVolume: (Float) -> Unit, onInputDevice: (String) -> Unit, onOutputDevice: (String) -> Unit,
    onPreset: (String) -> Unit,
) {
    MaterialTheme(colorScheme = LiteColors) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MicUp Lite", color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall)
                    Text("Естественный голос · сохранённые настройки", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                Text("VoIP", color = MaterialTheme.colorScheme.primary)
            }
            Button(onClick = onToggle, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (audio.running) "Выключить звук" else "Включить звук")
            }
            audio.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            if (audio.running) {
                LinearProgressIndicator(progress = ((audio.inputDb + 60f) / 60f).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth())
                Text("Микрофон активен · настройки применяются сразу", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LitePanel("10 пресетов громкости") {
                    var open by remember { mutableStateOf(false) }
                    val preset = LitePresets.all.firstOrNull { it.id == settings.presetId }
                    Box {
                        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(preset?.name ?: "Свои настройки")
                        }
                        DropdownMenu(expanded = open, onDismissRequest = { open = false }, modifier = Modifier.heightIn(max = 360.dp)) {
                            LitePresets.all.forEach { p ->
                                DropdownMenuItem(text = { Text(p.name) }, onClick = { onPreset(p.id); open = false })
                            }
                        }
                    }
                    Text(preset?.description ?: "Последние уровни сохранены", fontSize = 12.sp)
                    Text("Пресеты меняют только громкость. Выбор устройств остаётся прежним.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                LitePanel("Микрофон") {
                    LiteSlider("Усиление микрофона", settings.microphoneDb, 0f..36f, "дБ", "mic_gain", onMicrophone)
                    Text("Без эквалайзера, реверберации и изменения высоты голоса. Пики ограничиваются автоматически.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                LitePanel("Динамики и наушники") {
                    LiteSlider("Громкость выхода", settings.volume * 100f, 0f..100f, "%", "output_volume") { onVolume(it / 100f) }
                    LiteSlider("Усиление выхода", settings.outputDb, 0f..24f, "дБ", "output_gain", onOutput)
                    val outputStatus = when {
                        settings.outputDb == 0f -> "Усиление выхода выключено"
                        boost.available == false -> "Система не поддерживает усиление выхода на этом устройстве"
                        boost.active -> "Усиление выхода включено"
                        else -> "Усиление применится после запуска звука"
                    }
                    Text(outputStatus, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                LitePanel("Устройства") {
                    DevicePicker("Микрофон", settings.inputKey, devices.inputs, onInputDevice)
                    DevicePicker("Выход звука", settings.outputKey, devices.outputs, onOutputDevice)
                    audio.routeWarning?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    Text("Авто предпочитает подключённые наушники, затем динамик телефона. Недоступное устройство остаётся сохранённым.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                OutlinedButton(onClick = { onPreset("natural") }, modifier = Modifier.fillMaxWidth()) { Text("Сбросить только уровни") }
                Text("Все уровни, пресет и устройства сохраняются после закрытия приложения.\n\nVoIP здесь — прослушивание в MicUp Lite. Обработанный голос не передаётся в звонки Telegram или WhatsApp.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun LitePanel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun LiteSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>,
    unit: String, tag: String, onChange: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text("%.1f %s".format(value, unit), fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
    }
    Slider(value = value, onValueChange = onChange, valueRange = range,
        modifier = Modifier.fillMaxWidth().height(48.dp).testTag(tag))
}

@Composable
private fun DevicePicker(label: String, selected: String, devices: List<LiteDevice>, onSelect: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val chosen = devices.firstOrNull { it.key == selected }
    val title = if (selected == "auto") "Авто" else chosen?.name ?: "Сохранённое устройство недоступно"
    Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Box {
        OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth().testTag("device_$label")) { Text(title) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("Авто") }, onClick = { onSelect("auto"); open = false })
            devices.forEach { device ->
                DropdownMenuItem(text = { Text(device.name) }, onClick = { onSelect(device.key); open = false })
            }
        }
    }
}
