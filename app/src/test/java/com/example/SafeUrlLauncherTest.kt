package com.example

import com.example.core.security.SafeUrlLauncher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SafeUrlLauncherTest {

    @Test
    fun `allows ordinary public https urls`() {
        assertTrue(SafeUrlLauncher.isSafeHttpsUrl("https://www.figma.com"))
        assertTrue(SafeUrlLauncher.isSafeHttpsUrl("https://fonts.google.com/specimen/Cairo"))
    }

    @Test
    fun `blocks non-https and dangerous schemes`() {
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("http://example.com"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("javascript:alert(1)"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("file:///sdcard/payload.apk"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("intent://scan/#Intent;end"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("not a url"))
    }

    @Test
    fun `blocks executable binary file extensions`() {
        listOf(".apk", ".exe", ".bat", ".cmd", ".msi", ".dex", ".jar", ".sh", ".ps1", ".so")
            .forEach { extension ->
                assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://example.com/download/file$extension"))
            }
    }

    @Test
    fun `blocks local private and deceptive destinations`() {
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://localhost/admin"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://127.0.0.1/private"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://192.168.1.10/router"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://10.0.0.5/internal"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://user:pass@example.com"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://example.com:8443"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://xn--pple-43d.com"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://example.com/path with spaces"))
    }
}
