package com.aiva.assistant.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Send
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aiva.assistant.model.AnalyzedCommand
import com.aiva.assistant.model.AuditLogEntry
import com.aiva.assistant.model.CallAlias
import com.aiva.assistant.model.SafetyTier
import com.aiva.assistant.model.UpiContact
import com.aiva.assistant.model.VoiceLanguage
import com.aiva.assistant.ui.theme.AmberCaution
import com.aiva.assistant.ui.theme.ElectricCyan
import com.aiva.assistant.ui.theme.EmeraldSafe
import com.aiva.assistant.ui.theme.GlassBorder
import com.aiva.assistant.ui.theme.GlassCard
import com.aiva.assistant.ui.theme.ObsidianBg
import com.aiva.assistant.ui.theme.RoseAlert
import com.aiva.assistant.ui.theme.TextPrimary
import com.aiva.assistant.ui.theme.TextSecondary
import com.aiva.assistant.ui.theme.VioletPulse

@Composable
fun MainScreen(
    isListening: Boolean,
    rmsDb: Float,
    liveTranscript: String,
    statusMessage: String,
    selectedLanguage: VoiceLanguage,
    handsFreeBoost: Boolean,
    confirmBeforeCall: Boolean,
    speakConfirmations: Boolean,
    geminiApiKey: String,
    pendingCommand: AnalyzedCommand?,
    upiContacts: List<UpiContact>,
    callAliases: List<CallAlias>,
    auditLogs: List<AuditLogEntry>,
    onLanguageChange: (VoiceLanguage) -> Unit,
    onToggleHandsFree: (Boolean) -> Unit,
    onToggleConfirmCall: (Boolean) -> Unit,
    onToggleSpeak: (Boolean) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onMicClick: () -> Unit,
    onSubmitTextCommand: (String) -> Unit,
    onConfirmCommand: (AnalyzedCommand) -> Unit,
    onCancelCommand: () -> Unit,
    onAddUpiContact: (String, String, String) -> Unit,
    onDeleteUpiContact: (String) -> Unit,
    onAddCallAlias: (String, String, String) -> Unit,
    onDeleteCallAlias: (String) -> Unit,
    onClearLogs: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        containerColor = ObsidianBg,
        bottomBar = {
            NavigationBar(containerColor = GlassCard) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Rounded.Mic, contentDescription = "Assistant") },
                    label = { Text("AIVA") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = "Vault") },
                    label = { Text("Vault") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Rounded.History, contentDescription = "Activity") },
                    label = { Text("Log") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        listOf(ObsidianBg, Color(0xFF0F172A), ObsidianBg)
                    )
                )
        ) {
            when (selectedTab) {
                0 -> AssistantHomeTab(
                    isListening = isListening,
                    rmsDb = rmsDb,
                    liveTranscript = liveTranscript,
                    statusMessage = statusMessage,
                    selectedLanguage = selectedLanguage,
                    handsFreeBoost = handsFreeBoost,
                    pendingCommand = pendingCommand,
                    onLanguageChange = onLanguageChange,
                    onToggleHandsFree = onToggleHandsFree,
                    onMicClick = onMicClick,
                    onSubmitTextCommand = onSubmitTextCommand,
                    onConfirmCommand = onConfirmCommand,
                    onCancelCommand = onCancelCommand
                )
                1 -> VaultTab(
                    upiContacts = upiContacts,
                    callAliases = callAliases,
                    onAddUpiContact = onAddUpiContact,
                    onDeleteUpiContact = onDeleteUpiContact,
                    onAddCallAlias = onAddCallAlias,
                    onDeleteCallAlias = onDeleteCallAlias
                )
                2 -> ActivityLogTab(
                    logs = auditLogs,
                    onClearLogs = onClearLogs
                )
                3 -> SettingsTab(
                    confirmBeforeCall = confirmBeforeCall,
                    speakConfirmations = speakConfirmations,
                    handsFreeBoost = handsFreeBoost,
                    geminiApiKey = geminiApiKey,
                    onToggleConfirmCall = onToggleConfirmCall,
                    onToggleSpeak = onToggleSpeak,
                    onToggleHandsFree = onToggleHandsFree,
                    onSaveApiKey = onSaveApiKey,
                    onRequestPermissions = onRequestPermissions
                )
            }
        }
    }
}

