package com.test.smsapplication
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.telephony.SmsManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.test.smsapplication.adapters.DashAdapter
import com.test.smsapplication.models.ModelData
import com.test.smsapplication.models.SmsData

class Sample : AppCompatActivity() {

    private var sharedPreferences: SharedPreferences? = null
    private lateinit var smsManager: SmsManager

    private val handler: Handler = Handler()
    private var mediaPlayer: MediaPlayer? = null

    private var adapter: DashAdapter? = null
    private var idList: MutableList<Int>? = null
    private var phoneList: MutableList<String>? = null
    private var messageList: MutableList<String>? = null
    private var messageTypeList: MutableList<String>? = null
    private var statusList: MutableList<Int>? = null

    companion object {
        private val smsCountListeners = mutableListOf<((Int) -> Unit)>()
        var ipLink: String? = null
        const val PERMISSION_REQUEST_SEND_SMS = 1
        const val DELAY_BETWEEN_SMS = 3000L
        @SuppressLint("ServiceCast")
        fun getIpAddress(): CharSequence {
            return ipLink.toString()
        }
        fun getSmsLimit(): String {
            return smsCount.toString()
        }
        private var smsCount: Int = 0
            set(value) {
                field = value
                notifyListeners()
            }
        fun addSMSCountListener(listener: (Int) -> Unit) {
            smsCountListeners.add(listener)
            listener.invoke(smsCount)
        }
        private fun notifyListeners() {
            smsCountListeners.forEach { it.invoke(smsCount) }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        setContentView(R.layout.activity_sample)

        sharedPreferences = this.getSharedPreferences("teda.uz", 0)
        smsManager = SmsManager.getDefault()

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        val navController = findNavController(R.id.nav_host_fragment_activity_sample)
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_settings
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        if (!isSmsPermissionGranted()) {
            val alertDialog = AlertDialog.Builder(this)
            alertDialog.setTitle("SMS")
            alertDialog.setMessage("SMS xizmatiga ruxsat berildi")
            alertDialog.setPositiveButton("OK") { dialog, _ ->
                requestSendSmsPermission()
                dialog.dismiss()
            }
            alertDialog.setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            alertDialog.show()
        } else {
            smsManager = SmsManager.getDefault()
            //getData()
        }
        getData()
    }
    @SuppressLint("SetTextI18n")
    private fun getNewData() {
        smsCount = sharedPreferences?.getString("smsLimit", "")!!.toInt()
        if (smsCount <= 50) {
            Toast.makeText(this, "Sms limit tugadi", Toast.LENGTH_LONG).show()
        }

        val queue = Volley.newRequestQueue(this)

        val phone = sharedPreferences?.getString("phone", "")
        val url = "${ipLink}api/sms/status?page=0&size=100&employeePhone=${phone?.substring(1)}&status=1"
        val stringRequest = StringRequest(
            Request.Method.GET, url, {
                Toast.makeText(this, "Yangi Smslar Tekshirilmoqda", Toast.LENGTH_LONG).show()
            }, {
                Toast.makeText(this, "Ulanishda Xatolik!", Toast.LENGTH_LONG).show()
            })
        queue.add(stringRequest)
    }
    private fun getData() {
        smsCount = getSmsLimit()
        if (smsCount <= 50) {
            Toast.makeText(this, "Sms limit tugadi", Toast.LENGTH_LONG).show()
        }
        val queue = Volley.newRequestQueue(this)

        val phone = sharedPreferences?.getString("phone", "")
        //val url = "http://185.185.80.245:77/api/sms/status?page=0&size=50&employeePhone=998977515747&status=2"
        ipLink = getIPAdress()
        val url = "${ipLink}api/sms/status?page=0&size=100&employeePhone=${
            phone?.substring(1)
        }&status=2"
        val stringRequest = StringRequest(
            Request.Method.GET, url, { response ->
                val gson = Gson()
                val json = gson.fromJson(response, ModelData::class.java)
                println(response)
                idList = ArrayList()
                phoneList = ArrayList()
                messageList = ArrayList()
                messageTypeList = ArrayList()
                statusList = ArrayList()
                if (json.data.content.isEmpty()) {
                    Toast.makeText(this, "Ma'lumot topilmadi", Toast.LENGTH_LONG).show()
                    handler.postDelayed({
                        getNewData()
                    }, 7000)
                } else {
                    for (i in json.data.content.indices) {
                        idList?.add(json.data.content[i].id)
                        phoneList?.add(json.data.content[i].tel)
                        messageList?.add(json.data.content[i].zapros)
                        messageTypeList?.add(json.data.content[i].platforma)
                        statusList?.add(json.data.content[i].flag)
                    }

                    sendTypeSms(
                        idList,
                        phoneList,
                        messageList,
                        messageTypeList,
                        statusList,
                        this
                    )

                    adapter = DashAdapter(
                        this,
                        phoneList!!,
                        messageList!!,
                        messageTypeList!!
                    )
                    mediaPlayer?.stop()
                }
            }, {
                mediaPlayer = MediaPlayer.create(this, R.raw.errors)
                mediaPlayer?.start()
                Toast.makeText(this, "Ulanishda Xatolik", Toast.LENGTH_LONG).show()
            })
        queue.add(stringRequest)
    }

