package com.test.smsapplication.ui.settings
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.test.smsapplication.R
import com.test.smsapplication.adapters.MyAdapterCallback
import com.test.smsapplication.adapters.SetAdapter

class SettingsFragment : Fragment() {

    var ediSetIpAdress: EditText? = null
    var ediSetPhone: EditText? = null
    var btnSetAdd: ImageView? = null
    var btnSetAddPhone: ImageView? = null
    var txtSetIpAdress: TextView? = null
    var txtSetPhone: TextView? = null
    var listSettings: ListView? = null

    private var imgSetSound: ImageView? = null
    private var imgSetSounds: ImageView? = null
    private var data = ""

    var linkList = ArrayList<String>()
    var verList = ArrayList<String>()
    var statusList = ArrayList<String>()
    var ipList = ArrayList<String>()
    private var adapter: SetAdapter? = null

    var sharedPreferences: SharedPreferences? = null

    @SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        ediSetIpAdress = view.findViewById(R.id.ediSetIpAdress)
        ediSetPhone = view.findViewById(R.id.ediSetPhone)
        btnSetAdd = view.findViewById(R.id.btnSetAdd)
        btnSetAddPhone = view.findViewById(R.id.btnSetAddPhone)
        txtSetIpAdress = view.findViewById(R.id.txtSetIpAdress)
        txtSetPhone = view.findViewById(R.id.txtSetPhone)
        listSettings = view.findViewById(R.id.listSettings)

        imgSetSound = view.findViewById(R.id.imgSetSound)
        imgSetSounds = view.findViewById(R.id.imgSetSounds)

        sharedPreferences = activity?.getSharedPreferences("teda.uz", 0)
        data = sharedPreferences?.getString("ipAddress", "").toString()

        listSettings!!.adapter = null
        listSettings!!.divider = null
        listSettings!!.dividerHeight = 20

        getData()
        val routerIpAddress = getRouterIpAddress(requireContext())

        btnSetAdd!!.setOnClickListener {
            if (ediSetIpAdress?.text.toString() == "") {
                ediSetIpAdress?.error = "Ip nomini kiriting"
                return@setOnClickListener
            }
            if (ediSetIpAdress?.text.toString().trim().last() != '/') {
                ediSetIpAdress?.setText(ediSetIpAdress?.text.toString().trim() + "/")
            }
            if (ediSetIpAdress?.text.toString().contains("localhost")) {
                Toast.makeText(context, "localhost o'zgaradi $routerIpAddress", Toast.LENGTH_SHORT).show()
                ediSetIpAdress?.setText(ediSetIpAdress?.text.toString().replace("localhost", routerIpAddress
                ))
            }else if (!ediSetIpAdress?.text.toString().contains("http://") && !ediSetIpAdress?.text.toString().contains("https://")) {
                ediSetIpAdress?.error = "http:// yoki https:// qo'shing"
                return@setOnClickListener
            }

            ipList.add(ediSetIpAdress?.text.toString().trim() + "@0")
            val json = Gson().toJson(ipList)
            sharedPreferences?.edit()?.putString("ipAddress", json)?.apply()
            ediSetIpAdress?.text?.clear()
            getData()
        }
        btnSetAddPhone!!.setOnClickListener {
            if (ediSetPhone?.text.toString() == "") {
                ediSetPhone?.error = "Telefon raqamini kiriting"
                return@setOnClickListener
            }
            sharedPreferences?.edit()?.putString("phone", "+998"+ediSetPhone?.text.toString())?.apply()
            ediSetPhone?.text?.clear()
            getData()
        }
        txtSetIpAdress?.setOnClickListener {
            Toast.makeText(context, "Ip addressni o'zgartirish", Toast.LENGTH_SHORT).show()
            val alertDialog = AlertDialog.Builder(context)
            alertDialog.setCancelable(false)
            alertDialog.setTitle("Smslaringizni sonini")
            alertDialog.setMessage("Smslaringizni sonini kiriting! (minimal 50 ta)")
            val input = EditText(context)
            alertDialog.setView(input)
            input.inputType = 2
            input.hint = "Smslar soni kiriting"
            input.background = resources.getDrawable(R.drawable.edit_text_back)

            alertDialog.setPositiveButton("Kiritish") { dialog, _ ->
                val smsLimit = input.text.toString()
                if (smsLimit.trim().isEmpty()) {
                    showDialog("Xatolik", "Smslar soni kiriting")
                } else if (smsLimit.toInt() < 50) {
                    showDialog("Xatolik", "Smslar soni minimal 50 ta")
                } else if (smsLimit.toInt() > 5000) {
                    showDialog("Xatolik", "Smslar soni maximal 5000 ta")
                } else {
                    sharedPreferences?.edit()?.putString("smsLimit", smsLimit)?.apply()
                    txtSetIpAdress!!.text = "Sms Limit: $smsLimit"
                    dialog.dismiss()
                    showDialog("Xabar", "Smslar soni saqlandi")
                }
            }
            alertDialog.setNegativeButton("Bekor qilish") { dialog, _ ->
                dialog.dismiss()
            }
            alertDialog.show()
        }

