package com.aiva.assistant.engine

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import com.aiva.assistant.data.SecureVault
import com.aiva.assistant.model.ActionType
import com.aiva.assistant.model.AnalyzedCommand

class PhoneActionExecutor(
    private val context: Context,
    private val vault: SecureVault
) {

    data class ExecutionResult(val success: Boolean, val message: String)

    fun execute(command: AnalyzedCommand): ExecutionResult {
        return when (command.actionType) {
            ActionType.BLOCKED -> ExecutionResult(false, "Blocked for your phone's security.")
            ActionType.CALL -> executeCall(command.parameters["recipient"].orEmpty())
            ActionType.UPI_PAY -> executeUpi(
                upiId = command.parameters["upiId"].orEmpty(),
                name = command.parameters["recipient"].orEmpty(),
                amount = command.parameters["amount"].orEmpty(),
                note = command.parameters["note"].orEmpty()
            )
            ActionType.WHATSAPP -> executeWhatsApp(
                recipient = command.parameters["recipient"].orEmpty(),
                message = command.parameters["message"].orEmpty()
            )
            ActionType.SMS -> executeSms(
                recipient = command.parameters["recipient"].orEmpty(),
                message = command.parameters["message"].orEmpty()
            )
            ActionType.ALARM -> executeAlarm(
                hour = command.parameters["hour"]?.toIntOrNull() ?: 7,
                minute = command.parameters["minute"]?.toIntOrNull() ?: 0
            )
            ActionType.TIMER -> executeTimer(command.parameters["seconds"]?.toIntOrNull() ?: 60)
            ActionType.OPEN_APP -> executeOpenApp(command.parameters["appName"].orEmpty())
            ActionType.MAPS -> executeMaps(command.parameters["destination"].orEmpty())
            ActionType.YOUTUBE -> executeYouTube(command.parameters["query"].orEmpty())
            ActionType.NOTE -> {
                val note = command.parameters["note"].orEmpty().ifBlank { command.rawText }
                vault.addNote(note)
                ExecutionResult(true, "Saved note: \"$note\"")
            }
            ActionType.WEB_SEARCH, ActionType.UNKNOWN -> executeWebSearch(
                command.parameters["query"].orEmpty().ifBlank { command.rawText }
            )
        }
    }

    private fun executeCall(recipient: String): ExecutionResult {
        val number = resolvePhoneNumber(recipient)
            ?: return ExecutionResult(false, "No phone number found for '$recipient'. Save an alias in the Vault tab or check Contacts permission.")

        val hasCallPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED
        val intent = Intent(if (hasCallPerm) Intent.ACTION_CALL else Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${Uri.encode(number)}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ExecutionResult(true, if (hasCallPerm) "Calling $recipient ($number)..." else "Opened dialer for $recipient ($number)")
    }

    private fun executeUpi(upiId: String, name: String, amount: String, note: String): ExecutionResult {
        if (upiId.isBlank()) {
            return ExecutionResult(false, "UPI ID is missing for '$name'. Open the Vault tab and save their UPI ID first.")
        }
        if (amount.isBlank() || (amount.toDoubleOrNull() ?: 0.0) <= 0.0) {
            return ExecutionResult(false, "Invalid payment amount.")
        }

        val uri = Uri.Builder()
            .scheme("upi")
            .authority("pay")
            .appendQueryParameter("pa", upiId)
            .appendQueryParameter("pn", name.ifBlank { "Recipient" })
            .appendQueryParameter("am", amount)
            .appendQueryParameter("cu", "INR")
            .appendQueryParameter("tn", note.ifBlank { "Payment via AIVA" })
            .build()

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            val chooser = Intent.createChooser(intent, "Complete ₹$amount UPI Payment").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            ExecutionResult(true, "Opened UPI app for ₹$amount to $name. Verify & enter your UPI PIN.")
        } catch (_: Exception) {
            ExecutionResult(false, "No UPI app (GPay, PhonePe, Paytm, BHIM) found on this phone.")
        }
    }

    private fun executeWhatsApp(recipient: String, message: String): ExecutionResult {
        val rawNumber = resolvePhoneNumber(recipient)?.replace(Regex("[^0-9+]"), "")
        val url = if (!rawNumber.isNullOrBlank()) {
            val formatted = if (rawNumber.length == 10) "91$rawNumber" else rawNumber.removePrefix("+")
            "https://wa.me/$formatted?text=${Uri.encode(message)}"
        } else {
            "https://wa.me/?text=${Uri.encode(message)}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ExecutionResult(true, "Opened WhatsApp for ${recipient.ifBlank { "message" }}.")
        } catch (_: Exception) {
            ExecutionResult(false, "Could not open WhatsApp.")
        }
    }

    private fun executeSms(recipient: String, message: String): ExecutionResult {
        val number = resolvePhoneNumber(recipient) ?: ""
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}")).apply {
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ExecutionResult(true, "Opened SMS app for ${recipient.ifBlank { "recipient" }}.")
    }

    private fun executeAlarm(hour: Int, minute: Int): ExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, "AIVA Alarm")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ExecutionResult(true, String.format("Set alarm for %02d:%02d.", hour, minute))
        } catch (_: Exception) {
            ExecutionResult(false, "No Clock app available to set alarm.")
        }
    }

    private fun executeTimer(seconds: Int): ExecutionResult {
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds.coerceAtLeast(1))
            putExtra(AlarmClock.EXTRA_MESSAGE, "AIVA Timer")
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ExecutionResult(true, "Started timer for $seconds seconds.")
        } catch (_: Exception) {
            ExecutionResult(false, "No Clock app available to start timer.")
        }
    }

    private fun executeOpenApp(appName: String): ExecutionResult {
        if (appName.isBlank()) return ExecutionResult(false, "Please say the name of the app to open.")
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(mainIntent, 0)
        val query = appName.lowercase().trim()
        val match = apps.firstOrNull {
            it.loadLabel(pm).toString().lowercase().contains(query)
        } ?: return ExecutionResult(false, "Could not find an installed app named '$appName'.")

        val launchIntent = pm.getLaunchIntentForPackage(match.activityInfo.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return ExecutionResult(false, "Unable to launch '${match.loadLabel(pm)}'.")

        context.startActivity(launchIntent)
        return ExecutionResult(true, "Opened ${match.loadLabel(pm)}.")
    }

    private fun executeMaps(destination: String): ExecutionResult {
        val uri = Uri.parse("google.navigation:q=${Uri.encode(destination)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            ExecutionResult(true, "Opened navigation to $destination.")
        } catch (_: Exception) {
            executeWebSearch("directions to $destination")
        }
    }

    private fun executeYouTube(query: String): ExecutionResult {
        val uri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ExecutionResult(true, "Opened YouTube for \"$query\".")
    }

    private fun executeWebSearch(query: String): ExecutionResult {
        val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return ExecutionResult(true, "Searched Google for \"$query\".")
    }

    private fun resolvePhoneNumber(recipient: String): String? {
        if (recipient.isBlank()) return null
        val digitsOnly = recipient.replace(Regex("[^0-9+]"), "")
        if (digitsOnly.length >= 10) return digitsOnly

        val q = recipient.lowercase().trim()

        // 1. Check user's saved Vault Call Aliases first (highest accuracy!)
        vault.getCallAliases().firstOrNull { alias ->
            alias.label.lowercase().contains(q) ||
                q.contains(alias.label.lowercase()) ||
                alias.aliases.split(",").any { a ->
                    val cleaned = a.trim().lowercase()
                    cleaned.isNotBlank() && (q.contains(cleaned) || cleaned.contains(q))
                }
        }?.let { return it.phoneNumber }

        // 2. Query Android Contacts if READ_CONTACTS permission is granted
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPerm) return null

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(0)?.lowercase() ?: continue
                val number = cursor.getString(1) ?: continue
                if (name.contains(q) || q.contains(name)) {
                    return number
                }
            }
        }
        return null
    }
}
