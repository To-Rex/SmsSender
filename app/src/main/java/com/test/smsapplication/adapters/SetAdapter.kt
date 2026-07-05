package com.test.smsapplication.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageButton
import android.widget.TextView
import com.test.smsapplication.R

class SetAdapter(
    private val context: Context,
    private val itemList: List<String>,
    private val itemList1: List<String>,
    private val callback: MyAdapterCallback
) : BaseAdapter() {
    @SuppressLint("UseCompatLoadingForDrawables")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        var convertView = convertView

        if (convertView == null) {
            convertView =
                LayoutInflater.from(context).inflate(R.layout.item_settings, parent, false)
        }

        val txtItemMessage = convertView!!.findViewById<TextView>(R.id.txtItemSettings)
        val imgItemSettings = convertView.findViewById<ImageButton>(R.id.imgItemSettings)
        val itemSettings = convertView.findViewById<View>(R.id.itemSettings)
        if (itemList[position].contains("0")) {
            txtItemMessage.text = itemList1[position].replace("@0", "")
            itemSettings.setBackgroundResource(R.drawable.bg_item_settings_inactive)
            imgItemSettings.setBackgroundResource(R.drawable.bg_delete_btn)
        } else {
            txtItemMessage.text = itemList1[position].replace("@1", "")
            itemSettings.setBackgroundResource(R.drawable.bg_item_settings_active)
            imgItemSettings.setBackgroundResource(R.drawable.bg_delete_btn)
        }

        imgItemSettings.setOnClickListener {
            callback.onDeleteButtonClicked(position)
        }

        convertView.setOnClickListener {
            callback.onItemClicked(position)
        }
        return convertView
    }
    override fun getItem(position: Int): Any {
        return itemList[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getCount(): Int {
        return itemList.size
    }
}

interface MyAdapterCallback {
    fun onDeleteButtonClicked(position: Int)
    fun onItemClicked(position: Int)
}
