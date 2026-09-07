package com.personal.moneytracker.updater

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

data class UpdateInfo(val versionCode: Int, val versionName: String, val releaseNotes: String, val apkUrl: String)

class AppUpdater @Inject constructor() {
    
    suspend fun checkForUpdate(jsonUrl: String, context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(jsonUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            if (connection.responseCode == 200) {
                val json = connection.inputStream.bufferedReader().readText()
                val obj = JSONObject(json)
                val remoteVersionCode = obj.getInt("versionCode")
                
                val pInfo: PackageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                val localVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pInfo.longVersionCode.toInt()
                } else {
                    @Suppress("DEPRECATION")
                    pInfo.versionCode
                }

                if (remoteVersionCode > localVersionCode) {
                    return@withContext UpdateInfo(
                        versionCode = remoteVersionCode,
                        versionName = obj.getString("versionName"),
                        releaseNotes = obj.getString("releaseNotes"),
                        apkUrl = obj.getString("apkUrl")
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        null
    }

    fun downloadAndInstall(context: Context, url: String) {
        try {
            // Delete old file if exists
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
            if (file.exists()) file.delete()

            val request = DownloadManager.Request(Uri.parse(url)).apply {
                setTitle("Money Tracker Update")
                setDescription("Đang tải xuống bản cập nhật...")
                setMimeType("application/vnd.android.package-archive")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                // Save to app-specific external directory to ensure FileProvider has access
                setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk")
            }
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)
            Toast.makeText(context, "Đang tải bản cập nhật trong nền...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi khi bắt đầu tải: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

class AppUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
            val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId != -1L) {
                try {
                    val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
                    if (file.exists()) {
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val installIntent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(uri, "application/vnd.android.package-archive")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                        }
                        context.startActivity(installIntent)
                    } else {
                        Toast.makeText(context, "Không tìm thấy file APK sau khi tải", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Lỗi khởi động cài đặt: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
