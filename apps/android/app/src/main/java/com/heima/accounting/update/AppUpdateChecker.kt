package com.heima.accounting.update

import com.heima.accounting.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AppUpdateInfo(
    val version: String,
    val releaseNotes: String,
    val downloadUrl: String,
)

sealed interface UpdateCheckResult {
    data class Available(val update: AppUpdateInfo) : UpdateCheckResult
    data object UpToDate : UpdateCheckResult
    data class Failed(val userMessage: String) : UpdateCheckResult
}

/**
 * A deliberately small, account-free updater: GitHub hosts the public APK and
 * metadata, while Android's browser/download UI remains responsible for the
 * user-approved installation. The app never requests silent-install access.
 */
object AppUpdateChecker {
    private const val LatestReleaseApi =
        "https://api.github.com/repos/olo12119/heima-accounting-releases/releases/latest"

    suspend fun check(): UpdateCheckResult = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(LatestReleaseApi).openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "application/vnd.github+json")
            connection.setRequestProperty("User-Agent", "HeimaAccounting/${BuildConfig.VERSION_NAME}")
            try {
                if (connection.responseCode !in 200..299) {
                    error("GitHub HTTP ${connection.responseCode}")
                }
                val json = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                parseRelease(json)
            } finally {
                connection.disconnect()
            }
        }.fold(
            onSuccess = { update ->
                if (compareVersions(update.version, BuildConfig.VERSION_NAME) > 0) {
                    UpdateCheckResult.Available(update)
                } else {
                    UpdateCheckResult.UpToDate
                }
            },
            onFailure = { UpdateCheckResult.Failed("暂时无法检查更新，请确认网络后重试") },
        )
    }

    internal fun parseRelease(json: String): AppUpdateInfo {
        val root = JSONObject(json)
        val version = root.getString("tag_name").trim().removePrefix("v").removePrefix("V")
        val assets = root.optJSONArray("assets")
        var apkUrl: String? = null
        if (assets != null) {
            for (index in 0 until assets.length()) {
                val asset = assets.getJSONObject(index)
                if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url").takeIf(String::isNotBlank)
                    if (apkUrl != null) break
                }
            }
        }
        return AppUpdateInfo(
            version = version,
            releaseNotes = root.optString("body").trim(),
            downloadUrl = apkUrl ?: root.getString("html_url"),
        )
    }

    internal fun compareVersions(candidate: String, current: String): Int {
        val left = candidate.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val right = current.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val size = maxOf(left.size, right.size)
        for (index in 0 until size) {
            val comparison = (left.getOrElse(index) { 0 }).compareTo(right.getOrElse(index) { 0 })
            if (comparison != 0) return comparison
        }
        return 0
    }
}