@Composable
private fun AssistantHomeTab(
    isListening: Boolean,
    rmsDb: Float,
    liveTranscript: String,
    statusMessage: String,
    selectedLanguage: VoiceLanguage,
    handsFreeBoost: Boolean,
    pendingCommand: AnalyzedCommand?,
    onLanguageChange: (VoiceLanguage) -> Unit,
    onToggleHandsFree: (Boolean) -> Unit,
    onMicClick: () -> Unit,
    onSubmitTextCommand: (String) -> Unit,
    onConfirmCommand: (AnalyzedCommand) -> Unit,
    onCancelCommand: () -> Unit
) {
    var manualInput by remember { mutableStateOf("") }
    val pulseTransition = rememberInfiniteTransition(label = "orb")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening) 1.14f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val dynamicBoost = if (isListening) (rmsDb.coerceIn(0f, 10f) * 0.015f) else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("AIVA", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Safe Voice Assistant • EN / অসমীয়া / हिन्दी", fontSize = 12.sp, color = TextSecondary)
            }
            Surface(
                color = if (handsFreeBoost) ElectricCyan.copy(alpha = 0.2f) else GlassCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, if (handsFreeBoost) ElectricCyan else GlassBorder),
                modifier = Modifier.clickable { onToggleHandsFree(!handsFreeBoost) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = null,
                        tint = if (handsFreeBoost) ElectricCyan else TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (handsFreeBoost) "Far-Field ON" else "Far-Field",
                        fontSize = 12.sp,
                        color = if (handsFreeBoost) ElectricCyan else TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Language Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VoiceLanguage.entries.forEach { lang ->
                FilterChip(
                    selected = selectedLanguage == lang,
                    onClick = { onLanguageChange(lang) },
                    label = { Text(lang.label, fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                        selectedLabelColor = ElectricCyan
                    )
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        // Animated Glowing Voice Orb
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(176.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(164.dp)
                    .scale(pulseScale + dynamicBoost)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isListening) {
                                listOf(ElectricCyan.copy(alpha = 0.42f), VioletPulse.copy(alpha = 0.14f), Color.Transparent)
                            } else {
                                listOf(VioletPulse.copy(alpha = 0.22f), Color.Transparent)
                            }
                        )
                    )
            )
            Surface(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .clickable { onMicClick() },
                shape = CircleShape,
                color = if (isListening) ElectricCyan else GlassCard,
                border = BorderStroke(2.dp, if (isListening) Color.White else ElectricCyan)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isListening) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                        contentDescription = "Speak",
                        tint = if (isListening) ObsidianBg else ElectricCyan,
                        modifier = Modifier.size(46.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = if (isListening) "Listening... Speak clearly" else "Tap the orb and speak in English, Assamese, or Hindi",
            color = if (isListening) ElectricCyan else TextSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        if (liveTranscript.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                border = BorderStroke(1.dp, GlassBorder),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "\"$liveTranscript\"",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        if (statusMessage.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(statusMessage, color = EmeraldSafe, fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        // Analyze & Verify Card
        if (pendingCommand != null) {
            Spacer(Modifier.height(16.dp))
            VerifyCommandCard(
                command = pendingCommand,
                onConfirm = { onConfirmCommand(pendingCommand) },
                onCancel = onCancelCommand
            )
        }

        Spacer(Modifier.height(18.dp))

        // Quick Try Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "Call Mom",
                "Pay 100 rupees to Rahul",
                "মা-ক ফোন কৰা",
                "Set alarm 7 AM",
                "Open WhatsApp"
            ).forEach { sample ->
                Surface(
                    color = GlassCard,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.clickable { onSubmitTextCommand(sample) }
                ) {
                    Text(
                        text = sample,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Type Command Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = manualInput,
                onValueChange = { manualInput = it },
                placeholder = { Text("Or type a command...", color = TextSecondary, fontSize = 13.sp) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = GlassBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (manualInput.isNotBlank()) {
                        onSubmitTextCommand(manualInput)
                        manualInput = ""
                    }
                },
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ElectricCyan)
            ) {
                Icon(Icons.Rounded.Send, contentDescription = "Analyze", tint = ObsidianBg)
            }
        }
    }
}

@Composable
private fun VerifyCommandCard(
    command: AnalyzedCommand,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val tierColor by animateColorAsState(
        targetValue = when (command.safetyTier) {
            SafetyTier.SAFE -> EmeraldSafe
            SafetyTier.CAUTION -> AmberCaution
            SafetyTier.PAYMENT -> VioletPulse
            SafetyTier.BLOCKED -> RoseAlert
        },
        label = "tierColor"
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        border = BorderStroke(1.5.dp, tierColor),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (command.safetyTier == SafetyTier.BLOCKED) Icons.Rounded.Warning else Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = tierColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = command.safetyTier.badge,
                    color = tierColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.weight(1f))
                if (command.analyzedByAi) {
                    Text("Gemini AI", color = ElectricCyan, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(command.title, color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(command.summary, color = TextSecondary, fontSize = 14.sp)

            command.warnings.forEach { warning ->
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = tierColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "• $warning",
                        color = tierColor,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.dp, GlassBorder),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Cancel")
                }

                if (command.safetyTier != SafetyTier.BLOCKED) {
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = tierColor, contentColor = ObsidianBg),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Confirm & Run", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun VaultTab(
    upiContacts: List<UpiContact>,
    callAliases: List<CallAlias>,
    onAddUpiContact: (String, String, String) -> Unit,
    onDeleteUpiContact: (String) -> Unit,
    onAddCallAlias: (String, String, String) -> Unit,
    onDeleteCallAlias: (String) -> Unit
) {
    var upiName by remember { mutableStateOf("") }
    var upiId by remember { mutableStateOf("") }
    var upiAliases by remember { mutableStateOf("") }

    var aliasLabel by remember { mutableStateOf("") }
    var aliasPhone by remember { mutableStateOf("") }
    var aliasExtra by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Secure Contact & UPI Vault", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(
            "Save trusted phone aliases (like Mom / মা / माँ) and UPI IDs here so AIVA never picks the wrong person.",
            fontSize = 13.sp,
            color = TextSecondary
        )

        // Add Call Alias Card
        Card(
            colors = CardDefaults.cardColors(containerColor = GlassCard),
            border = BorderStroke(1.dp, GlassBorder),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Call Alias", fontWeight = FontWeight.Bold, color = ElectricCyan)
                OutlinedTextField(
                    value = aliasLabel,
                    onValueChange = { aliasLabel = it },
                    label = { Text("Name (e.g., Mom)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = aliasPhone,
                    onValueChange = { aliasPhone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = aliasExtra,
                    onValueChange = { aliasExtra = it },
                    label = { Text("Voice Aliases (e.g., মা, माँ, Mummy)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (aliasLabel.isNotBlank() && aliasPhone.isNotBlank()) {
                            onAddCallAlias(aliasLabel, aliasPhone, aliasExtra)
                            aliasLabel = ""
                            aliasPhone = ""
                            aliasExtra = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Call Alias")
                }
            }
        }

        callAliases.forEach { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                border = BorderStroke(1.dp, GlassBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.label, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(item.phoneNumber, color = ElectricCyan, fontSize = 13.sp)
                        if (item.aliases.isNotBlank()) {
                            Text("Aliases: ${item.aliases}", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = { onDeleteCallAlias(item.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = RoseAlert)
                    }
                }
            }
        }

        // Add UPI Contact Card
        Card(
            colors = CardDefaults.cardColors(containerColor = GlassCard),
            border = BorderStroke(1.dp, GlassBorder),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Add Verified UPI Payee", fontWeight = FontWeight.Bold, color = VioletPulse)
                OutlinedTextField(
                    value = upiName,
                    onValueChange = { upiName = it },
                    label = { Text("Payee Name (e.g., Rahul)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = upiId,
                    onValueChange = { upiId = it },
                    label = { Text("UPI ID (e.g., rahul@okaxis)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = upiAliases,
                    onValueChange = { upiAliases = it },
                    label = { Text("Voice Aliases (e.g., ৰাহুল, राहुल)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        if (upiName.isNotBlank() && upiId.contains("@")) {
                            onAddUpiContact(upiName, upiId, upiAliases)
                            upiName = ""
                            upiId = ""
                            upiAliases = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPulse),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save UPI Contact")
                }
            }
        }

        upiContacts.forEach { item ->
            Card(
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                border = BorderStroke(1.dp, GlassBorder),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, color = TextPrimary, fontWeight = FontWeight.Bold)
                        Text(item.upiId, color = VioletPulse, fontSize = 13.sp)
                        if (item.aliases.isNotBlank()) {
                            Text("Aliases: ${item.aliases}", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                    IconButton(onClick = { onDeleteUpiContact(item.id) }) {
                        Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = RoseAlert)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityLogTab(
    logs: List<AuditLogEntry>,
    onClearLogs: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Security Audit Log", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            if (logs.isNotEmpty()) {
                OutlinedButton(onClick = onClearLogs) {
                    Text("Clear")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No commands recorded yet.", color = TextSecondary)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(logs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = GlassCard),
                        border = BorderStroke(1.dp, GlassBorder),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.actionTitle, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(log.timestamp, color = TextSecondary, fontSize = 11.sp)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("Heard: \"${log.spokenText}\"", color = TextSecondary, fontSize = 12.sp)
                            Text("Status: ${log.status}", color = ElectricCyan, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsTab(
    confirmBeforeCall: Boolean,
    speakConfirmations: Boolean,
    handsFreeBoost: Boolean,
    geminiApiKey: String,
    onToggleConfirmCall: (Boolean) -> Unit,
    onToggleSpeak: (Boolean) -> Unit,
    onToggleHandsFree: (Boolean) -> Unit,
    onSaveApiKey: (String) -> Unit,
    onRequestPermissions: () -> Unit
) {
    var keyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Security & AI Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

        SettingToggleRow(
            title = "Confirm Before Calling",
            subtitle = "Pause on Verify Card before dialing any contact",
            checked = confirmBeforeCall,
            onCheckedChange = onToggleConfirmCall
        )
        SettingToggleRow(
            title = "Speak Confirmations Aloud",
            subtitle = "AIVA reads back what it understood",
            checked = speakConfirmations,
            onCheckedChange = onToggleSpeak
        )
        SettingToggleRow(
            title = "Far-Field Listening Boost",
            subtitle = "Extended silence window for speaking from across a room",
            checked = handsFreeBoost,
            onCheckedChange = onToggleHandsFree
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = GlassCard),
            border = BorderStroke(1.dp, GlassBorder),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = EmeraldSafe)
                    Spacer(Modifier.width(8.dp))
                    Text("Optional Gemini AI Brain", fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Text(
                    "Works 100% offline by default. Add a free Gemini API key (stored encrypted on-device) for deeper Assamese, Hindi, and English understanding.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    label = { Text("Gemini API Key (optional)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { onSaveApiKey(keyInput) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Save Encrypted Key")
                }
            }
        }

        OutlinedButton(
            onClick = onRequestPermissions,
            border = BorderStroke(1.dp, ElectricCyan),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Grant / Refresh Phone Permissions (Mic, Contacts, Call)")
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        border = BorderStroke(1.dp, GlassBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(subtitle, color = TextSecondary, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = ElectricCyan)
            )
        }
    }
}
