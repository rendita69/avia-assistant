package com.aiva.assistant.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.aiva.assistant.model.AuditLogEntry
import com.aiva.assistant.model.CallAlias
import com.aiva.assistant.model.UpiContact
import com.aiva.assistant.model.VoiceLanguage
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SecureVault(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "aiva_secure_vault",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (_: Exception) {
        context.getSharedPreferences("aiva_fallback_vault", Context.MODE_PRIVATE)
    }

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key", value.trim()).apply()

    var confirmBeforeCall: Boolean
        get() = prefs.getBoolean("confirm_before_call", true)
        set(value) = prefs.edit().putBoolean("confirm_before_call", value).apply()

    var speakConfirmations: Boolean
        get() = prefs.getBoolean("speak_confirmations", true)
        set(value) = prefs.edit().putBoolean("speak_confirmations", value).apply()

    var handsFreeMode: Boolean
        get() = prefs.getBoolean("hands_free_mode", false)
        set(value) = prefs.edit().putBoolean("hands_free_mode", value).apply()

    var selectedLanguage: VoiceLanguage
        get() {
            val saved = prefs.getString("selected_language", VoiceLanguage.AUTO.name)
            return VoiceLanguage.entries.firstOrNull { it.name == saved } ?: VoiceLanguage.AUTO
        }
        set(value) = prefs.edit().putString("selected_language", value.name).apply()

    fun getUpiContacts(): List<UpiContact> {
        val raw = prefs.getString("upi_contacts", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            UpiContact(
                id = obj.optString("id", UUID.randomUUID().toString()),
                name = obj.optString("name"),
                upiId = obj.optString("upiId"),
                aliases = obj.optString("aliases")
            )
        }
    }

    fun saveUpiContact(name: String, upiId: String, aliases: String) {
        val list = getUpiContacts().toMutableList()
        list.add(UpiContact(UUID.randomUUID().toString(), name.trim(), upiId.trim(), aliases.trim()))
        persistUpiContacts(list)
    }

    fun deleteUpiContact(id: String) {
        persistUpiContacts(getUpiContacts().filterNot { it.id == id })
    }

    private fun persistUpiContacts(list: List<UpiContact>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("name", it.name)
                put("upiId", it.upiId)
                put("aliases", it.aliases)
            })
        }
        prefs.edit().putString("upi_contacts", arr.toString()).apply()
    }

    fun getCallAliases(): List<CallAlias> {
        val raw = prefs.getString("call_aliases", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            CallAlias(
                id = obj.optString("id", UUID.randomUUID().toString()),
                label = obj.optString("label"),
                phoneNumber = obj.optString("phoneNumber"),
                aliases = obj.optString("aliases")
            )
        }
    }

    fun saveCallAlias(label: String, phoneNumber: String, aliases: String) {
        val list = getCallAliases().toMutableList()
        list.add(CallAlias(UUID.randomUUID().toString(), label.trim(), phoneNumber.trim(), aliases.trim()))
        persistCallAliases(list)
    }

    fun deleteCallAlias(id: String) {
        persistCallAliases(getCallAliases().filterNot { it.id == id })
    }

    private fun persistCallAliases(list: List<CallAlias>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("id", it.id)
                put("label", it.label)
                put("phoneNumber", it.phoneNumber)
                put("aliases", it.aliases)
            })
        }
        prefs.edit().putString("call_aliases", arr.toString()).apply()
    }

    fun getAuditLogs(): List<AuditLogEntry> {
        val raw = prefs.getString("audit_logs", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return List(arr.length()) { i ->
            val obj = arr.getJSONObject(i)
            AuditLogEntry(
                timestamp = obj.optString("timestamp"),
                spokenText = obj.optString("spokenText"),
                actionTitle = obj.optString("actionTitle"),
                status = obj.optString("status")
            )
        }
    }

    fun addAuditLog(spokenText: String, actionTitle: String, status: String) {
        val time = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date())
        val updated = listOf(AuditLogEntry(time, spokenText, actionTitle, status)) + getAuditLogs().take(49)
        val arr = JSONArray()
        updated.forEach {
            arr.put(JSONObject().apply {
                put("timestamp", it.timestamp)
                put("spokenText", it.spokenText)
                put("actionTitle", it.actionTitle)
                put("status", it.status)
            })
        }
        prefs.edit().putString("audit_logs", arr.toString()).apply()
    }

    fun clearAuditLogs() {
        prefs.edit().putString("audit_logs", "[]").apply()
    }

    fun getNotes(): List<String> {
        val raw = prefs.getString("quick_notes", "[]") ?: "[]"
        val arr = JSONArray(raw)
        return List(arr.length()) { arr.optString(it) }
    }

    fun addNote(note: String) {
        val updated = listOf(note.trim()) + getNotes().take(49)
        val arr = JSONArray()
        updated.forEach { arr.put(it) }
        prefs.edit().putString("quick_notes", arr.toString()).apply()
    }
}
