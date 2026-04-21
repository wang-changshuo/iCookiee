package com.aifinance.feature.home.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat

/**
 * 权限处理工具类
 *
 * 提供相机和存储权限的检查、请求等功能
 * 适配 Android 13+ (API 33) 的权限变化
 */
object PermissionUtils {

    /**
     * 检查相机权限是否已授予
     *
     * @param context 上下文
     * @return 如果已授予返回 true，否则返回 false
     */
    fun hasCameraPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 请求相机权限
     *
     * @param launcher ActivityResultLauncher 用于启动权限请求
     */
    fun requestCameraPermission(
        launcher: ActivityResultLauncher<String>
    ) {
        launcher.launch(Manifest.permission.CAMERA)
    }

    /**
     * 检查是否应该显示权限理由
     *
     * @param activity Activity 实例
     * @return 如果应该显示理由返回 true，否则返回 false
     */
    fun shouldShowRationale(activity: Activity): Boolean {
        return activity.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
    }

    /**
     * 获取相机权限理由文本
     *
     * @return 权限理由说明文本
     */
    fun getCameraPermissionRationale(): String {
        return "需要相机权限来拍摄照片进行 OCR 识别"
    }

    /**
     * 检查存储权限是否已授予
     *
     * 适配 Android 13+ (API 33) 的权限变化：
     * - API < 33: 使用 READ_EXTERNAL_STORAGE
     * - API >= 33: 使用 READ_MEDIA_IMAGES
     *
     * @param context 上下文
     * @return 如果已授予返回 true，否则返回 false
     */
    fun hasStoragePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 获取所需权限名称数组
     *
     * 适配 Android 13+ (API 33) 的权限变化：
     * - API < 33: 返回 CAMERA 和 READ_EXTERNAL_STORAGE
     * - API >= 33: 返回 CAMERA 和 READ_MEDIA_IMAGES
     *
     * @param sdkVersion SDK 版本号，用于测试时模拟不同版本，默认使用当前设备版本
     * @return 权限名称数组
     */
    fun getRequiredPermissions(sdkVersion: Int = Build.VERSION.SDK_INT): Array<String> {
        return if (sdkVersion >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_MEDIA_IMAGES
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }
}
