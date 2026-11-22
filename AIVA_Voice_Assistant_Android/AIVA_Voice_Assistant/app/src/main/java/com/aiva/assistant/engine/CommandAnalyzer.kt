package com.aiva.assistant.engine

import com.aiva.assistant.data.SecureVault
import com.aiva.assistant.model.ActionType
import com.aiva.assistant.model.AnalyzedCommand
import com.aiva.assistant.model.SafetyTier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class CommandAnalyzer(private val vault: SecureVault) {

    private val blockedKeywords = listOf(
        "factory reset", "format phone", "wipe data", "delete all", "uninstall",
        "erase phone", "disable security", "change pin", "share otp", "read otp",
        "সব ডিলিট", "ফোন ফৰ্মেট", "আনইনষ্টল",
        "सब डिलीट", "फोन फॉर्मेट", "फैक्ट्री रीसेट", "अनइंस्टॉल"
    )

    private val payKeywords = listOf(
        "pay", "send money", "transfer", "upi", "gpay", "phonepe", "paytm", "rupees", "rs",
        "টকা", "পঠিওৱা", "পেমেণ্ট", "পইচা", "ট্ৰান্সফাৰ", "পঠিয়া", "দিয়া",
        "रुपये", "रुपए", "पैसे", "भेजो", "भेज दो", "पेमेंट", "पे करो", "ट्रांसफर"
    )

    private val callKeywords = listOf(
        "call", "dial", "ring", "phone",
        "ফোন কৰা", "কল কৰা", "ফোন লগোৱা", "কল লগোৱা", "ফোন", "কল",
        "कॉल करो", "कॉल लगाओ", "फोन लगाओ", "फ़ोन लगाओ", "फोन करो", "कॉल", "फोन"
    )

    private val whatsappKeywords = listOf(
        "whatsapp", "whats app", "হোৱাটছএপ", "হোৱাটচএপ", "व्हाट्सएप", "व्हाट्सऐप"
    )

    private val smsKeywords = listOf(
        "sms", "text message", "send message", "মেছেজ", "বাৰ্তা", "मैसेज", "एसएमएस"
    )

    private val alarmKeywords = listOf("alarm", "wake me", "এলাৰ্ম", "अलार्म")
    private val timerKeywords = listOf("timer", "countdown", "টাইমাৰ", "टाइमर")
    private val openAppKeywords = listOf("open", "launch", "start", "খোলা", "খোল", "खोलो", "चलाओ")
    private val mapKeywords = listOf("navigate", "directions", "route to", "google maps", "ৰাস্তা", "মেপ", "रास्ता", "मैप")
    private val youtubeKeywords = listOf("youtube", "play video", "ইউটিউব", "यूट्यूब")
    private val noteKeywords = listOf("note", "remember", "টোকা", "মনত ৰাখা", "नोट", "याद रखना")

    suspend fun analyze(rawInput: String): AnalyzedCommand {
        val cleaned = normalizeDigits(rawInput.trim())
        val lower = cleaned.lowercase()

        // 1. Hard security firewall (cannot be bypassed even by AI)
        if (blockedKeywords.any { lower.contains(it) }) {
            return AnalyzedCommand(
                rawText = rawInput,
                actionType = ActionType.BLOCKED,
                safetyTier = SafetyTier.BLOCKED,
                title = "Blocked High-Risk Command",
                summary = "AIVA blocked this request to protect your phone data and security.",
                warnings = listOf("Destructive system, file-deletion, and OTP actions are permanently disabled.")
            )
        }

        // 2. Try optional Gemini AI if user saved a key, falling back cleanly to offline rules
        val apiKey = vault.geminiApiKey
        if (apiKey.isNotBlank()) {
            val aiResult = analyzeWithGemini(cleaned, apiKey)
            if (aiResult != null) return enforceSecurityPolicy(aiResult)
        }

        // 3. Fast, reliable offline multilingual parser (EN / AS / HI)
        return enforceSecurityPolicy(parseOffline(cleaned))
    }

    private fun parseOffline(text: String): AnalyzedCommand {
        val lower = text.lowercase()

        // UPI Payment
        if (payKeywords.any { lower.contains(it) } && Regex("\\d+").containsMatchIn(text)) {
            val amount = Regex("(\\d+(?:\\.\\d{1,2})?)").find(text)?.groupValues?.get(1) ?: ""
            val recipient = extractRecipientForPayment(text, amount)
            val matchedUpi = findUpiContact(recipient)
            val warnings = mutableListOf<String>()
            val numericAmount = amount.toDoubleOrNull() ?: 0.0
            if (numericAmount >= 5000.0) {
                warnings.add("Large amount (₹$amount) — double-check recipient before opening UPI.")
            }
            if (matchedUpi == null) {
                warnings.add("No saved UPI ID for '${recipient.ifBlank { "this person" }}'. Add them in the Vault tab first.")
            } else {
                warnings.add("UPI app will open pre-filled. Enter your UPI PIN manually to complete payment.")
            }

            return AnalyzedCommand(
                rawText = text,
                actionType = ActionType.UPI_PAY,
                safetyTier = SafetyTier.PAYMENT,
                title = "UPI Payment • ₹$amount",
                summary = if (matchedUpi != null) {
                    "Pay ₹$amount to ${matchedUpi.name} (${matchedUpi.upiId})"
                } else {
                    "Pay ₹$amount to ${recipient.ifBlank { "Unknown Recipient" }} (UPI ID missing)"
                },
                parameters = mapOf(
                    "amount" to amount,
                    "recipient" to (matchedUpi?.name ?: recipient),
                    "upiId" to (matchedUpi?.upiId ?: ""),
                    "note" to "Paid via AIVA"
                ),
                warnings = warnings
            )
        }

        // WhatsApp
        if (whatsappKeywords.any { lower.contains(it) }) {
            val cleaned = stripKeywords(text, whatsappKeywords + listOf("send", "message", "to", "পঠিওৱা", "भेजो", "को"))
            val parts = cleaned.split(Regex("(?i)\\b(saying|that|message|:)\\b"), limit = 2)
            val recipient = parts.getOrNull(0)?.trim().orEmpty()
            val msg = parts.getOrNull(1)?.trim().orEmpty()
            return AnalyzedCommand(
                rawText = text,
                actionType = ActionType.WHATSAPP,
                safetyTier = SafetyTier.CAUTION,
                title = "Send WhatsApp Message",
                summary = "Message ${recipient.ifBlank { "contact" }}${if (msg.isNotBlank()) ": \"$msg\"" else ""}",
                parameters = mapOf("recipient" to recipient, "message" to msg),
                warnings = listOf("Verify recipient and message before opening WhatsApp.")
            )
        }

        // SMS
        if (smsKeywords.any { lower.contains(it) }) {
            val cleaned = stripKeywords(text, smsKeywords + listOf("send", "to", "পঠিওৱা", "भेजो", "को"))
            val parts = cleaned.split(Regex("(?i)\\b(saying|that|:)\\b"), limit = 2)
            val recipient = parts.getOrNull(0)?.trim().orEmpty()
            val msg = parts.getOrNull(1)?.trim().orEmpty()
            return AnalyzedCommand(
                rawText = text,
                actionType = ActionType.SMS,
                safetyTier = SafetyTier.CAUTION,
                title = "Compose SMS",
                summary = "SMS to ${recipient.ifBlank { "contact" }}${if (msg.isNotBlank()) ": \"$msg\"" else ""}",
                parameters = mapOf("recipient" to recipient, "message" to msg),
                warnings = listOf("AIVA pre-fills your SMS app so you can review before sending.")
            )
        }

        // Phone Call
        if (callKeywords.any { lower.contains(it) }) {
            val target = extractCallTarget(text)
            return AnalyzedCommand(
                rawText = text,
                actionType = ActionType.CALL,
                safetyTier = SafetyTier.CAUTION,
                title = "Call ${target.ifBlank { "Contact" }}",
                summary = "Place a phone call to ${target.ifBlank { "selected contact" }}",
                parameters = mapOf("recipient" to target),
                warnings = if (target.isBlank()) listOf("Could not detect who to call. Try saying 'Call Mom'.") else emptyList()
            )
        }

        // Alarm
        if (alarmKeywords.any { lower.contains(it) }) {
            val timeMatch = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm|a\\.m\\.|p\\.m\\.)?", RegexOption.IGNORE_CASE).find(text)
            var hour = timeMatch?.groupValues?.get(1)?.toIntOrNull() ?: 7
            val minute = timeMatch?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val meridiem = timeMatch?.groupValues?.get(3)?.lowercase() ?: ""
            if (meridiem.startsWith("p") && hour < 12) hour += 12
            if (meridiem.startsWith("a") && hour == 12) hour = 0
            val formatted = String.format("%02d:%02d", hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            return AnalyzedCommand(
                rawText = text,
                actionType = ActionType.ALARM,
                safetyTier = SafetyTier.SAFE,
                title = "Set Alarm for $formatted",
                summary = "Create an Android system alarm at $formatted",
                parameters = mapOf("hour" to hour.coerceIn(0, 23).toString(), "minute" to minute.coerceIn(0, 59).toString())
            )
        }

        // Timer
        if (timerKeywords.any { lower.contains(it) }) {
            val value = Regex("(\\d+)").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: 5
            val isSeconds = lower.contains("sec") || lower.contains("ছেকেণ্ড") || lower.contains("सेकंड")
            val seconds = if (isSeconds) value else value * 60
            return AnalyzedCommand(
                rawText = text,
                actionType = ActionType.TIMER,
                safetyTier = SafetyTier.SAFE,
                title = "Start Timer",
                summary = "Start a countdown timer for $value ${if (isSeconds) "seconds" else "minutes"}",
                parameters = mapOf("seconds" to seconds.toString())
            )
        }

        // Google Maps
        if (mapKeywords.any { lower.contains(it) }) {
            val dest = stripKeywords(text, mapKeywords + listOf("to", "for", "show", "দেখুওৱা", "दिखाओ"))
            return AnalyzedCommand(
                rawText = text,
                actionType = ActionType.MAPS,
                safetyTier = SafetyTier.SAFE,
                title = "Navigate in Google Maps",
                summary = "Directions to ${dest.ifBlank { "destination" }}",
                parameters = mapOf("destination" to dest)
            )
        }

        // YouTube
        if (youtubeKeywords.any { lower.contains(it) }) {
            val query = stripKeywords(text, youtubeKeywords + listOf("search", "play", "on", "চলোৱা", "चलाओ"))
            return AnalyzedCommand(
                rawText = text,
                actionType = ActionType.YOUTUBE,
                safetyTier = SafetyTier.SAFE,
                title = "Search YouTube",
                summary = "Play or search \"$query\" on YouTube",
                parameters = mapOf("query" to query.ifBlank { text })
            )
        }

        // Open App
        if (openAppKeywords.any { lower.contains(it) }) {
            val appName = stripKeywords(text, openAppKeywords + listOf("app", "application", "এপ", "ऐप"))
            return AnalyzedCommand(
                rawText = text,
                actionType = ActionType.OPEN_APP,
                safetyTier = SafetyTier.SAFE,
                title = "Open ${appName.ifBlank { "App" }}",
                summary = "Launch installed app matching \"$appName\"",
                parameters = mapOf("appName" to appName)
            )
        }

        // Quick Note
        if (noteKeywords.any { lower.contains(it) }) {
            val noteBody = stripKeywords(text, noteKeywords + listOf("save", "make a", "write", "that", "লিখা", "लिखो"))
            return AnalyzedCommand(
                rawText = text,
                actionType = ActionType.NOTE,
                safetyTier = SafetyTier.SAFE,
                title = "Save Quick Note",
                summary = noteBody.ifBlank { text },
                parameters = mapOf("note" to noteBody.ifBlank { text })
            )
        }

        // Default: Safe Web Search
        return AnalyzedCommand(
            rawText = text,
            actionType = ActionType.WEB_SEARCH,
            safetyTier = SafetyTier.SAFE,
            title = "Web Search",
            summary = "Search Google for \"$text\"",
            parameters = mapOf("query" to text)
        )
    }

    private fun enforceSecurityPolicy(cmd: AnalyzedCommand): AnalyzedCommand {
        return when (cmd.actionType) {
            ActionType.UPI_PAY -> cmd.copy(safetyTier = SafetyTier.PAYMENT)
            ActionType.CALL, ActionType.WHATSAPP, ActionType.SMS -> cmd.copy(safetyTier = SafetyTier.CAUTION)
            ActionType.BLOCKED -> cmd.copy(safetyTier = SafetyTier.BLOCKED)
            else -> cmd
        }
    }

    private suspend fun analyzeWithGemini(text: String, apiKey: String): AnalyzedCommand? = withContext(Dispatchers.IO) {
        try {
            val endpoint = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$apiKey")
            val conn = (endpoint.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 7000
                readTimeout = 9000
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }

            val prompt = """
                You are AIVA, a strict, mistake-free Android voice command parser for English, Assamese, and Hindi.
                Convert the user's spoken command into ONLY a valid JSON object (no markdown):
                {
                  "action": "CALL|UPI_PAY|WHATSAPP|SMS|ALARM|TIMER|OPEN_APP|MAPS|YOUTUBE|WEB_SEARCH|NOTE|BLOCKED",
                  "title": "Short clear title",
                  "summary": "Exact human-readable verification summary",
                  "recipient": "person name if applicable",
                  "amount": "numeric amount only if UPI_PAY",
                  "message": "message body if WHATSAPP or SMS",
                  "appName": "app name if OPEN_APP",
                  "query": "search query, destination, or note text",
                  "hour": "0-23 if ALARM",
                  "minute": "0-59 if ALARM",
                  "seconds": "total seconds if TIMER"
                }
                Command: "$text"
            """.trimIndent()

            val body = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))
                ))
            }

            OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
            if (conn.responseCode !in 200..299) return@withContext null

            val responseText = conn.inputStream.bufferedReader().readText()
            val candidateText = JSONObject(responseText)
                .getJSONArray("candidates").getJSONObject(0)
                .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                .getString("text")
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val parsed = JSONObject(candidateText)
            val action = runCatching { ActionType.valueOf(parsed.optString("action", "WEB_SEARCH")) }
                .getOrDefault(ActionType.WEB_SEARCH)

            val recipient = parsed.optString("recipient")
            val amount = parsed.optString("amount")
            val matchedUpi = if (action == ActionType.UPI_PAY) findUpiContact(recipient) else null
            val warnings = mutableListOf<String>()
            if (action == ActionType.UPI_PAY) {
                if (matchedUpi == null) {
                    warnings.add("UPI ID not saved for '$recipient'. Add them in the Vault tab.")
                } else {
                    warnings.add("UPI app opens pre-filled. Enter your UPI PIN manually.")
                }
            }

            AnalyzedCommand(
                rawText = text,
                actionType = action,
                safetyTier = when (action) {
                    ActionType.UPI_PAY -> SafetyTier.PAYMENT
                    ActionType.CALL, ActionType.WHATSAPP, ActionType.SMS -> SafetyTier.CAUTION
                    ActionType.BLOCKED -> SafetyTier.BLOCKED
                    else -> SafetyTier.SAFE
                },
                title = parsed.optString("title", action.name),
                summary = parsed.optString("summary", text),
                parameters = mapOf(
                    "recipient" to (matchedUpi?.name ?: recipient),
                    "upiId" to (matchedUpi?.upiId ?: ""),
                    "amount" to amount,
                    "message" to parsed.optString("message"),
                    "appName" to parsed.optString("appName"),
                    "query" to parsed.optString("query", text),
                    "destination" to parsed.optString("query", text),
                    "note" to parsed.optString("query", text),
                    "hour" to parsed.optString("hour", "7"),
                    "minute" to parsed.optString("minute", "0"),
                    "seconds" to parsed.optString("seconds", "60")
                ),
                warnings = warnings,
                analyzedByAi = true
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun findUpiContact(recipientQuery: String): com.aiva.assistant.model.UpiContact? {
        if (recipientQuery.isBlank()) return null
        val q = recipientQuery.lowercase().trim()
        return vault.getUpiContacts().firstOrNull { contact ->
            contact.name.lowercase().contains(q) ||
                q.contains(contact.name.lowercase()) ||
                contact.aliases.split(",").any { alias ->
                    val a = alias.trim().lowercase()
                    a.isNotBlank() && (q.contains(a) || a.contains(q))
                }
        }
    }

    private fun extractRecipientForPayment(text: String, amount: String): String {
        val stopWords = payKeywords + listOf(
            "to", "for", "please", "now", "immediately", "ko", "को", "ক", "লৈ", "दें", "करो", "কৰা", amount
        )
        var cleaned = stripKeywords(text, stopWords)
        // Remove Assamese suffix "-ক" (e.g., ৰাহুলক -> ৰাহুল)
        if (cleaned.endsWith("ক") && cleaned.length > 2) {
            cleaned = cleaned.dropLast(1)
        }
        return cleaned.trim()
    }

    private fun extractCallTarget(text: String): String {
        val stopWords = callKeywords + listOf(
            "please", "to", "my", "now", "automatically", "ko", "को", "ক", "কৰা", "लगाओ", "करो"
        )
        var cleaned = stripKeywords(text, stopWords).trim()
        if (cleaned.endsWith("ক") && cleaned.length > 2) {
            cleaned = cleaned.dropLast(1)
        }
        // Normalize common multilingual kinship terms for easier matching
        return when (cleaned.lowercase()) {
            "মা", "মাঁ", "माँ", "मां", "mummy", "ammi", "aai" -> "Mom"
            "দেউতা", "पापा", "पिताजी", "papa", "baba" -> "Dad"
            else -> cleaned
        }
    }

    private fun stripKeywords(text: String, keywords: List<String>): String {
        var result = text
        keywords.sortedByDescending { it.length }.forEach { kw ->
            if (kw.isNotBlank()) {
                result = result.replace(Regex("(?i)(^|\\s)${Regex.escape(kw)}(\\s|$)"), " ")
            }
        }
        return result.replace(Regex("\\s+"), " ").trim()
    }

    private fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '০'..'৯' -> sb.append(('0'.code + (ch.code - '০'.code)).toChar())
                in '०'..'९' -> sb.append(('0'.code + (ch.code - '०'.code)).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
