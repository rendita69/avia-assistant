package com.aiva.assistant

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.aiva.assistant.data.SecureVault
import com.aiva.assistant.engine.CommandAnalyzer
import com.aiva.assistant.engine.PhoneActionExecutor
import com.aiva.assistant.engine.VoiceController
import com.aiva.assistant.model.ActionType
import com.aiva.assistant.model.AnalyzedCommand
import com.aiva.assistant.model.SafetyTier
import com.aiva.assistant.ui.MainScreen
import com.aiva.assistant.ui.theme.AivaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var vault: SecureVault
    private lateinit var analyzer: CommandAnalyzer
    private lateinit var executor: PhoneActionExecutor
    private lateinit var voiceController: VoiceController

    private var isListening by mutableStateOf(false)
    private var rmsDb by mutableFloatStateOf(0f)
    private var liveTranscript by mutableStateOf("")
    private var statusMessage by mutableStateOf("")
    private var pendingCommand by mutableStateOf<AnalyzedCommand?>(null)

    private var selectedLanguage by mutableStateOf(com.aiva.assistant.model.VoiceLanguage.AUTO)
    private var handsFreeBoost by mutableStateOf(false)
    private var confirmBeforeCall by mutableStateOf(true)
    private var speakConfirmations by mutableStateOf(true)
    private var geminiApiKey by mutableStateOf("")

    private var upiContacts by mutableStateOf(emptyList<com.aiva.assistant.model.UpiContact>())
    private var callAliases by mutableStateOf(emptyList<com.aiva.assistant.model.CallAlias>())
    private var auditLogs by mutableStateOf(emptyList<com.aiva.assistant.model.AuditLogEntry>())

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val micOk = results[Manifest.permission.RECORD_AUDIO] == true
        statusMessage = if (micOk) {
            "Permissions updated. Tap the mic to speak."
        } else {
            "Microphone permission is required for voice commands."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        vault = SecureVault(this)
        analyzer = CommandAnalyzer(vault)
        executor = PhoneActionExecutor(this, vault)

        refreshVaultState()

        voiceController = VoiceController(
            context = this,
            onListeningChanged = { isListening = it },
            onRmsChanged = { rmsDb = it },
            onPartialResult = { liveTranscript = it },
            onFinalResult = { spoken ->
                liveTranscript = spoken
                handleCommandInput(spoken)
            },
            onError = { err -> statusMessage = err }
        )

        requestAllPermissions()

        setContent {
            AivaTheme {
                MainScreen(
                    isListening = isListening,
                    rmsDb = rmsDb,
                    liveTranscript = liveTranscript,
                    statusMessage = statusMessage,
                    selectedLanguage = selectedLanguage,
                    handsFreeBoost = handsFreeBoost,
                    confirmBeforeCall = confirmBeforeCall,
                    speakConfirmations = speakConfirmations,
                    geminiApiKey = geminiApiKey,
                    pendingCommand = pendingCommand,
                    upiContacts = upiContacts,
                    callAliases = callAliases,
                    auditLogs = auditLogs,
                    onLanguageChange = {
                        selectedLanguage = it
                        vault.selectedLanguage = it
                    },
                    onToggleHandsFree = {
                        handsFreeBoost = it
                        vault.handsFreeMode = it
                    },
                    onToggleConfirmCall = {
                        confirmBeforeCall = it
                        vault.confirmBeforeCall = it
                    },
                    onToggleSpeak = {
                        speakConfirmations = it
                        vault.speakConfirmations = it
                    },
                    onSaveApiKey = { key ->
                        geminiApiKey = key
                        vault.geminiApiKey = key
                        statusMessage = if (key.isBlank()) "Switched to offline analysis mode." else "Gemini API key saved securely."
                    },
                    onMicClick = {
                        if (isListening) {
                            voiceController.stopListening()
                        } else {
                            statusMessage = ""
                            voiceController.startListening(selectedLanguage, handsFreeBoost)
                        }
                    },
                    onSubmitTextCommand = { text ->
                        liveTranscript = text
                        handleCommandInput(text)
                    },
                    onConfirmCommand = { cmd ->
                        runAndLog(cmd)
                    },
                    onCancelCommand = {
                        pendingCommand?.let {
                            vault.addAuditLog(it.rawText, it.title, "Cancelled by user")
                            auditLogs = vault.getAuditLogs()
                        }
                        pendingCommand = null
                        statusMessage = "Action cancelled safely."
                    },
                    onAddUpiContact = { name, upi, aliases ->
                        vault.saveUpiContact(name, upi, aliases)
                        upiContacts = vault.getUpiContacts()
                    },
                    onDeleteUpiContact = { id ->
                        vault.deleteUpiContact(id)
                        upiContacts = vault.getUpiContacts()
                    },
                    onAddCallAlias = { label, phone, aliases ->
                        vault.saveCallAlias(label, phone, aliases)
                        callAliases = vault.getCallAliases()
                    },
                    onDeleteCallAlias = { id ->
                        vault.deleteCallAlias(id)
                        callAliases = vault.getCallAliases()
                    },
                    onClearLogs = {
                        vault.clearAuditLogs()
                        auditLogs = emptyList()
                    },
                    onRequestPermissions = { requestAllPermissions() }
                )
            }
        }
    }

    private fun handleCommandInput(input: String) {
        lifecycleScope.launch {
            statusMessage = "Analyzing command..."
            val analyzed = analyzer.analyze(input)

            when {
                analyzed.safetyTier == SafetyTier.BLOCKED -> {
                    pendingCommand = analyzed
                    vault.addAuditLog(input, analyzed.title, "Blocked")
                    auditLogs = vault.getAuditLogs()
                    statusMessage = "Blocked for safety."
                    if (speakConfirmations) voiceController.speak("Blocked for your phone's security.")
                }
                analyzed.safetyTier == SafetyTier.SAFE -> {
                    pendingCommand = null
                    runAndLog(analyzed)
                }
                analyzed.actionType == ActionType.CALL && !confirmBeforeCall -> {
                    pendingCommand = null
                    runAndLog(analyzed)
                }
                else -> {
                    pendingCommand = analyzed
                    statusMessage = "Please review and tap Confirm."
                    if (speakConfirmations) voiceController.speak("Please verify: ${analyzed.summary}")
                }
            }
        }
    }

    private fun runAndLog(command: AnalyzedCommand) {
        val result = executor.execute(command)
        vault.addAuditLog(command.rawText, command.title, if (result.success) "Executed" else "Failed: ${result.message}")
        auditLogs = vault.getAuditLogs()
        pendingCommand = null
        statusMessage = result.message
        if (speakConfirmations) voiceController.speak(result.message)
    }

    private fun refreshVaultState() {
        selectedLanguage = vault.selectedLanguage
        handsFreeBoost = vault.handsFreeMode
        confirmBeforeCall = vault.confirmBeforeCall
        speakConfirmations = vault.speakConfirmations
        geminiApiKey = vault.geminiApiKey
        upiContacts = vault.getUpiContacts()
        callAliases = vault.getCallAliases()
        auditLogs = vault.getAuditLogs()
    }

    private fun requestAllPermissions() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.CALL_PHONE
            )
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        voiceController.release()
    }
}
