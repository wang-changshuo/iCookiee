package com.aifinance.feature.importer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinance.core.designsystem.theme.BrandPrimary
import com.aifinance.core.designsystem.theme.IcokieTextStyles
import com.aifinance.core.designsystem.theme.OnPrimary
import com.aifinance.core.designsystem.theme.OnSurfacePrimary
import com.aifinance.core.designsystem.theme.OnSurfaceSecondary
import com.aifinance.core.designsystem.theme.SurfacePrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImporterScreen(
    onBack: () -> Unit,
    viewModel: ImporterViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.importBankStatement(context, it) }
    }

    Scaffold(
        containerColor = SurfacePrimary,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "账单导入",
                        style = IcokieTextStyles.titleMedium,
                        color = OnSurfacePrimary,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = OnSurfaceSecondary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfacePrimary,
                    titleContentColor = OnSurfacePrimary
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F2F7))
                .padding(paddingValues)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = "导入类型",
                        style = IcokieTextStyles.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ImportChannelItem(
                            label = "微信",
                            icon = Icons.Default.Wallet,
                            selected = uiState.selectedChannel == ImportChannel.WECHAT,
                            onClick = { viewModel.selectChannel(ImportChannel.WECHAT) },
                            modifier = Modifier.weight(1f),
                        )
                        ImportChannelItem(
                            label = "支付宝",
                            icon = Icons.Default.Description,
                            selected = uiState.selectedChannel == ImportChannel.ALIPAY,
                            onClick = { viewModel.selectChannel(ImportChannel.ALIPAY) },
                            modifier = Modifier.weight(1f),
                        )
                        ImportChannelItem(
                            label = "银行账单",
                            icon = Icons.Default.AccountBalance,
                            selected = uiState.selectedChannel == ImportChannel.BANK,
                            onClick = { viewModel.selectChannel(ImportChannel.BANK) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (uiState.selectedChannel == ImportChannel.BANK) {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "银行账单导入规则",
                            style = IcokieTextStyles.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "1. 支持 Excel/CSV；2. 收入固定分类“职业收入”；3. 支出固定分类“餐饮”；4. 账单日期无具体时间时统一为 1:00；5. 仅导入收入和支出，不处理账户余额。",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Button(
                            onClick = {
                                viewModel.clearMessage()
                                filePickerLauncher.launch(
                                    arrayOf(
                                        "application/vnd.ms-excel",
                                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                        "text/csv",
                                        "application/csv",
                                        "text/comma-separated-values",
                                        "application/octet-stream",
                                        "*/*",
                                    )
                                )
                            },
                            enabled = !uiState.isImporting,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        ) {
                            if (uiState.isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = OnPrimary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(text = "正在导入...", color = OnPrimary)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.UploadFile,
                                    contentDescription = null,
                                    tint = OnPrimary,
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(text = "选择银行账单文件并导入", color = OnPrimary)
                            }
                        }
                        uiState.message?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (it.startsWith("导入成功")) Color(0xFF067647) else Color(0xFFB42318),
                            )
                        }
                    }
                }
            } else {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 42.dp, horizontal = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "${uiState.selectedChannel.label}导入窗口已预留，后续可继续开发。",
                            style = IcokieTextStyles.bodyMedium,
                            color = Color(0xFF6B7280),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ImportChannelItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (selected) Color(0xFFE8F0FF) else Color(0xFFF6F7F9)
    val iconTint = if (selected) BrandPrimary else Color(0xFF6B7280)
    val textColor = if (selected) Color(0xFF1D4ED8) else Color(0xFF4B5563)
    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint)
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = textColor)
    }
}
