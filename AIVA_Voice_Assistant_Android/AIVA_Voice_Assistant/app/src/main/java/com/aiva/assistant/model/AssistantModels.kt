package com.aiva.assistant.model

enum class VoiceLanguage(val label: String, val localeTag: String) {
    AUTO("Auto (EN/AS/HI)", "en-IN"),
    ENGLISH("English", "en-IN"),
    ASSAMESE("অসমীয়া", "as-IN"),
    HINDI("हिन्दी", "hi-IN")
}

enum class SafetyTier(val badge: String) {
    SAFE("SAFE • INSTANT"),
    CAUTION("VERIFY BEFORE ACTION"),
    PAYMENT("SECURE UPI • PIN REQUIRED"),
    BLOCKED("BLOCKED FOR SAFETY")
}

enum class ActionType {
    CALL,
    UPI_PAY,
    WHATSAPP,
    SMS,
    ALARM,
    TIMER,
    OPEN_APP,
    MAPS,
    YOUTUBE,
    WEB_SEARCH,
    NOTE,
    BLOCKED,
    UNKNOWN
}

data class AnalyzedCommand(
    val rawText: String,
    val actionType: ActionType,
    val safetyTier: SafetyTier,
    val title: String,
    val summary: String,
    val parameters: Map<String, String> = emptyMap(),
    val warnings: List<String> = emptyList(),
    val analyzedByAi: Boolean = false
)

data class UpiContact(
    val id: String,
    val name: String,
    val upiId: String,
    val aliases: String = ""
)

data class CallAlias(
    val id: String,
    val label: String,
    val phoneNumber: String,
    val aliases: String = ""
)

data class AuditLogEntry(
    val timestamp: String,
    val spokenText: String,
    val actionTitle: String,
    val status: String
)
