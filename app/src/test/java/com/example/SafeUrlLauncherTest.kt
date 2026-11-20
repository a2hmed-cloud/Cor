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
    fun `allows verified https urls`() {
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
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://example.com/app/installer.apk"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://example.com/setup.exe"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://example.com/patch.dex"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://example.com/library.jar"))
        assertFalse(SafeUrlLauncher.isSafeHttpsUrl("https://example.com/native.so"))
    }
}
