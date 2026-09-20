package com.minews1

import android.Manifest
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.minews1.ui.DevicesScreen
import com.minews1.ui.HistoryScreen
import com.minews1.ui.MainScreen
import com.minews1.ui.theme.MinewTheme
import java.util.Locale

data class SensorState(
    val t: Float = Float.NaN,
    val h: Float = Float.NaN,
    val battery: Int = -1,
    val last: Long = 0L,
)

private sealed class Screen {
    data object Main : Screen()
    data object Devices : Screen()
    data class History(val deviceId: String, val deviceName: String) : Screen()
}

class MainActivity : ComponentActivity() {
    private val main = Handler(Looper.getMainLooper())
    private lateinit var db: HistoryDb
    private lateinit var deviceDb: DeviceDb
    private var scanner: BluetoothLeScanner? = null
    private var callback: ScanCallback? = null
    private var screenIsHistory = false

    private var screen: Screen by mutableStateOf(Screen.Main)
    private var devices by mutableStateOf(listOf<DeviceDb.Device>())
    private var sensorStates by mutableStateOf(mapOf<String, SensorState>())
    private var mainStatus by mutableStateOf("Поиск датчиков…")
    private var debugEddystone by mutableStateOf("")
    private var debugEddystoneTlm by mutableStateOf("")
    private var foundDevices by mutableStateOf(listOf<Ble.Found>())
    private var deviceScanStatus by mutableStateOf("Готово к сканированию")
    private var deviceScanActive by mutableStateOf(false)

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (!needPermissions()) startMonitoring() else mainStatus = "Разрешение Bluetooth не выдано"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        db = HistoryDb(this)
        deviceDb = DeviceDb(this)
        deviceDb.ensureMinew()
        refreshDevices()

