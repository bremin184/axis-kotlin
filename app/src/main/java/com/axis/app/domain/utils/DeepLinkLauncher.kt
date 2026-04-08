package com.axis.app.domain.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Handles launching the M-Pesa app with pre-filled parameters.
 */
object DeepLinkLauncher {

    private const val MPESA_PACKAGE = "com.safaricom.mpesa.services"

    fun launchSend(context: Context, phone: String, amount: Double) {
        try {
            // M-Pesa sometimes supports specific deep links for STK push or pre-filling
            // We'll use a standard view intent with custom data or fallback to package launch
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("mpesa://sendmoney?phone=$phone&amount=$amount")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            val packageManager = context.packageManager
            val resolved = intent.resolveActivity(packageManager)
            
            if (resolved != null) {
                context.startActivity(intent)
            } else {
                // Fallback: Just open the app if specific deep link fails
                val launchIntent = packageManager.getLaunchIntentForPackage(MPESA_PACKAGE)
                if (launchIntent != null) {
                    context.startActivity(launchIntent)
                } else {
                    Toast.makeText(context, "M-Pesa app not found", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Could not launch M-Pesa: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
