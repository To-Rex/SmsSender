package com.test.smsapplication.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.telephony.SmsManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AbsListView.OnScrollListener.SCROLL_STATE_IDLE
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.test.smsapplication.R
import com.test.smsapplication.Sample
import com.test.smsapplication.adapters.DashAdapter
import com.test.smsapplication.models.ModelData
import com.test.smsapplication.models.SmsData

class HomeFragment : Fragment() {

    private var sharedPreferences: SharedPreferences? = null
    private lateinit var smsManager: SmsManager
    private val handler: Handler = Handler()
    var homeList: ListView? = null
    private var txtHomipAdress: TextView? = null
    private var txtHomipLimt: TextView? = null
    private var btnHomNewSms: Button? = null
    private var btnHomSendSms: Button? = null
    private var adapter: DashAdapter? = null
    private var id_list: MutableList<Int>? = null
    private var phone_list: MutableList<String>? = null
    private var message_list: MutableList<String>? = null
    private var message_type_list: MutableList<String>? = null
    private var status_list: MutableList<Int>? = null
    var swipeRefreshHome: SwipeRefreshLayout? = null
    private var smsLimit = 0
    private var mediaPlayer: MediaPlayer? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(
            R.layout.fragment_home,
            container,
            false
        )

        homeList = view.findViewById(R.id.homeList)
        txtHomipAdress = view.findViewById(R.id.txtHomipAdress)
        txtHomipLimt = view.findViewById(R.id.txtHomipLimt)
        btnHomNewSms = view.findViewById(R.id.btnHomNewSms)
        btnHomSendSms = view.findViewById(R.id.btnHomSendSms)
        swipeRefreshHome = view.findViewById(R.id.swipeRefreshHome)

        homeList!!.divider = null
        homeList!!.dividerHeight = 20
        sharedPreferences = activity?.getSharedPreferences("teda.uz", 0)

        if (!isSmsPermissionGranted()) {
            val alertDialog = AlertDialog.Builder(requireActivity())
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
            getData()
        }

        swipeRefreshHome?.setOnRefreshListener {
            if (!isSmsPermissionGranted()) {
                val alertDialog = AlertDialog.Builder(requireActivity())
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
                getData()
            }
        }
        homeList?.setOnScrollListener(object : AbsListView.OnScrollListener {
            override fun onScrollStateChanged(view: AbsListView?, scrollState: Int) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    if (homeList?.firstVisiblePosition == 0) {
                        swipeRefreshHome?.isEnabled = true
                    }
                }
            }

