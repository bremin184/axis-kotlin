package com.axis.app.sms

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler

class SmsObserver(
    private val context: Context,
    handler: Handler,
    private val onSmsReceived: () -> Unit
) : ContentObserver(handler) {

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        onSmsReceived()
    }
}