    private fun sendSms(
        idList: MutableList<Int>?,
        phoneList: MutableList<String>?,
        messageList: MutableList<String>?,
        messageTypeList: MutableList<String>?,
        statusList: MutableList<Int>?,
        requireContext: Context) {
        if (messageList?.size != 0 && phoneList?.size != 0 && idList?.size != 0 && smsCount > 0) {
            val id = idList?.get(0)
            val message = messageList?.get(0)
            val phoneNumber = phoneList?.get(0)
            val messages = smsManager.divideMessage(message)
            val messageType = messageTypeList?.get(0)
            for (msg in messages) {
                smsManager.sendTextMessage(phoneNumber, null, msg, null, null)
                smsCount--
                if (sharedPreferences?.getString("sound", "0") == "0") {
                    mediaPlayer = MediaPlayer.create(this, R.raw.sound)
                    mediaPlayer?.start()
                }
                if (sharedPreferences?.getString("vibration", "0") == "1") {
                    Toast.makeText(this, "Vibration", Toast.LENGTH_SHORT).show()
                    val vibrator =
                        this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(500)
                }
                updateSms(idList?.get(0))
            }
            val editor = sharedPreferences?.edit()
            editor?.putString("smsLimit", smsCount.toString())
            editor?.apply()
            saveSms(message!!, phoneNumber!!, id!!)
            if (messageList.size > 0) {
                handler.postDelayed({
                    sendSms(
                        idList,
                        phoneList,
                        messageList,
                        messageTypeList,
                        statusList,
                        requireContext)
                }, DELAY_BETWEEN_SMS)
            } else {
                Toast.makeText(this, "Sms tugadi", Toast.LENGTH_SHORT).show()
                if (sharedPreferences?.getString("sounds", "0") == "0") {
                    Handler().postDelayed({
                        mediaPlayer = MediaPlayer.create(this, R.raw.sounds)
                        mediaPlayer?.start()
                    }, 1000)
                }
                if (sharedPreferences?.getString("vibration", "0") == "1") {
                    Toast.makeText(this, "Vibration", Toast.LENGTH_SHORT).show()
                    val vibrator =
                        this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(500)
                }
                getNewData()
            }
        } else {
            handler.postDelayed({
                getData()
            }, 8000)
        }
    }

