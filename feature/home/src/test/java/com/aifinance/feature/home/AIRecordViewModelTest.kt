package com.aifinance.feature.home

import android.net.Uri
import app.cash.turbine.test
import com.aifinance.core.data.repository.ai.AIRepository
import com.aifinance.core.model.TransactionType
import com.aifinance.feature.home.state.AIRecognitionResult
import com.aifinance.feature.home.state.AIRecognitionState
import com.aifinance.feature.home.state.ProcessingStep
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.io.File

/**
 * AIRecordViewModel 单元测试
 *
 * 使用Turbine测试StateFlow状态变化
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AIRecordViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @MockK
    private lateinit var aiRepository: AIRepository

    private lateinit var viewModel: AIRecordViewModel

    @Before
    fun setup() {
        MockKAnnotations.init(this)
        viewModel = AIRecordViewModel(aiRepository)
    }

    @Test
    fun `initial state is Idle`() = runTest {
        viewModel.uiState.test {
            assertEquals(AIRecognitionState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `processImage emits correct state sequence`() = runTest {
        // Arrange
        val uri = mockk<Uri>()
        val file = File("test.jpg")
        val ocrText = "Total: $50.00\nStore: Starbucks"
        val aiResponse = """{"amount":50.0,"category":"餐饮","merchant":"Starbucks","date":"2024-03-29","type":"EXPENSE","paymentTime":"14:30","paymentMethod":"支付宝","paymentAccount":"花呗","description":"咖啡购买","confidence":{"amount":0.95,"category":0.9,"merchant":0.95,"date":0.95,"type":0.9,"paymentTime":0.85,"paymentMethod":0.9,"paymentAccount":0.8,"description":0.75}}"""

        coEvery { aiRepository.recognizeImage(file) } returns Result.success(ocrText)
        coEvery { aiRepository.sendMessage(any()) } returns Result.success(aiResponse)

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(AIRecognitionState.Idle, awaitItem())

            viewModel.processImage(uri, file)
            advanceUntilIdle()

            assertEquals(AIRecognitionState.Processing(ProcessingStep.UPLOADING), awaitItem())
            assertEquals(AIRecognitionState.Processing(ProcessingStep.RECOGNIZING), awaitItem())
            assertEquals(AIRecognitionState.Processing(ProcessingStep.PARSING), awaitItem())

            val success = awaitItem() as AIRecognitionState.Success
            assertEquals(50.0, success.result.amount, 0.01)
            assertEquals("餐饮", success.result.category)
            assertEquals("Starbucks", success.result.merchant)
            assertEquals(TransactionType.EXPENSE, success.result.type)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `processImage emits error on OCR failure`() = runTest {
        // Arrange
        val uri = mockk<Uri>()
        val file = File("test.jpg")

        coEvery { aiRepository.recognizeImage(file) } returns Result.failure(
            Exception("Network error")
        )

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(AIRecognitionState.Idle, awaitItem())

            viewModel.processImage(uri, file)
            advanceUntilIdle()

            assertEquals(AIRecognitionState.Processing(ProcessingStep.UPLOADING), awaitItem())

            val error = awaitItem() as AIRecognitionState.Error
            assertTrue(error.message.contains("OCR识别失败"))
            assertTrue(error.canRetry)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `processImage emits error on AI parsing failure`() = runTest {
        // Arrange
        val uri = mockk<Uri>()
        val file = File("test.jpg")
        val ocrText = "Total: $50.00"

        coEvery { aiRepository.recognizeImage(file) } returns Result.success(ocrText)
        coEvery { aiRepository.sendMessage(any()) } returns Result.failure(
            Exception("AI service error")
        )

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(AIRecognitionState.Idle, awaitItem())

            viewModel.processImage(uri, file)
            advanceUntilIdle()

            assertEquals(AIRecognitionState.Processing(ProcessingStep.UPLOADING), awaitItem())
            assertEquals(AIRecognitionState.Processing(ProcessingStep.RECOGNIZING), awaitItem())
            assertEquals(AIRecognitionState.Processing(ProcessingStep.PARSING), awaitItem())

            val error = awaitItem() as AIRecognitionState.Error
            assertTrue(error.message.contains("AI解析失败"))
            assertTrue(error.canRetry)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `processImage emits error on invalid JSON`() = runTest {
        // Arrange
        val uri = mockk<Uri>()
        val file = File("test.jpg")
        val ocrText = "Total: $50.00"
        val invalidJson = "not valid json"

        coEvery { aiRepository.recognizeImage(file) } returns Result.success(ocrText)
        coEvery { aiRepository.sendMessage(any()) } returns Result.success(invalidJson)

        // Act & Assert
        viewModel.uiState.test {
            assertEquals(AIRecognitionState.Idle, awaitItem())

            viewModel.processImage(uri, file)
            advanceUntilIdle()

            assertEquals(AIRecognitionState.Processing(ProcessingStep.UPLOADING), awaitItem())
            assertEquals(AIRecognitionState.Processing(ProcessingStep.RECOGNIZING), awaitItem())
            assertEquals(AIRecognitionState.Processing(ProcessingStep.PARSING), awaitItem())

            val error = awaitItem() as AIRecognitionState.Error
            assertTrue(error.message.contains("解析结果失败"))
            assertTrue(error.canRetry)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry restarts process with saved data`() = runTest {
        // Arrange
        val uri = mockk<Uri>()
        val file = File("test.jpg")
        val ocrText = "Total: $50.00"
        val aiResponse = """{"amount":50.0,"category":"餐饮","merchant":"Starbucks","date":"2024-03-29","type":"EXPENSE","paymentTime":"14:30","paymentMethod":"支付宝","paymentAccount":"花呗","description":"咖啡购买","confidence":{"amount":0.95,"category":0.9,"merchant":0.95,"date":0.95,"type":0.9,"paymentTime":0.85,"paymentMethod":0.9,"paymentAccount":0.8,"description":0.75}}"""

        // First call fails, retry succeeds
        var callCount = 0
        coEvery { aiRepository.recognizeImage(file) } coAnswers {
            callCount++
            if (callCount == 1) {
                Result.failure(Exception("First attempt failed"))
            } else {
                Result.success(ocrText)
            }
        }
        coEvery { aiRepository.sendMessage(any()) } returns Result.success(aiResponse)

        // Act & Assert
        viewModel.uiState.test {
            // First attempt
            viewModel.processImage(uri, file)
            awaitItem() // Idle
            advanceUntilIdle()
            awaitItem() // Uploading
            awaitItem() // Error

            // Retry
            viewModel.retry()
            advanceUntilIdle()
            awaitItem() // Uploading
            awaitItem() // Recognizing
            awaitItem() // Parsing

            val success = awaitItem() as AIRecognitionState.Success
            assertEquals(50.0, success.result.amount, 0.01)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reset clears state and saved data`() = runTest {
        // Arrange
        val uri = mockk<Uri>()
        val file = File("test.jpg")
        val ocrText = "Total: $50.00"
        val aiResponse = """{"amount":50.0,"category":"餐饮","merchant":"Starbucks","date":"2024-03-29","type":"EXPENSE","paymentTime":"14:30","paymentMethod":"支付宝","paymentAccount":"花呗","description":"咖啡购买","confidence":{"amount":0.95,"category":0.9,"merchant":0.95,"date":0.95,"type":0.9,"paymentTime":0.85,"paymentMethod":0.9,"paymentAccount":0.8,"description":0.75}}"""

        coEvery { aiRepository.recognizeImage(file) } returns Result.success(ocrText)
        coEvery { aiRepository.sendMessage(any()) } returns Result.success(aiResponse)

        // Act & Assert
        viewModel.uiState.test {
            viewModel.processImage(uri, file)
            awaitItem() // Idle
            advanceUntilIdle()
            awaitItem() // Uploading
            awaitItem() // Recognizing
            awaitItem() // Parsing
            awaitItem() // Success

            // Reset
            viewModel.reset()
            assertEquals(AIRecognitionState.Idle, awaitItem())

            // Retry after reset should not work (no saved data)
            viewModel.retry()
            // Should not emit any new state

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `retry without saved data does nothing`() = runTest {
        viewModel.uiState.test {
            assertEquals(AIRecognitionState.Idle, awaitItem())

            // Try to retry without any previous processImage call
            viewModel.retry()

            // Should not emit any new state
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/**
 * Main Dispatcher Rule for Coroutines Testing
 */
@ExperimentalCoroutinesApi
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
