package com.aifinance.feature.settings

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRecordImage by remember { mutableStateOf(true) }
    var showLocation by remember { mutableStateOf(true) }
    var pushEnabled by remember { mutableStateOf(true) }
    var pushDaily by remember { mutableStateOf(true) }
    var pushBudget by remember { mutableStateOf(false) }
    var pushRecommend by remember { mutableStateOf(true) }
    var pushReview by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFEFF4FD))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "返回",
                modifier = Modifier
                    .size(28.dp)
                    .clickable(onClick = onBack),
                tint = Color(0xFF374151),
            )
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(text = "设置", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.size(28.dp))
        }

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingArrowRow("月统计起始日", "每月1日")
                SettingArrowRow("胖咔回复设置", "")
                SettingSwitchRow("展示记录图片", showRecordImage) { showRecordImage = it }
                SettingSwitchRow("记录时展示位置信息", showLocation) { showLocation = it }
            }
        }

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingSwitchRow("推送服务", pushEnabled) { pushEnabled = it }
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                        SettingCheckRow("每日记账", pushDaily) { pushDaily = !pushDaily }
                        SettingCheckRow("预算提醒", pushBudget) { pushBudget = !pushBudget }
                        SettingCheckRow("功能推荐", pushRecommend) { pushRecommend = !pushRecommend }
                        SettingCheckRow("账单回顾", pushReview) { pushReview = !pushReview }
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingArrowRow("帮助与反馈", "")
                SettingArrowRow("关于App", "")
            }
        }

        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "删除所有历史账单数据", style = MaterialTheme.typography.titleMedium, color = Color(0xFF374151))
                Text(text = "删除", style = MaterialTheme.typography.titleMedium, color = Color(0xFFFF4D4F), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SettingArrowRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF374151))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (value.isNotBlank()) {
                Text(text = value, style = MaterialTheme.typography.titleMedium, color = Color(0xFF4B5563))
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF374151))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingCheckRow(
    title: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color(0xFF374151))
        Box(
            modifier = Modifier
                .size(28.dp)
                .border(1.dp, if (checked) Color(0xFF2E5FE6) else Color(0xFFD1D5DB), CircleShape)
                .background(if (checked) Color(0xFF2E5FE6) else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}