    private fun sendTypeSms(
        idList: MutableList<Int>?,
        phoneList: MutableList<String>?,
        messageList: MutableList<String>?,
        messageTypeList: MutableList<String>?,
        statusList: MutableList<Int>?,
        requireContext: Context
    ) {
        if (messageList?.size != 0 && phoneList?.size != 0 && idList?.size != 0 && smsCount > 0) {
            val id = idList?.get(0)
            val message = messageList?.get(0)
            val phoneNumber = phoneList?.get(0)
            val messages = smsManager.divideMessage(message)
            val messageType = messageTypeList?.get(0)
            //if (messageType != null){ toast(messageType) }
            if (!messageType.isNullOrEmpty()) {
                for (msg in messages) {
                    smsManager.sendTextMessage(phoneNumber, null, msg, null, null)
                    smsCount--
                    if (sharedPreferences?.getString("sound", "0") == "0") {
                        mediaPlayer = MediaPlayer.create(this, R.raw.sound)
                        mediaPlayer?.start()
                    }
                    if (sharedPreferences?.getString("vibration", "0") == "1") {
                        Toast.makeText(this, "Vibration", Toast.LENGTH_SHORT).show()
                        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        vibrator.vibrate(500)
                    }
                    updateSms(id)
                }
                val editor = sharedPreferences?.edit()
                editor?.putString("smsLimit", smsCount.toString())
                editor?.apply()
                saveSms(message!!, phoneNumber!!, id!!)
                removeItem()
            }
            if (messageList?.size!! > 0) {
                handler.postDelayed({
                    sendTypeSms(
                        idList,
                        phoneList,
                        messageList,
                        messageTypeList,
                        statusList,
                        requireContext
                    )
                }, DELAY_BETWEEN_SMS)
            } else {
                Toast.makeText(this, "Sms tugadi", Toast.LENGTH_SHORT).show()
                if (sharedPreferences?.getString("sounds", "0") == "0") {
                    Handler().postDelayed({
                        mediaPlayer = MediaPlayer.create(this, R.raw.sounds)
                        mediaPlayer?.start()
                    }, 1000)
                }
                if (sharedPreferences?.getString("vibration", "0") == "1") {
                    Toast.makeText(this, "Vibration", Toast.LENGTH_SHORT).show()
                    val vibrator =
                        this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(500)
                }
                getNewData()
            }
        } else {
            handler.postDelayed({
                getData()
            }, 8000)
        }
    }

    private fun saveSms(message: String, phoneNumber: String, idList: Int) {
        sharedPreferences = this.getSharedPreferences("teda.uz", 0)
        val editor = sharedPreferences?.edit()
        val gson = Gson()
        val json = sharedPreferences?.getString("smsData", "")
        println(json)
        val type = object : TypeToken<ArrayList<SmsData>>() {}.type //type of array list
        var smsData: ArrayList<SmsData>? = gson.fromJson(json, type)
        if (smsData == null) {
            smsData = ArrayList()
        }
        smsData.add(SmsData(idList, phoneNumber, message))
        val jsonSmsData = gson.toJson(smsData)
        editor?.putString("smsData", jsonSmsData)
        editor?.apply()
    }

    private fun updateSms(get: Int?) {
        val queue = Volley.newRequestQueue(this)
        val urls: String = if (ipLink.toString().last() == '/') {
            ipLink.toString().substring(0, ipLink.toString().length - 1)
        } else {
            //txtHomipAdress?.text.toString()
            ipLink.toString()
        }
        //https://api.teda.uz:7788/api/sms
        val url = "$urls/api/sms"
        val listId: ArrayList<Int> = ArrayList()
        listId.add(get!!)
        val stringRequest = object : StringRequest(
            Method.PUT, url,
            { response ->
                println("Response is: $response")
            },
            { println("That didn't work!") }) {
            override fun getBodyContentType(): String {
                return "application/json; charset=utf-8"
            }

            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                headers["Authorization"] =
                    "Bearer 5927728152:AAExhfEpagD__0D9A6b_qJs56SuXV06oZ-8"
                return headers
            }

            override fun getBody(): ByteArray {
                val gson = Gson()
                val json = gson.toJson(listId)
                return json.toByteArray()
            }
        }
        queue.add(stringRequest)
    }
    private fun isSmsPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            "android.permission.SEND_SMS"
        ) == PackageManager.PERMISSION_GRANTED
    }
    private fun requestSendSmsPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf("android.permission.SEND_SMS"),
            PERMISSION_REQUEST_SEND_SMS
        )
    }
    private fun removeItem() {
        idList?.removeAt(0)
        phoneList.removeAt(0)
        messageList.removeAt(0)
        messageTypeList.removeAt(0)
        statusList.removeAt(0)
        adapter?.notifyDataSetChanged()
    }
    private fun <E> List<E>?.removeAt(position: Int) {
        val mutableList = this as MutableList<E>
        mutableList.reversed()
        mutableList.removeAt(position)
    }
    private fun getIPAdress():String{
        var ipAddress = ""
        val data = sharedPreferences?.getString("ipAddress", "").toString()
        for (i in data.split(",").indices) {
            if (data.split(",")[i].contains("@1")) {
                ipAddress = data.split(",")[i].split("@1")[0]
                break
            } else {
                ipAddress = data.split(",")[0].replace("@0", "")
            }
        }
        return ipAddress.replace("[", "").replace("]", "").replace("\"", "")
    }
    private fun getSmsLimit(): Int {
        return sharedPreferences?.getString("smsLimit", "")!!.toInt()
    }
}