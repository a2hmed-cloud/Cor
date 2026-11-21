package com.example.core.security

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle

object SafeUrlLauncher {

    private val BLOCKED_EXTENSIONS = setOf(
        ".apk", ".exe", ".bat", ".cmd", ".msi", ".dex", ".jar", ".sh", ".ps1", ".so"
    )

    private fun isPrivateOrLocalHost(host: String): Boolean {
        val value = host.lowercase().removeSuffix(".")
        if (value == "localhost" || value.endsWith(".localhost") || value.endsWith(".local")) return true
        if (value.contains(":")) return true // Reject IP-literal IPv6 destinations.
        val parts = value.split(".")
        if (parts.size == 4 && parts.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }) {
            val a = parts[0].toInt()
            val b = parts[1].toInt()
            return a == 10 || a == 127 || (a == 192 && b == 168) ||
                (a == 172 && b in 16..31) || (a == 169 && b == 254) || a == 0
        }
        return false
    }

    fun isSafeHttpsUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.length < 8 || trimmed.length > 500) return false
        if (trimmed.any { it.isISOControl() || it.isWhitespace() }) return false
        val uri = try {
            Uri.parse(trimmed)
        } catch (_: Exception) {
            return false
        }
        if (!uri.isHierarchical) return false
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        if (!uri.userInfo.isNullOrBlank()) return false
        if (uri.port != -1 && uri.port != 443) return false

        val host = uri.host?.lowercase() ?: return false
        if (host.isBlank() || host.length > 253 || !host.contains(".")) return false
        if (host.startsWith(".") || host.endsWith(".") || host.contains("..")) return false
        if (host.contains("xn--")) return false // Avoid visually deceptive IDN/punycode hosts.
        if (!host.matches(Regex("^[a-z0-9.-]+$"))) return false
        if (host.split(".").any { it.isBlank() || it.length > 63 || it.startsWith("-") || it.endsWith("-") }) return false
        if (isPrivateOrLocalHost(host)) return false

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