        setContent {
            MinewTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    when (val s = screen) {
                        is Screen.Main -> MainScreen(
                            devices = devices,
                            sensorStates = sensorStates,
                            statusText = mainStatus,
                            debugLast = debugEddystone,
                            debugTlm = debugEddystoneTlm,
                            onOpenDevices = ::goDevices,
                            onOpenHistory = { d -> goHistory(d.id, d.name) },
                        )
                        is Screen.Devices -> {
                            BackHandler(onBack = ::goMain)
                            DevicesScreen(
                                found = foundDevices,
                                status = deviceScanStatus,
                                scanning = deviceScanActive,
                                isAdded = { mac -> deviceDb.find(mac) != null },
                                onBack = ::goMain,
                                onScan = ::startDeviceScan,
                                onAdd = { f ->
                                    deviceDb.add(f.mac, if (f.type == "xiaomi") "Xiaomi LYWSDCGQ/01ZM" else f.name, f.type)
                                    refreshDevices()
                                },
                            )
                        }
                        is Screen.History -> {
                            BackHandler(onBack = ::goMain)
                            HistoryScreen(
                                deviceId = s.deviceId,
                                deviceName = s.deviceName,
                                db = db,
                                onBack = ::goMain,
                            )
                        }
                    }
                }
            }
        }

        if (needPermissions()) {
            permissionLauncher.launch(arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT))
        } else {
            startMonitoring()
        }
    }

    override fun onPause() {
        super.onPause()
        stopScan()
        main.removeCallbacksAndMessages(null)
    }

    override fun onResume() {
        super.onResume()
        if (!screenIsHistory && !needPermissions()) {
            main.postDelayed({ if (!screenIsHistory) startMonitoring() }, 150)
        }
    }

    private fun needPermissions(): Boolean =
        Build.VERSION.SDK_INT >= 31 &&
            (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)

    private fun refreshDevices() {
        devices = deviceDb.all()
    }

    private fun goMain() {
        screenIsHistory = false
        screen = Screen.Main
        startMonitoring()
    }

    private fun goDevices() {
        screenIsHistory = true
        stopScan()
        main.removeCallbacksAndMessages(null)
        foundDevices = emptyList()
        deviceScanStatus = "Готово к сканированию"
        deviceScanActive = false
        screen = Screen.Devices
    }

    private fun goHistory(deviceId: String, name: String) {
        screenIsHistory = true
        stopScan()
        main.removeCallbacksAndMessages(null)
        screen = Screen.History(deviceId, name)
    }

    private fun startMonitoring() {
        if (needPermissions()) return
        screenIsHistory = false
        startScan()
        main.postDelayed(object : Runnable {
            override fun run() {
                if (!screenIsHistory) {
                    stopScan(); startScan(); main.postDelayed(this, 12000)
                }
            }
        }, 12000)
    }

    private fun startScan() {
        if (screenIsHistory || needPermissions()) return
        val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bm.adapter
        if (adapter == null || !adapter.isEnabled) { mainStatus = "Включите Bluetooth"; return }
        val sc = adapter.bluetoothLeScanner
        if (sc == null) { mainStatus = "BLE недоступен"; return }
        scanner = sc
        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) { parseMeasurement(result) }
            override fun onScanFailed(errorCode: Int) { main.post { mainStatus = "Ошибка BLE-сканирования: $errorCode" } }
        }
        try {
            sc.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build(), callback)
        } catch (e: SecurityException) {
            mainStatus = "Нет разрешения Bluetooth"
        }
    }

    private fun stopScan() {
        val sc = scanner
        val cb = callback
        if (sc != null && cb != null) {
            try { sc.stopScan(cb) } catch (e: SecurityException) { /* no-op */ }
            callback = null
        }
    }

    private fun startDeviceScan() {
        if (needPermissions()) { deviceScanStatus = "Нет разрешения Bluetooth"; return }
        val bm = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bm.adapter
        if (adapter == null || !adapter.isEnabled) { deviceScanStatus = "Включите Bluetooth"; return }
        val sc = adapter.bluetoothLeScanner
        if (sc == null) { deviceScanStatus = "BLE недоступен"; return }
        scanner = sc
        val found = LinkedHashMap<String, Ble.Found>()
        foundDevices = emptyList()
        deviceScanStatus = "Сканирование…"
        deviceScanActive = true
        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val f = Ble.identify(result) ?: return
                val old = found[f.mac]
                if (old == null || f.rssi > old.rssi) {
                    found[f.mac] = f
                    main.post { foundDevices = found.values.toList() }
                }
            }
            override fun onScanFailed(errorCode: Int) {
                main.post { deviceScanStatus = "Ошибка BLE-сканирования: $errorCode" }
            }
        }
        try {
            sc.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).setReportDelay(0).build(), callback)
        } catch (e: SecurityException) {
            deviceScanStatus = "Нет разрешения Bluetooth"
            deviceScanActive = false
            return
        }
        main.postDelayed({
            stopScan()
            deviceScanActive = false
            deviceScanStatus = "Найдено устройств: ${foundDevices.size}"
        }, 10000)
    }

    private fun parseMeasurement(r: ScanResult) {
        val record = r.scanRecord ?: return
        val scanMac: String? = try { r.device.address } catch (e: SecurityException) { null }
        if (scanMac == null) return
        var d = deviceDb.find(scanMac)
        val xdata = record.getServiceData(Ble.XIAOMI_UUID)
        val mdata = record.getServiceData(Ble.MINEW_UUID)
        val edata = record.getServiceData(Ble.EDDYSTONE_UUID)
        if (edata != null) {
            val hex = edata.joinToString(" ") { String.format(Locale.US, "%02X", it.toInt() and 0xFF) }
            val isTlm = edata.isNotEmpty() && Ble.u(edata, 0) == 0x20
            val line = "${if (isTlm) "TLM" else "FEAA"} [$scanMac] len=${edata.size}: $hex"
            main.post {
                debugEddystone = line
                if (isTlm) debugEddystoneTlm = line
            }
        }
        if (edata != null && edata.size >= 6 && Ble.u(edata, 0) == 0x20) {
            val wd = deviceDb.find(Ble.WTS300_MAC)
            if (wd != null && wd.type == "minew_wts300") d = wd
        }
        if (xdata != null && Ble.isXiaomi(xdata)) {
            val embedded = Ble.xiaomiMac(xdata)
            val xd = embedded?.let { deviceDb.find(it) }
            if (xd != null && xd.type == "xiaomi") d = xd
        }
        if (d == null && mdata != null) {
            val embedded = Ble.minewEmbeddedMac(mdata)
            if (embedded != null) {
                val md = deviceDb.find(embedded)
                if (md != null && md.type == "minew") d = md
            }
        }
        if (d == null) return

        var t = Float.NaN
        var h = Float.NaN
        var battery = -1

        when (d.type) {
            "minew" -> {
                val data = mdata
                if (data == null || data.size < 13 || Ble.u(data, 0) != 0xA1 || Ble.u(data, 1) != 0x01) return
                val tr = (((data[3].toInt() and 255) shl 8) or (data[4].toInt() and 255)).toShort()
                val hr = (((data[5].toInt() and 255) shl 8) or (data[6].toInt() and 255)).toShort()
                t = tr / 256f
                h = hr / 256f
                battery = data[2].toInt() and 255
                val embedded = String.format(
                    Locale.US, "%02X:%02X:%02X:%02X:%02X:%02X",
                    data[12].toInt() and 255, data[11].toInt() and 255, data[10].toInt() and 255,
                    data[9].toInt() and 255, data[8].toInt() and 255, data[7].toInt() and 255,
                )
                if (!d.id.equals(scanMac, ignoreCase = true) && !d.id.equals(embedded, ignoreCase = true)) return
            }
            "xiaomi" -> {
                if (xdata == null || !Ble.isXiaomi(xdata)) return
                when (Ble.u(xdata, 11)) {
                    0x0D -> {
                        if (xdata.size < 18) return
                        val tr = (Ble.u(xdata, 14) or (Ble.u(xdata, 15) shl 8)).toShort()
                        val hr = (Ble.u(xdata, 16) or (Ble.u(xdata, 17) shl 8)).toShort()
                        t = tr / 10f
                        h = hr / 10f
                    }
                    0x0A -> {
                        if (xdata.size < 15) return
                        battery = Ble.u(xdata, 14)
                    }
                    else -> return
                }
            }
            "minew_wts300" -> {
                if (edata == null || edata.size < 6 || Ble.u(edata, 0) != 0x20) return
                val tr = ((Ble.u(edata, 4) shl 8) or Ble.u(edata, 5)).toShort()
                t = tr / 256f
            }
            else -> { /* unknown device type: still send a heartbeat below */ }
        }

        val id = d.id
        val ft = t
        val fh = h
        val fb = battery
        main.post { updateSensor(id, ft, fh, fb) }
    }

    private fun updateSensor(id: String, t: Float, h: Float, battery: Int) {
        val prev = sensorStates[id] ?: SensorState()
        val next = SensorState(
            t = if (!t.isNaN()) t else prev.t,
            h = if (!h.isNaN()) h else prev.h,
            battery = if (battery >= 0) battery else prev.battery,
            last = System.currentTimeMillis(),
        )
        sensorStates = sensorStates + (id to next)
        if (!next.t.isNaN() && !next.h.isNaN()) db.add(id, next.last, next.t, next.h)
        mainStatus = "●  BLE-сканирование активно"
    }
}
