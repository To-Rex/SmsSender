package com.test.smsapplication.ui.dashboard

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.gson.Gson
import com.test.smsapplication.R
import com.test.smsapplication.adapters.DashAdapter
import com.test.smsapplication.models.SmsData

class DashboardFragment : Fragment() {

    var btnDashRefresh: Button? = null
    var dashList: ListView? = null
    private var sharedPreferences: SharedPreferences? = null
    private var adapter: DashAdapter? = null
    private var id_list: MutableList<Int>? = null
    private var phone_list: MutableList<String>? = null
    private var message_list: MutableList<String>? = null
    private var message_type_list: MutableList<String>? = null
    private var status_list: MutableList<Int>? = null


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        btnDashRefresh = view.findViewById(R.id.btnDashRefresh)
        dashList = view.findViewById(R.id.dashList)


        dashList!!.divider = null
        dashList!!.dividerHeight = 20
        sharedPreferences = activity?.getSharedPreferences("teda.uz", 0)
        getData()

        btnDashRefresh!!.setOnClickListener {
            //clear shared preferences data
            sharedPreferences = activity?.getSharedPreferences("teda.uz", 0)
            val editor = sharedPreferences?.edit()
            editor?.putString("smsData", "")
            editor?.apply()
            getData()
        }

        return view

    }

    @SuppressLint("SetTextI18n")
    private fun getData() {
        dashList!!.adapter = null

        val getPref = sharedPreferences?.getString("smsData", "")
        val gson = Gson()
        if (getPref == null || getPref.isEmpty()) {
            return
        } else {
            val data = gson.fromJson(getPref, Array<SmsData>::class.java).toList()

            id_list = ArrayList()
            phone_list = ArrayList()
            message_list = ArrayList()
            message_type_list = ArrayList()
            status_list = ArrayList()


            for (i in data.indices) {
                id_list!!.add(data[i].idList!!)
                phone_list!!.add(data[i].phoneNumber!!)
                message_list!!.add(data[i].message!!)
                //message_type_list!!.add(data[i].messageType!!)
                //Toast.makeText(activity, "data: ${data[i].messageType}", Toast.LENGTH_SHORT).show()
            }
            btnDashRefresh!!.text = "${data.size} ta SMS larni tozalash"

            adapter = DashAdapter(activity!!, phone_list!!, message_list!!, phone_list!!)
            dashList!!.adapter = adapter
        }
    }
}