        imgSetSound?.setOnClickListener {
            if (sharedPreferences?.getString("sound", "0") == "0") {
                sharedPreferences?.edit()?.putString("sound", "1")?.apply()
                getData()
            }else if (sharedPreferences?.getString("sound", "0") == "1") {
                sharedPreferences?.edit()?.putString("sound", "2")?.apply()
                getData()
            }else{
                sharedPreferences?.edit()?.putString("sound", "0")?.apply()
                getData()
            }
            Toast.makeText(context, sharedPreferences?.getString("sound", "0"), Toast.LENGTH_SHORT).show()
        }
        imgSetSounds?.setOnClickListener {
            if (sharedPreferences?.getString("sounds", "0") == "0") {
                sharedPreferences?.edit()?.putString("sounds", "1")?.apply()
                getData()
            }else{
                sharedPreferences?.edit()?.putString("sounds", "0")?.apply()
                getData()
            }
            Toast.makeText(context, sharedPreferences?.getString("sounds", "0"), Toast.LENGTH_SHORT).show()
        }

        txtSetPhone?.setOnClickListener {
            ediSetPhone?.visibility = View.VISIBLE
            btnSetAddPhone?.visibility = View.VISIBLE
        }
        return view
    }

    private fun getRouterIpAddress(context: Context): String {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiInfo: WifiInfo? = wifiManager.connectionInfo
        val ipAddress: Int = wifiInfo?.ipAddress ?: 0

        return String.format(
            "%d.%d.%d.%d",
            (ipAddress and 0xff),
            (ipAddress shr 8 and 0xff),
            (ipAddress shr 16 and 0xff),
            (ipAddress shr 24 and 0xff)
        )
    }

    private fun showDialog(title: String, message: String) {
        val alertDialog = AlertDialog.Builder(context)
        alertDialog.setCancelable(false)
        alertDialog.setTitle(title)
        alertDialog.setMessage(message)
        alertDialog.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        alertDialog.show()
    }

    @SuppressLint("SetTextI18n")
    private fun getData(){
        linkList.clear()
        verList.clear()
        statusList.clear()
        listSettings?.adapter = null
        sharedPreferences = activity?.getSharedPreferences("teda.uz", 0)
        data = sharedPreferences?.getString("ipAddress", "").toString()
        if (sharedPreferences?.getString("phone", "") == ""){
            Toast.makeText(activity, "Telefon raqami kiritilmagan", Toast.LENGTH_SHORT).show()
            txtSetPhone?.text = "phone number"
        }else{
            txtSetPhone?.text = sharedPreferences?.getString("phone", "")
            ediSetPhone?.visibility = View.GONE
            btnSetAddPhone?.visibility = View.GONE
        }
        val data = sharedPreferences?.getString("ipAddress", "").toString()
        if (data == ""||data == "[]"){
            Toast.makeText(activity, "Ip nomi kiritilmagan😳\uD83D\uDE33\uD83D\uDE33", Toast.LENGTH_SHORT).show()
            ediSetIpAdress?.error = "Ip nomini kiriting"
            return
        }
        if (sharedPreferences?.getString("smsLimit", "") == ""||sharedPreferences?.getString("smsLimit", "") == "0"){
            sharedPreferences?.edit()?.putString("smsLimit", "50")?.apply()
            txtSetIpAdress?.text = "Sms Limit: 50" //default sms limit
        }else{
            txtSetIpAdress?.text = "Sms Limit: "+sharedPreferences?.getString("smsLimit", "")
        }

        ipList = Gson().fromJson(data, Array<String>::class.java).toCollection(ArrayList())
        if (ipList.size > 0) {
            for (i in 0 until ipList.size) {
                val ip = ipList[i].split("@")
                linkList.add(ip[0])
                verList.add(ip[1])
            }
            listSettings?.adapter = adapter
            listSettings?.adapter = SetAdapter(requireContext(), verList, linkList, object :
                MyAdapterCallback {
                override fun onDeleteButtonClicked(position: Int) {
                    ipList.removeAt(position)
                    val json = Gson().toJson(ipList)
                    sharedPreferences?.edit()?.putString("ipAddress", json)?.apply()
                    getData()
                }
                override fun onItemClicked(position: Int) {
                    val dialog = AlertDialog.Builder(context)
                    dialog.setIcon(android.R.drawable.ic_dialog_alert)
                    dialog.setTitle("Bu Ip adresini faollashtirish yoki to'xtatish")
                    dialog.setPositiveButton(if (verList[position] == "0") "Faollashtirish" else "To'xtatish") { dialog, _ ->
                        if (verList[position] == "0") {
                            verList[position] = "1"
                        } else {
                            verList[position] = "0"
                        }
                        for (i in 0 until ipList.size) {
                            if (i == position) {
                                ipList[i] = linkList[i] + "@" + verList[i]
                            } else {
                                ipList[i] = linkList[i] + "@0"
                            }
                        }
                        val json = Gson().toJson(ipList)
                        sharedPreferences?.edit()?.putString("ipAddress", json)?.apply()
                        getData()
                        dialog.dismiss()
                    }
                    dialog.setNegativeButton("Bekor") { dialog, _ ->
                        dialog.dismiss()
                    }
                    dialog.show()
                }
            })
        }

        if (sharedPreferences?.getString("sound", "0")==null){
            sharedPreferences?.edit()?.putString("sound", "0")?.apply()
        }
        if (sharedPreferences?.getString("sounds", "0")==null){
            sharedPreferences?.edit()?.putString("sounds", "0")?.apply()
        }

        if (sharedPreferences?.getString("sound", "0") == "0") {
            imgSetSound?.setImageResource(R.drawable.sound_on)
        }
        if (sharedPreferences?.getString("sound", "0") == "1") {
            imgSetSound?.setImageResource(R.drawable.vibration_icon)
        }
        if (sharedPreferences?.getString("sound", "0") == "2") {
            imgSetSound?.setImageResource(R.drawable.sound_off)
        }

        if (sharedPreferences?.getString("sounds", "0") == "0") {
            imgSetSounds?.setImageResource(R.drawable.sound_1)
        }
        if (sharedPreferences?.getString("sounds", "0") == "1") {
            imgSetSounds?.setImageResource(R.drawable.sound_2)
        }

        /*for (i in 0 until verList.size) {
            if (verList[i] == "1") {
                txtSetIpAdress?.text = linkList[i]
                break
            }else{
                txtSetIpAdress?.text = linkList[0]
            }
        }*/
    }
}