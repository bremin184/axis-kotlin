package com.axis.app.sms

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri

/**
 * Reads M-Pesa SMS messages from the device inbox via content provider.
 * Requires READ_SMS permission.
 */
class SmsReader(private val contentResolver: ContentResolver) {

    companion object {
        private val SMS_URI: Uri = Uri.parse("content://sms/inbox")
        private const val MPESA_ADDRESS = "MPESA"
    }

    /**
     * Read all M-Pesa SMS messages from the inbox.
     * @param sinceTimestamp Only read messages after this Unix timestamp (millis). Default 0 = all.
     * @return List of raw SMS body strings paired with their timestamps.
     */
    fun readMpesaSms(sinceTimestamp: Long = 0): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()

        val selection = if (sinceTimestamp > 0) {
            "address = ? AND date > ?"
        } else {
            "address = ?"
        }

        val selectionArgs = if (sinceTimestamp > 0) {
            arrayOf(MPESA_ADDRESS, sinceTimestamp.toString())
        } else {
            arrayOf(MPESA_ADDRESS)
        }

        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                SMS_URI,
                arrayOf("_id", "body", "date"),
                selection,
                selectionArgs,
                "date DESC"
            )

            cursor?.let {
                val bodyIndex = it.getColumnIndexOrThrow("body")
                val dateIndex = it.getColumnIndexOrThrow("date")

                while (it.moveToNext()) {
                    val body = it.getString(bodyIndex) ?: continue
                    val date = it.getLong(dateIndex)
                    messages.add(SmsMessage(body = body, timestamp = date))
                }
            }
        } finally {
            cursor?.close()
        }

        return messages
    }

    /**
     * Count total M-Pesa messages in inbox.
     */
    fun countMpesaSms(): Int {
        var count = 0
        var cursor: Cursor? = null
        try {
            cursor = contentResolver.query(
                SMS_URI,
                arrayOf("_id"),
                "address = ?",
                arrayOf(MPESA_ADDRESS),
                null
            )
            count = cursor?.count ?: 0
        } finally {
            cursor?.close()
        }
        return count
    }
}

/**
 * Raw SMS data from content provider.
 */
data class SmsMessage(
    val body: String,
    val timestamp: Long
)