            override fun onScroll(
                view: AbsListView?,
                firstVisibleItem: Int,
                visibleItemCount: Int,
                totalItemCount: Int
            ) {
                if (firstVisibleItem > 0) {
                    swipeRefreshHome?.isEnabled = false
                }
            }
        })

        btnHomNewSms!!.setOnClickListener {
            if (!isSmsPermissionGranted()) {
                val alertDialog = AlertDialog.Builder(requireActivity())
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
                getNewData()
            }
        }

        btnHomSendSms!!.setOnClickListener {
            sendSms(
                id_list,
                phone_list,
                message_list,
                message_type_list,
                status_list,
                requireContext()
            )
        }

        return view
    }



    @SuppressLint("SetTextI18n")
    private fun getData() {
        var ipAddress = ""
        val data = sharedPreferences?.getString("ipAddress", "").toString()
        if (data == "") {
            return
        }
        for (i in data.split(",").indices) {
            if (data.split(",")[i].contains("@1")) {
                ipAddress = data.split(",")[i].split("@1")[0]
                break
            } else {
                ipAddress = data.split(",")[0].replace("@0", "")
            }
        }
        txtHomipAdress!!.text = ipAddress.replace("[", "").replace("]", "").replace("\"", "")
        txtHomipLimt!!.text = "${sharedPreferences?.getString("smsLimit", "")} ta sms"
        smsLimit = sharedPreferences?.getString("smsLimit", "")!!.toInt()
        if (smsLimit <= 50) {
            txtHomipLimt!!.setTextColor(Color.RED)
        }

        val queue = Volley.newRequestQueue(activity)

        val phone = sharedPreferences?.getString("phone", "")
        //val url = "http://185.185.80.245:77/api/sms/status?page=0&size=50&employeePhone=998977515747&status=2"
        val url = "${txtHomipAdress?.text}api/sms/status?page=0&size=100&employeePhone=${
            phone?.substring(1)
        }&status=2"
        val stringRequest = StringRequest(
            Request.Method.GET, url, { response ->
                val gson = Gson()
                val json = gson.fromJson(response, ModelData::class.java)
                println(response)
                id_list = ArrayList()
                phone_list = ArrayList()
                message_list = ArrayList()
                message_type_list = ArrayList()
                status_list = ArrayList()
                if (json.data.content.isEmpty()) {
                    Toast.makeText(context, "Ma'lumot topilmadi", Toast.LENGTH_LONG).show()
                    handler.postDelayed({
                        getNewData()
                    }, 7000)
                } else {
                    for (i in json.data.content.indices) {
                        id_list?.add(json.data.content[i].id)
                        phone_list?.add(json.data.content[i].tel)
                        message_list?.add(json.data.content[i].zapros)
                        message_type_list?.add(json.data.content[i].platforma)
                        status_list?.add(json.data.content[i].flag)
                    }

                    sendTypeSms(
                        id_list,
                        phone_list,
                        message_list,
                        message_type_list,
                        status_list,
                        requireContext()
                    )

                    btnHomSendSms!!.text = "(${phone_list?.size}) ta sms yuborish"
                    adapter = DashAdapter(
                        requireContext(),
                        phone_list!!,
                        message_list!!,
                        message_type_list!!
                    )
                    homeList!!.adapter = adapter
                    swipeRefreshHome?.isRefreshing = false
                    mediaPlayer?.stop()
                }
            }, {
                swipeRefreshHome?.isRefreshing = false


                mediaPlayer = MediaPlayer.create(requireContext(), R.raw.errors)
                mediaPlayer?.start()
                Toast.makeText(context, "That didn't work!", Toast.LENGTH_LONG).show()
            })
        queue.add(stringRequest)
    }

    @SuppressLint("SetTextI18n")
    private fun sendSms(
        idList: MutableList<Int>?,
        phoneList: MutableList<String>?,
        messageList: MutableList<String>?,
        messageTypeList: MutableList<String>?,
        statusList: MutableList<Int>?,
        requireContext: Context) {
        if (messageList?.size != 0 && phoneList?.size != 0 && idList?.size != 0 && smsLimit > 0) {
            val id = idList?.get(0)
            val message = messageList?.get(0)
            val phoneNumber = phoneList?.get(0)
            val messages = smsManager.divideMessage(message)
            val messageType = messageTypeList?.get(0)
            for (msg in messages) {
                smsManager.sendTextMessage(phoneNumber, null, msg, null, null)
                smsLimit--
                txtHomipLimt?.text = "$smsLimit ta sms"
                if (sharedPreferences?.getString("sound", "0") == "0") {
                    mediaPlayer = MediaPlayer.create(requireContext(), R.raw.sound)
                    mediaPlayer?.start()
                }
                if (sharedPreferences?.getString("vibration", "0") == "1") {
                    Toast.makeText(requireContext(), "Vibration", Toast.LENGTH_SHORT).show()
                    val vibrator =
                        requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                    vibrator.vibrate(500)
                }
                updateSms(idList?.get(0))
            }
            val editor = sharedPreferences?.edit()
            editor?.putString("smsLimit", smsLimit.toString())
            editor?.apply()
            saveSms(message!!, phoneNumber!!, id!!)
            removeItem(0)
            if (messageList.size > 0) {
                handler.postDelayed({
                    sendSms(
                        idList,
                        phoneList,
                        messageList,
                        messageTypeList,
                        statusList,
                        requireContext)
                }, Sample.DELAY_BETWEEN_SMS)
            } else {
                Toast.makeText(requireContext(), "Sms tugadi", Toast.LENGTH_SHORT).show()
                if (sharedPreferences?.getString("sounds", "0") == "0") {
                    Handler().postDelayed({
                        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.sounds)
                        mediaPlayer?.start()
                    }, 1000)
                }
                if (sharedPreferences?.getString("vibration", "0") == "1") {
                    Toast.makeText(requireContext(), "Vibration", Toast.LENGTH_SHORT).show()
                    val vibrator =
                        requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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

    @SuppressLint("SetTextI18n")
    private fun sendTypeSms(
        idList: MutableList<Int>?,
        phoneList: MutableList<String>?,
        messageList: MutableList<String>?,
        messageTypeList: MutableList<String>?,
        statusList: MutableList<Int>?,
        requireContext: Context
    ) {
        if (messageList?.size != 0 && phoneList?.size != 0 && idList?.size != 0 && smsLimit > 0) {
            val id = idList?.get(0)
            val message = messageList?.get(0)
            val phoneNumber = phoneList?.get(0)
            val messages = smsManager.divideMessage(message)
            val messageType = messageTypeList?.get(0)
            //if (messageType != null){ toast(messageType) }
            if (!messageType.isNullOrEmpty()) {
                for (msg in messages) {
                    smsManager.sendTextMessage(phoneNumber, null, msg, null, null)
                    smsLimit--
                    txtHomipLimt?.text = "$smsLimit ta sms"
                    if (sharedPreferences?.getString("sound", "0") == "0") {
                        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.sound)
                        mediaPlayer?.start()
                    }
                    if (sharedPreferences?.getString("vibration", "0") == "1") {
                        Toast.makeText(requireContext(), "Vibration", Toast.LENGTH_SHORT).show()
                        val vibrator =
                            requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        vibrator.vibrate(500)
                    }
                    updateSms(idList?.get(0))
                }
                val editor = sharedPreferences?.edit()
                editor?.putString("smsLimit", smsLimit.toString())
                editor?.apply()
                saveSms(message!!, phoneNumber!!, id!!)
                removeItem(0)
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
                }, Sample.DELAY_BETWEEN_SMS)
            } else {
                Toast.makeText(requireContext(), "Sms tugadi", Toast.LENGTH_SHORT).show()
                if (sharedPreferences?.getString("sounds", "0") == "0") {
                    Handler().postDelayed({
                        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.sounds)
                        mediaPlayer?.start()
                    }, 1000)
                }
                if (sharedPreferences?.getString("vibration", "0") == "1") {
                    Toast.makeText(requireContext(), "Vibration", Toast.LENGTH_SHORT).show()
                    val vibrator =
                        requireActivity().getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
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

    private fun updateSms(get: Int?) {
        val queue = Volley.newRequestQueue(activity)
        val urls: String = if (txtHomipAdress?.text.toString().last() == '/') {
            txtHomipAdress?.text.toString().substring(0, txtHomipAdress?.text.toString().length - 1)
        } else {
            txtHomipAdress?.text.toString()
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

    private fun removeItem(position: Int) {
        id_list?.removeAt(position)
        phone_list.removeAt(position)
        message_list.removeAt(position)
        message_type_list.removeAt(position)
        status_list.removeAt(position)
        adapter?.notifyDataSetChanged()
    }

    private fun saveSms(message: String, phoneNumber: String, idList: Int) {
        sharedPreferences = activity?.getSharedPreferences("teda.uz", 0)
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

    private fun isSmsPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireActivity(),
            "android.permission.SEND_SMS"
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSendSmsPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf("android.permission.SEND_SMS"),
            Sample.PERMISSION_REQUEST_SEND_SMS
        )
    }

    @SuppressLint("SetTextI18n")
    private fun getNewData() {
        var ipAddress = ""
        val data = sharedPreferences?.getString("ipAddress", "").toString()
        if (data == "") {
            return
        }
        for (i in data.split(",").indices) {
            if (data.split(",")[i].contains("@1")) {
                ipAddress = data.split(",")[i].split("@1")[0]
                break
            } else {
                ipAddress = data.split(",")[0].replace("@0", "")
            }
        }
        txtHomipAdress!!.text = ipAddress.replace("[", "").replace("]", "").replace("\"", "")
        txtHomipLimt!!.text = "${sharedPreferences?.getString("smsLimit", "")} ta sms"
        smsLimit = sharedPreferences?.getString("smsLimit", "")!!.toInt()
        if (smsLimit <= 50) {
            txtHomipLimt!!.setTextColor(Color.RED)
        }

        val queue = Volley.newRequestQueue(activity)

        val phone = sharedPreferences?.getString("phone", "")
        //val url = "http://185.185.80.245:77/api/sms/status?page=0&size=50&employeePhone=998977515747&status=2"
        //val url = "${txtHomipAdress?.text}api/sms/status?page=0&size=50&employeePhone=998977515747&status=2"
        val url = "${txtHomipAdress?.text}api/sms/status?page=0&size=100&employeePhone=${
            phone?.substring(1)
        }&status=1"
        val stringRequest = StringRequest(
            Request.Method.GET, url, {
                getData()
            }, {
                swipeRefreshHome?.isRefreshing = false
                Toast.makeText(context, "That didn't work!", Toast.LENGTH_LONG).show()
            })
        queue.add(stringRequest)
    }
}

private fun <E> List<E>?.removeAt(position: Int) {
    val mutableList = this as MutableList<E>
    mutableList.reversed()
    mutableList.removeAt(position)
}
