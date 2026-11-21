package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AuthState
import com.example.ui.components.GlassCyberCard
import com.example.ui.components.NeonPulseDot
import com.example.ui.theme.ElectricGreen
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassSurfaceSolid
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBgDeep
import com.example.ui.theme.OrbitronFontFamily
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.LoginUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authState: AuthState,
    loginUiState: LoginUiState,
    onKeyChanged: (String) -> Unit,
    onActivateClick: () -> Unit,
    onGetKeyClick: () -> Unit,
    onDismissGetKeySheet: () -> Unit,
    onRegenerateKey: (Boolean) -> Unit,
    onApplyGeneratedKey: () -> Unit,
) {
    val isVerifying = authState is AuthState.Verifying
    val errorMsg = (authState as? AuthState.Unauthenticated)?.errorMessage

    val infiniteTransition = rememberInfiniteTransition(label = "emblem_orbit")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "orbit_rotation",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg),
    ) {
        // Subtle cyber banner backdrop at top
        Image(
            painter = painterResource(id = R.drawable.img_cyber_banner_1795279577988),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .alpha(0.38f),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, ObsidianBg),
                    )
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 480.dp),
            ) {
                // Animated Emblem Halo
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(108.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .rotate(orbitAngle)
                            .clip(CircleShape)
                            .border(
                                width = 2.dp,
                                brush = Brush.sweepGradient(
                                    listOf(NeonCrimson, Color.Transparent, ElectricGreen, Color.Transparent)
                                ),
                                shape = CircleShape,
                            ),
                    )
                    Image(
                        painter = painterResource(id = R.drawable.ic_nipex_emblem_1795279576883),
                        contentDescription = "NIPEX LAG V12 Emblem",
                        modifier = Modifier
                            .size(84.dp)
                            .clip(CircleShape)
                            .border(1.dp, GlassBorderSubtle, CircleShape),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.displaySmall,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    NeonPulseDot(color = ElectricGreen, size = 7.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.tagline_cyber),
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricGreen,
                        letterSpacing = 1.4.sp,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                GlassCyberCard(
                    glowAccent = if (errorMsg != null) NeonCrimson else ElectricGreen,
                    isHighlighted = true,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = NeonCrimson,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.login_title),
                                style = MaterialTheme.typography.titleSmall,
                                color = TextPrimary,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonCrimson.copy(alpha = 0.16f))
                                .border(1.dp, NeonCrimson.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "V12 VIP GATE",
                                fontFamily = OrbitronFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                color = NeonCrimson,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(R.string.login_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = loginUiState.keyInput,
                        onValueChange = onKeyChanged,
                        singleLine = true,
                        enabled = !isVerifying,
                        label = {
                            Text(
                                text = "ACTIVATION KEY",
                                fontFamily = OrbitronFontFamily,
                                fontSize = 11.sp,
                            )
                        },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.hint_activation_key),
                                fontFamily = JetBrainsMonoFontFamily,
                                color = TextMuted,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = ElectricGreen,
                            )
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { onActivateClick() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricGreen,
                            unfocusedBorderColor = GlassBorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = ElectricGreen,
                            unfocusedLabelColor = TextMuted,
                            cursorColor = NeonCrimson,
                            focusedContainerColor = ObsidianBgDeep.copy(alpha = 0.7f),
                            unfocusedContainerColor = ObsidianBgDeep.copy(alpha = 0.5f),
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activation_key_input"),
                    )

                    AnimatedVisibility(
                        visible = errorMsg != null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Text(
                            text = errorMsg.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = NeonCrimson,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onActivateClick,
                        enabled = !isVerifying,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCrimson,
                            contentColor = TextPrimary,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("activate_key_button"),
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(
                                color = TextPrimary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.verifying_key),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.btn_activate_key),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = onGetKeyClick,
                        enabled = !isVerifying,
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            ElectricGreen.copy(alpha = 0.75f),
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ElectricGreen,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("get_key_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_get_key),
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // VIP Capabilities Preview Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    VipFeaturePill(label = "FLOATING HUD", accent = ElectricGreen, modifier = Modifier.weight(1f))
                    VipFeaturePill(label = "LIVE FPS", accent = NeonCrimson, modifier = Modifier.weight(1f))
                    VipFeaturePill(label = "RAM CLEANER", accent = ElectricGreen, modifier = Modifier.weight(1f))
                }
            }
        }
    }

    if (loginUiState.isGetKeySheetOpen) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val context = LocalContext.current
        var copied by remember(loginUiState.generatedKeyPreview) { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = onDismissGetKeySheet,
            sheetState = sheetState,
            containerColor = GlassSurfaceSolid,
            contentColor = TextPrimary,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "NIPEX V12 KEY VAULT",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TierChip(
                            label = "VIP PRO",
                            selected = loginUiState.generatedKeyIsPro,
                            accent = NeonCrimson,
                            onClick = { onRegenerateKey(true) },
                        )
                        TierChip(
                            label = "24H TRIAL",
                            selected = !loginUiState.generatedKeyIsPro,
                            accent = ElectricGreen,
                            onClick = { onRegenerateKey(false) },
                        )
                    }
                }

                Text(
                    text = "Instant cryptographic checksum license generated locally. Copy or tap Apply & Unlock to enter the control deck.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianBgDeep)
                        .border(1.dp, ElectricGreen.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = loginUiState.generatedKeyPreview.orEmpty(),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = ElectricGreen,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onRegenerateKey(loginUiState.generatedKeyIsPro) },
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Generate New Key",
                            tint = TextSecondary,
                        )
                    }
                    IconButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(
                                ClipData.newPlainText("NIPEX Key", loginUiState.generatedKeyPreview.orEmpty())
                            )
                            copied = true
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Key",
                            tint = if (copied) ElectricGreen else TextPrimary,
                        )
                    }
                }

                if (copied) {
                    Text(
                        text = "Key copied to clipboard.",
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricGreen,
                    )
                }

                Button(
                    onClick = onApplyGeneratedKey,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricGreen,
                        contentColor = ObsidianBgDeep,
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("apply_generated_key_button"),
                ) {
                    Text(
                        text = "APPLY & UNLOCK NOW",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun VipFeaturePill(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF111521))
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
    ) {
        Text(
            text = label,
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 9.sp,
            color = accent,
            maxLines = 1,
        )
    }
}

@Composable
private fun TierChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) accent.copy(alpha = 0.2f) else ObsidianBgDeep)
            .border(1.dp, if (selected) accent else GlassBorderSubtle, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            fontFamily = OrbitronFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            color = if (selected) accent else TextMuted,
        )
    }
}
