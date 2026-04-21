package com.aifinance.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.webkit.WebView
import android.webkit.WebViewClient

private val Background = Color(0xFFF5F7FA)
private val UserBubble = Color(0xFF4A7DFF)
private val AIBubble = Color(0xFFFFFFFF)
private val InputBackground = Color(0xFFFFFFFF)
private val HintText = Color(0xFF9AA3B2)
private val TextPrimary = Color(0xFF1A1A1A)
private val TextSecondary = Color(0xFF5A6578)

private val quickPrompts = listOf(
    "记录一笔午餐支出",
    "查看本月消费分析",
    "生成月度财务报告",
    "给我一些理财建议",
)

@Composable
fun AiAssistantScreen(
    viewModel: AssistantViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                GlassmorphismNewChatButton(
                    onClick = { viewModel.clearConversation() },
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty()) {
                    EmptyState(
                        onPromptClick = { prompt ->
                            viewModel.onInputChange(prompt)
                            viewModel.sendMessage()
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.messages) { message ->
                            MessageItem(message = message)
                        }
                        if (uiState.isLoading) {
                            item {
                                LoadingIndicator()
                            }
                        }
                    }
                }
            }

            InputArea(
                inputText = uiState.inputText,
                onInputChange = viewModel::onInputChange,
                onSend = viewModel::sendMessage,
                isLoading = uiState.isLoading
            )
        }
    }
}



@Composable
private fun EmptyState(
    onPromptClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = "你好呀！👋",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "我是您的专属财务助手，不管是查账、分析消费习惯，还是制定预算计划，我都能帮您搞定。",
            fontSize = 15.sp,
            color = TextSecondary,
            lineHeight = 24.sp,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            quickPrompts.forEach { prompt ->
                QuickPromptChip(
                    text = prompt,
                    onClick = { onPromptClick(prompt) }
                )
            }
        }
    }
}

@Composable
private fun QuickPromptChip(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}

@Composable
private fun MessageItem(message: AssistantMessage) {
    val isUser = message.role == AssistantRole.USER

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (isUser) 18.dp else 4.dp,
                        bottomEnd = if (isUser) 4.dp else 18.dp
                    )
                )
                .background(if (isUser) UserBubble else AIBubble)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (isUser) {
                Text(
                    text = message.content,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color.White
                )
            } else {
                MarkdownText(content = message.content)
            }
        }
    }
}

@Composable
private fun MarkdownText(content: String) {
    val escapedContent = content
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", "\\n")

    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
            <style>
                body {
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    font-size: 15px;
                    line-height: 1.7;
                    color: #1A1A1A;
                    margin: 0;
                    padding: 0;
                    background: transparent;
                }
                h1, h2, h3, h4, h5, h6 {
                    margin-top: 12px;
                    margin-bottom: 8px;
                    font-weight: 600;
                }
                h1 { font-size: 18px; }
                h2 { font-size: 16px; }
                h3 { font-size: 15px; }
                strong, b { font-weight: 600; }
                code {
                    background-color: #f0f0f0;
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-family: monospace;
                    font-size: 13px;
                }
                pre {
                    background-color: #f5f5f5;
                    padding: 12px;
                    border-radius: 8px;
                    overflow-x: auto;
                    margin: 8px 0;
                }
                table {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 12px 0;
                    font-size: 14px;
                }
                th, td {
                    border: 1px solid #e0e0e0;
                    padding: 8px 10px;
                    text-align: left;
                }
                th {
                    background-color: #f8f8f8;
                    font-weight: 600;
                }
                ul, ol {
                    margin: 8px 0;
                    padding-left: 20px;
                }
                li {
                    margin: 4px 0;
                }
                p {
                    margin: 6px 0;
                }
                blockquote {
                    border-left: 3px solid #4A7DFF;
                    padding-left: 12px;
                    margin: 8px 0;
                    color: #555;
                }
            </style>
        </head>
        <body>
            <div id="content"></div>
            <script>
                document.getElementById('content').innerHTML = marked.parse('$escapedContent');
            </script>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient = WebViewClient()
                settings.apply {
                    javaScriptEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    setSupportZoom(false)
                }
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LoadingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(AIBubble)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    DotAnimation(index = index)
                }
            }
        }
    }
}

@Composable
private fun DotAnimation(index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = index * 150),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot"
    )

    Box(
        modifier = Modifier
            .size((6f * scale).dp)
            .clip(CircleShape)
            .background(UserBubble.copy(alpha = 0.6f))
    )
}

@Composable
private fun InputArea(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InputBackground)
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextField(
            value = inputText,
            onValueChange = onInputChange,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    "有什么想问我的吗",
                    fontSize = 15.sp,
                    color = HintText
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Background,
                unfocusedContainerColor = Background,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(24.dp),
            singleLine = true,
            enabled = !isLoading
        )

        IconButton(
            onClick = onSend,
            enabled = !isLoading && inputText.isNotBlank(),
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    if (inputText.isNotBlank()) UserBubble else Color(0xFFE5E8ED)
                )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "发送",
                    tint = if (inputText.isNotBlank()) Color.White else HintText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun GlassmorphismNewChatButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Color.White.copy(alpha = 0.25f)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "新建对话",
                tint = TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "新建对话",
                fontSize = 13.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
