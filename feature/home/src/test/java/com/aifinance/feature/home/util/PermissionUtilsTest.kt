package com.aifinance.feature.home.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * PermissionUtils 的单元测试
 */
class PermissionUtilsTest {

    private lateinit var mockContext: Context
    private lateinit var mockActivity: Activity

    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockActivity = mockk(relaxed = true)
        mockkStatic(ContextCompat::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(ContextCompat::class)
    }

    @Test
    fun `hasCameraPermission should return true when permission is granted`() {
        // Given: 相机权限已授予
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_GRANTED

        // When: 检查相机权限
        val result = PermissionUtils.hasCameraPermission(mockContext)

        // Then: 应该返回 true
        assertTrue(result)
        verify { ContextCompat.checkSelfPermission(mockContext, Manifest.permission.CAMERA) }
    }

    @Test
    fun `hasCameraPermission should return false when permission is denied`() {
        // Given: 相机权限未授予
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.CAMERA)
        } returns PackageManager.PERMISSION_DENIED

        // When: 检查相机权限
        val result = PermissionUtils.hasCameraPermission(mockContext)

        // Then: 应该返回 false
        assertFalse(result)
        verify { ContextCompat.checkSelfPermission(mockContext, Manifest.permission.CAMERA) }
    }

    @Test
    fun `hasStoragePermission should return true when READ_EXTERNAL_STORAGE is granted on API below 33`() {
        // Given: API < 33 且存储权限已授予
        every { mockContext.applicationInfo } returns mockk {
            every { targetSdkVersion } returns 32
        }
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_EXTERNAL_STORAGE)
        } returns PackageManager.PERMISSION_GRANTED

        // When: 检查存储权限
        val result = PermissionUtils.hasStoragePermission(mockContext)

        // Then: 应该返回 true
        assertTrue(result)
    }

    @Test
    fun `hasStoragePermission should return false when READ_EXTERNAL_STORAGE is denied on API below 33`() {
        // Given: API < 33 且存储权限未授予
        every { mockContext.applicationInfo } returns mockk {
            every { targetSdkVersion } returns 32
        }
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_EXTERNAL_STORAGE)
        } returns PackageManager.PERMISSION_DENIED

        // When: 检查存储权限
        val result = PermissionUtils.hasStoragePermission(mockContext)

        // Then: 应该返回 false
        assertFalse(result)
    }

    @Test
    fun `hasStoragePermission should return true when READ_MEDIA_IMAGES is granted on API 33 and above`() {
        // Given: API >= 33 且媒体图片权限已授予
        every { mockContext.applicationInfo } returns mockk {
            every { targetSdkVersion } returns 34
        }
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_MEDIA_IMAGES)
        } returns PackageManager.PERMISSION_GRANTED

        // When: 检查存储权限
        val result = PermissionUtils.hasStoragePermission(mockContext)

        // Then: 应该返回 true
        assertTrue(result)
    }

    @Test
    fun `hasStoragePermission should return false when READ_MEDIA_IMAGES is denied on API 33 and above`() {
        // Given: API >= 33 且媒体图片权限未授予
        every { mockContext.applicationInfo } returns mockk {
            every { targetSdkVersion } returns 34
        }
        every {
            ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_MEDIA_IMAGES)
        } returns PackageManager.PERMISSION_DENIED

        // When: 检查存储权限
        val result = PermissionUtils.hasStoragePermission(mockContext)

        // Then: 应该返回 false
        assertFalse(result)
    }

    @Test
    fun `getRequiredPermissions should return correct permissions for API below 33`() {
        // Given: API < 33
        val expectedPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        // When: 获取所需权限
        val result = PermissionUtils.getRequiredPermissions(32)

        // Then: 应该返回相机和 READ_EXTERNAL_STORAGE 权限
        assertArrayEquals(expectedPermissions, result)
    }

    @Test
    fun `getRequiredPermissions should return correct permissions for API 33 and above`() {
        // Given: API >= 33
        val expectedPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.READ_MEDIA_IMAGES
        )

        // When: 获取所需权限
        val result = PermissionUtils.getRequiredPermissions(33)

        // Then: 应该返回相机和 READ_MEDIA_IMAGES 权限
        assertArrayEquals(expectedPermissions, result)
    }

    @Test
    fun `shouldShowRationale should return true when shouldShowRequestPermissionRationale returns true`() {
        // Given: 应该显示权限理由
        every {
            mockActivity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        } returns true

        // When: 检查是否应该显示理由
        val result = PermissionUtils.shouldShowRationale(mockActivity)

        // Then: 应该返回 true
        assertTrue(result)
    }

    @Test
    fun `shouldShowRationale should return false when shouldShowRequestPermissionRationale returns false`() {
        // Given: 不应该显示权限理由
        every {
            mockActivity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
        } returns false

        // When: 检查是否应该显示理由
        val result = PermissionUtils.shouldShowRationale(mockActivity)

        // Then: 应该返回 false
        assertFalse(result)
    }

    @Test
    fun `getCameraPermissionRationale should return non-empty string`() {
        // When: 获取权限理由文本
        val result = PermissionUtils.getCameraPermissionRationale()

        // Then: 应该返回非空字符串
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `requestCameraPermission should launch permission request`() {
        // Given: Mock launcher
        val mockLauncher: androidx.activity.result.ActivityResultLauncher<String> = mockk(relaxed = true)

        // When: 请求相机权限
        PermissionUtils.requestCameraPermission(mockLauncher)

        // Then: 应该调用 launcher 的 launch 方法
        verify { mockLauncher.launch(Manifest.permission.CAMERA) }
    }
}
