package com.example.alertas

import android.os.Bundle
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.extensions.jsonBody
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.activity_main.*
import org.altbeacon.beacon.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.scheduleAtFixedRate

data class DetectedBeacon(
    val id: String,
    val from_: String,
    var until: String,
    val rssi: MutableList<Int>
)

class MainActivity : AppCompatActivity(), BeaconConsumer {
    private var beaconManager: BeaconManager? = null
    private var detectedBeacons: MutableList<DetectedBeacon> = mutableListOf<DetectedBeacon>()
    private val TAG = "DetectedBeacons"
    private var sessionId: String? = null
    val serverAPIURL = "http://gaia.manizales.unal.edu.co:3200/alertas/"
    private val timer = Timer("schedule", true)


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
                    val detectedBeacon = detectedBeacons.firstOrNull{ it.id == beaconId }

                    if (detectedBeacon == null) {
                        val c = Calendar.getInstance()
                        val sdf = SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss.SSS",
                            Locale.US
                        )

                        detectedBeacons.add(DetectedBeacon(beaconId, sdf.format(c.time), sdf.format(c.time), arrayListOf(beacon.rssi)))
                    } else {
                        val c = Calendar.getInstance()
                        val sdf = SimpleDateFormat(
                            "yyyy-MM-dd HH:mm:ss.SSS",
                            Locale.US
                        )
                        detectedBeacon.until = sdf.format(c.time)
                        detectedBeacon.rssi.add(beacon.rssi)
                    }
                }


                if (sessionId == null) {
                    val json = Gson().toJson(detectedBeacons)

                    Fuel.post(serverAPIURL).jsonBody(json).also {
                        Log.i(TAG, it.toString())
                    }.response { result ->
                        val (bytes, error) = result

                        if (bytes != null) {
                            val e = JsonParser().parse(String(bytes))
                            val obj = e.asJsonObject
                            sessionId = obj.get("session_uuid").asString

                            timer.scheduleAtFixedRate(30000, 30000)
                            {
                                Fuel.patch(serverAPIURL + "beacons/" + sessionId).jsonBody(Gson().toJson(detectedBeacons)).also{
                                    Log.i(TAG, it.toString())
                                }.response{ result -> }
                            }

                            runOnUiThread {
                                info.text = "Accede a los datos en http://gaia.manizales.unal.edu.co:3200/alertas/beacons/" + sessionId
                            }

                            Log.i(TAG, obj.toString())
                        } else {
                            runOnUiThread {
                                info.text = "Error"
                            }

                        }
                    }

                }
            }
        }
        try {
            beaconManager!!.startRangingBeaconsInRegion(Region("myRangingUniqueId", myBeaconNamespaceId, null, null))
        } catch (e: RemoteException) {
        }
    }
}