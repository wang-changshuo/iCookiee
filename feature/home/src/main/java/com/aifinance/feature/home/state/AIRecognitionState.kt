package com.aifinance.feature.home.state

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.aifinance.core.model.TransactionType

/**
 * AI识别状态密封类
 *
 * 管理OCR识别的完整生命周期状态，用于AIRecordViewModel状态管理。
 *
 * @author AI Assistant
 * @since 1.0.0
 */
@Stable
sealed class AIRecognitionState {

    /**
     * 初始/空闲状态
     *
     * 表示AI识别功能处于待命状态，等待用户发起识别请求。
     */
    @Immutable
    data object Idle : AIRecognitionState()

    /**
     * 处理中状态
     *
     * 表示AI识别正在进行中，包含当前处理步骤信息。
     *
     * @property step 当前处理步骤
     */
    @Immutable
    data class Processing(
        val step: ProcessingStep
    ) : AIRecognitionState()

    /**
     * 成功状态
     *
     * 表示AI识别成功完成，包含解析后的交易数据。
     *
     * @property result 识别结果数据
     */
    @Immutable
    data class Success(
        val result: AIRecognitionResult
    ) : AIRecognitionState()

    /**
     * 错误状态
     *
     * 表示AI识别过程中发生错误。
     *
     * @property message 错误信息
     * @property canRetry 是否可以重试
     */
    @Immutable
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : AIRecognitionState()
}

/**
 * 处理步骤枚举
 *
 * 定义AI识别过程中的各个处理阶段。
 */
enum class ProcessingStep {
    /**
     * 正在上传图片
     */
    UPLOADING,

    /**
     * OCR识别中
     */
    RECOGNIZING,

    /**
     * AI解析结果中
     */
    PARSING
}

/**
 * AI识别结果数据类
 *
 * 包含OCR识别和AI解析后的交易数据。
 *
 * @property amount 金额
 * @property category 分类
 * @property merchant 商家名称
 * @property date 日期
 * @property paymentTime 支付时间 (HH:mm格式)
 * @property paymentMethod 支付方式 (如: 支付宝/微信/银行卡/现金)
 * @property paymentAccount 支付账户详情 (如: 湖北农信储蓄卡(7198))
 * @property description 商品/服务描述
 * @property type 交易类型（收入/支出）
 * @property confidence 各字段识别置信度 (0.0-1.0)
 */
@Immutable
data class AIRecognitionResult(
    val amount: Double = 0.0,
    val category: String = "",
    val merchant: String = "",
    val date: String = "",
    val paymentTime: String? = null,
    val paymentMethod: String? = null,
    val paymentAccount: String? = null,
    val description: String? = null,
    val type: TransactionType = TransactionType.EXPENSE,
    val confidence: RecognitionConfidence = RecognitionConfidence()
)

/**
 * 字段识别置信度数据类
 *
 * @property amount 金额置信度
 * @property merchant 商家置信度
 * @property date 日期置信度
 * @property paymentTime 支付时间置信度
 * @property paymentMethod 支付方式置信度
 * @property paymentAccount 支付账户置信度
 * @property description 描述置信度
 * @property type 类型置信度
 * @property category 分类置信度
 */
@Immutable
data class RecognitionConfidence(
    val amount: Float = 1.0f,
    val merchant: Float = 1.0f,
    val date: Float = 1.0f,
    val paymentTime: Float = 1.0f,
    val paymentMethod: Float = 1.0f,
    val paymentAccount: Float = 1.0f,
    val description: Float = 1.0f,
    val type: Float = 1.0f,
    val category: Float = 1.0f
) {
    fun isLowConfidence(): Boolean {
        return amount < 0.7f || merchant < 0.7f || date < 0.7f ||
               paymentTime < 0.7f || paymentMethod < 0.7f || paymentAccount < 0.7f ||
               description < 0.7f || type < 0.7f || category < 0.7f
    }

    fun overallConfidence(): Float {
        return minOf(amount, merchant, date, paymentTime, paymentMethod,
                     paymentAccount, description, type, category)
    }
}
