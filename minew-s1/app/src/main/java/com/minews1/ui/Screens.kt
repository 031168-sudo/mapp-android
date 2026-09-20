@file:OptIn(ExperimentalMaterial3Api::class)

package com.minews1.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.minews1.Ble
import com.minews1.DeviceDb
import com.minews1.HistoryDb
import com.minews1.SensorState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun MainScreen(
    devices: List<DeviceDb.Device>,
    sensorStates: Map<String, SensorState>,
    statusText: String,
    debugLast: String = "",
    debugTlm: String = "",
    debugPerms: String = "",
    debugScanCount: Int = 0,
    debugWtsRaw: String = "",
    onOpenDevices: () -> Unit,
    onOpenHistory: (DeviceDb.Device) -> Unit,
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Температура и влажность") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                "BLE-мониторинг датчиков",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(devices, key = { it.id }) { device ->
                    DeviceCard(device = device, state = sensorStates[device.id], onClick = { onOpenHistory(device) })
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onOpenDevices, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Устройства")
            }
            Text(
                statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 10.dp),
            )
            if (debugPerms.isNotBlank()) {
                Text(
                    "debug perms: $debugPerms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "debug onScanResult calls: $debugScanCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                if (debugWtsRaw.isBlank()) "debug WTS300 raw: not seen yet" else "debug WTS300 raw: $debugWtsRaw",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (debugLast.isNotBlank()) {
                Text(
                    "debug last: $debugLast",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (debugTlm.isNotBlank()) {
                Text(
                    "debug tlm: $debugTlm",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun DeviceCard(device: DeviceDb.Device, state: SensorState?, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(device.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(device.id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                StatValue("Температура", state?.t?.takeIf { !it.isNaN() }?.let { String.format(Locale.US, "%.2f °C", it) } ?: "—.— °C")
                StatValue("Влажность", state?.h?.takeIf { !it.isNaN() }?.let { String.format(Locale.US, "%.1f %%", it) } ?: "—.— %")
            }
            Spacer(Modifier.height(8.dp))
            val active = state != null && state.last > 0
            val statusColor = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            val statusLine = if (active && state != null) {
                val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(state.last))
                "●  BLE • $time" + if (state.battery >= 0) "  •  ${state.battery}%" else ""
            } else "Ожидание данных"
            Text(statusLine, style = MaterialTheme.typography.labelMedium, color = statusColor)
        }
    }
}

@Composable
private fun StatValue(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun DevicesScreen(
    found: List<Ble.Found>,
    status: String,
    scanning: Boolean,
    isAdded: (String) -> Boolean,
    onBack: () -> Unit,
    onScan: () -> Unit,
    onAdd: (Ble.Found) -> Unit,
) {
    var addedLocally by remember { mutableStateOf(setOf<String>()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавление устройства") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {
            Text(
                "Нажмите «Сканировать». Приложение покажет найденные BLE-датчики. Выберите устройство и добавьте его.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )
            Button(onClick = onScan, enabled = !scanning, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (scanning) "Сканирование…" else "Сканировать BLE")
            }
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(found, key = { it.mac }) { f ->
                    val added = isAdded(f.mac) || f.mac in addedLocally
                    FoundDeviceRow(found = f, added = added, onAdd = { onAdd(f); addedLocally = addedLocally + f.mac })
                }
            }
        }
    }
}

@Composable
private fun FoundDeviceRow(found: Ble.Found, added: Boolean, onAdd: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(found.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (found.type == "xiaomi") "Xiaomi LYWSDCGQ/01ZM" else "Minew BLE sensor",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "${found.mac}   ${found.rssi} dBm",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAdd, enabled = !added, modifier = Modifier.fillMaxWidth()) {
                Text(if (added) "Добавлено" else "Добавить")
            }
        }
    }
}

@Composable
fun HistoryScreen(
    deviceId: String,
    deviceName: String,
    db: HistoryDb,
    onBack: () -> Unit,
) {
    var period by remember { mutableStateOf(0) }
    var anchor by remember { mutableStateOf(Calendar.getInstance()) }

    val range = remember(period, anchor.timeInMillis) { computeRange(anchor, period) }
    val readings = remember(deviceId, range) { db.range(deviceId, range.first, range.second) }
    val title = remember(period, anchor.timeInMillis) { formatTitle(anchor, period) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(deviceName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Назад") } },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 12.dp)) {
            SegmentedPeriodTabs(period = period, onSelect = { period = it })
            Spacer(Modifier.height(8.dp))
            HistoryChart(readings = readings, from = range.first, to = range.second, modifier = Modifier.weight(1f).fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            DateNavRow(
                title = title,
                onPrev = { anchor = shiftCalendar(anchor, period, -1) },
                onNext = { anchor = shiftCalendar(anchor, period, 1) },
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SegmentedPeriodTabs(period: Int, onSelect: (Int) -> Unit) {
    val labels = listOf("День", "Месяц", "Год")
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        labels.forEachIndexed { index, label ->
            SegmentedButton(
                selected = period == index,
                onClick = { onSelect(index) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = labels.size),
            ) { Text(label) }
        }
    }
}

@Composable
private fun DateNavRow(title: String, onPrev: () -> Unit, onNext: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
            IconButton(onClick = onPrev) { Icon(Icons.Filled.ChevronLeft, contentDescription = "Назад по дате") }
            Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onNext) { Icon(Icons.Filled.ChevronRight, contentDescription = "Вперёд по дате") }
        }
    }
}

private fun computeRange(anchor: Calendar, period: Int): Pair<Long, Long> {
    val start = anchor.clone() as Calendar
    val end: Calendar
    when (period) {
        0 -> {
            start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
            end = start.clone() as Calendar
            end.add(Calendar.DAY_OF_MONTH, 1)
        }
        1 -> {
            start.set(Calendar.DAY_OF_MONTH, 1); start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
            end = start.clone() as Calendar
            end.add(Calendar.MONTH, 1)
        }
        else -> {
            start.set(Calendar.MONTH, Calendar.JANUARY); start.set(Calendar.DAY_OF_MONTH, 1); start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
            end = start.clone() as Calendar
            end.add(Calendar.YEAR, 1)
        }
    }
    return start.timeInMillis to end.timeInMillis
}

private fun formatTitle(anchor: Calendar, period: Int): String {
    val fmt = when (period) {
        0 -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        1 -> SimpleDateFormat("LLLL yyyy", Locale.getDefault())
        else -> SimpleDateFormat("yyyy", Locale.getDefault())
    }
    val raw = fmt.format(anchor.time)
    return raw.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
}

private fun shiftCalendar(anchor: Calendar, period: Int, amount: Int): Calendar {
    val c = anchor.clone() as Calendar
    when (period) {
        0 -> c.add(Calendar.DAY_OF_MONTH, amount)
        1 -> c.add(Calendar.MONTH, amount)
        else -> c.add(Calendar.YEAR, amount)
    }
    return c
}
