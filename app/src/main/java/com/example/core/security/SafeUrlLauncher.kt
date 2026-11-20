package com.example.core.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle

object SafeUrlLauncher {

    private val BLOCKED_EXTENSIONS = setOf(
        ".apk", ".exe", ".bat", ".cmd", ".msi", ".dex", ".jar", ".sh", ".ps1", ".so"
    )

    fun isSafeHttpsUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.length < 8 || trimmed.length > 500) return false
        val uri = try {
            Uri.parse(trimmed)
        } catch (_: Exception) {
            return false
        }
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        if (host.isBlank() || !host.contains(".")) return false
        val pathLower = uri.path?.lowercase().orEmpty()
        if (BLOCKED_EXTENSIONS.any { pathLower.endsWith(it) }) return false
        return true
    }

    fun openSafeCustomTab(context: Context, rawUrl: String, toolbarColorArgb: Int): Boolean {
        if (!isSafeHttpsUrl(rawUrl)) return false
        val uri = Uri.parse(rawUrl.trim())
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            putExtras(Bundle().apply {
                putBinder("android.support.customtabs.extra.SESSION", null)
                putInt("android.support.customtabs.extra.TOOLBAR_COLOR", toolbarColorArgb)
                putBoolean("android.support.customtabs.extra.TITLE_VISIBILITY", true)
            })
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
