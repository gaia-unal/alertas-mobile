package com.example.alertas

import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import kotlinx.android.synthetic.main.activity_main.*
import org.altbeacon.beacon.*
import java.text.SimpleDateFormat
import java.util.*

data class DetectedBeacon(
    val id: String,
    val from: String,
    var until: String,
    val rssi: MutableList<Int>
)

class MainActivity : AppCompatActivity(), BeaconConsumer {
    private var beaconManager: BeaconManager? = null
    private var detectedBeacons: MutableMap<String, DetectedBeacon> = mutableMapOf<String, DetectedBeacon>()
    private val TAG = "DetectedBeacons"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        beaconManager = BeaconManager.getInstanceForApplication(this)
        beaconManager!!.getBeaconParsers().add(BeaconParser().
        setBeaconLayout(BeaconParser.EDDYSTONE_UID_LAYOUT));
        beaconManager!!.bind(this)
    }

    override fun onBeaconServiceConnect() {
        val myBeaconNamespaceId = Identifier.parse("0x68aaac9dae7ad8464a55")

        beaconManager!!.addRangeNotifier { beacons, region ->
            if (beacons.isNotEmpty()) {

                beacons.forEach { beacon ->
                    val beaconId = beacon.id2.toString()

                    if (!detectedBeacons.containsKey(beaconId)) {
                        val c = Calendar.getInstance()
                        val sdf = SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss.SSS",
                            Locale.US
                        )
                        detectedBeacons[beaconId] = DetectedBeacon(beaconId, sdf.format(c.time), sdf.format(c.time), arrayListOf(beacon.rssi))
                    } else {
                        val c = Calendar.getInstance()
                        val sdf = SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss.SSS",
                            Locale.US
                        )
                        detectedBeacons[beaconId]?.until = sdf.format(c.time)
                        detectedBeacons[beaconId]?.rssi?.add(beacon.rssi)
                    }
                }

                var json = Gson().toJson(detectedBeacons)
                Log.i(TAG, json.toString())
                runOnUiThread {
                    info.text = json.toString()
                }
            } /*else {
                runOnUiThread {
                    info.text = json.
                }
            }*/
        }
        try {
            beaconManager!!.startRangingBeaconsInRegion(Region("myRangingUniqueId", myBeaconNamespaceId, null, null))
        } catch (e: RemoteException) {
        }
    }
}