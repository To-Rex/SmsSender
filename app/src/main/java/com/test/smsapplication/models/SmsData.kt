package com.test.smsapplication.models

class SmsData(idList: Int, phoneNumber: String, message: String) {
    var idList: Int? = null
    var phoneNumber: String? = null
    var message: String? = null
    //var messageType: String? = null
    init {
        this.idList = idList
        this.phoneNumber = phoneNumber
        this.message = message
        //this.messageType = messageType
    }
}