package com.aifinance.feature.home.state

import com.aifinance.core.model.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AIRecognitionState 单元测试
 *
 * 验证状态密封类、枚举和数据类的正确性。
 */
class AIRecognitionStateTest {

    // ==================== AIRecognitionState 状态分支测试 ====================

    @Test
    fun `test Idle state exists and is object`() {
        val state: AIRecognitionState = AIRecognitionState.Idle
        assertTrue(state is AIRecognitionState.Idle)
    }

    @Test
    fun `test Processing state exists with correct step`() {
        val step = ProcessingStep.UPLOADING
        val state: AIRecognitionState = AIRecognitionState.Processing(step)

        assertTrue(state is AIRecognitionState.Processing)
        assertEquals(step, (state as AIRecognitionState.Processing).step)
    }

    @Test
    fun `test Success state exists with result`() {
        val result = AIRecognitionResult()
        val state: AIRecognitionState = AIRecognitionState.Success(result)

        assertTrue(state is AIRecognitionState.Success)
        assertEquals(result, (state as AIRecognitionState.Success).result)
    }

    @Test
    fun `test Error state exists with message and canRetry`() {
        val message = "网络错误"
        val canRetry = false
        val state: AIRecognitionState = AIRecognitionState.Error(message, canRetry)

        assertTrue(state is AIRecognitionState.Error)
        assertEquals(message, (state as AIRecognitionState.Error).message)
        assertFalse(state.canRetry)
    }

    @Test
    fun `test Error state default canRetry is true`() {
        val state = AIRecognitionState.Error("测试错误")
        assertTrue(state.canRetry)
    }

    // ==================== ProcessingStep 枚举值测试 ====================

    @Test
    fun `test ProcessingStep UPLOADING value exists`() {
        val step = ProcessingStep.UPLOADING
        assertEquals(ProcessingStep.UPLOADING, step)
    }

    @Test
    fun `test ProcessingStep RECOGNIZING value exists`() {
        val step = ProcessingStep.RECOGNIZING
        assertEquals(ProcessingStep.RECOGNIZING, step)
    }

    @Test
    fun `test ProcessingStep PARSING value exists`() {
        val step = ProcessingStep.PARSING
        assertEquals(ProcessingStep.PARSING, step)
    }

    @Test
    fun `test ProcessingStep contains all expected values`() {
        val values = ProcessingStep.values()
        assertEquals(3, values.size)
        assertTrue(values.contains(ProcessingStep.UPLOADING))
        assertTrue(values.contains(ProcessingStep.RECOGNIZING))
        assertTrue(values.contains(ProcessingStep.PARSING))
    }

    // ==================== AIRecognitionResult 默认值测试 ====================

    @Test
    fun `test AIRecognitionResult default amount is zero`() {
        val result = AIRecognitionResult()
        assertEquals(0.0, result.amount, 0.0001)
    }

    @Test
    fun `test AIRecognitionResult default category is empty`() {
        val result = AIRecognitionResult()
        assertEquals("", result.category)
    }

    @Test
    fun `test AIRecognitionResult default merchant is empty`() {
        val result = AIRecognitionResult()
        assertEquals("", result.merchant)
    }

    @Test
    fun `test AIRecognitionResult default date is empty`() {
        val result = AIRecognitionResult()
        assertEquals("", result.date)
    }

    @Test
    fun `test AIRecognitionResult default type is EXPENSE`() {
        val result = AIRecognitionResult()
        assertEquals(TransactionType.EXPENSE, result.type)
    }

    @Test
    fun `test AIRecognitionResult can be created with custom values`() {
        val result = AIRecognitionResult(
            amount = 100.50,
            category = "餐饮",
            merchant = "麦当劳",
            date = "2024-03-29",
            type = TransactionType.INCOME
        )

        assertEquals(100.50, result.amount, 0.0001)
        assertEquals("餐饮", result.category)
        assertEquals("麦当劳", result.merchant)
        assertEquals("2024-03-29", result.date)
        assertEquals(TransactionType.INCOME, result.type)
    }

    // ==================== 状态完备性测试（ exhaustive when ） ====================

    @Test
    fun `test all AIRecognitionState branches are exhaustive`() {
        val states = listOf(
            AIRecognitionState.Idle,
            AIRecognitionState.Processing(ProcessingStep.UPLOADING),
            AIRecognitionState.Success(AIRecognitionResult()),
            AIRecognitionState.Error("错误")
        )

        states.forEach { state ->
            val result = when (state) {
                is AIRecognitionState.Idle -> "idle"
                is AIRecognitionState.Processing -> "processing"
                is AIRecognitionState.Success -> "success"
                is AIRecognitionState.Error -> "error"
            }
            assertNotNull(result)
        }
    }
}
