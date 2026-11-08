/*
 * SPDX-FileCopyrightText: 2026 The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.system

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.UserHandle
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class UwuSystemUpdateRoutingTest {
    private val packageManager = mock<PackageManager>()
    private val context: Context = spy(ApplicationProvider.getApplicationContext()) {
        on { packageManager } doReturn this@UwuSystemUpdateRoutingTest.packageManager
    }

    @Test
    fun trustedUpdaterIsPreferredButUnavailableOrUntrustedUpdaterUsesNativeRoute() {
        assumeTrue(UserHandle.myUserId() == UserHandle.USER_SYSTEM)
        val updater = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = UPDATER_PACKAGE
                name = UPDATER_ACTIVITY
                enabled = true
                applicationInfo = ApplicationInfo().apply { flags = ApplicationInfo.FLAG_SYSTEM }
            }
        }
        val native = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = "native.update"
                name = "native.update.Activity"
            }
        }
        packageManager.stub {
            on {
                resolveActivity(
                    argThat { component?.packageName == UPDATER_PACKAGE },
                    eq(PackageManager.MATCH_SYSTEM_ONLY),
                )
            } doReturn updater
            on {
                resolveActivity(
                    argThat { action == Settings.ACTION_SYSTEM_UPDATE_SETTINGS },
                    eq(PackageManager.MATCH_SYSTEM_ONLY),
                )
            } doReturn native
            on { checkSignatures("android", UPDATER_PACKAGE) } doReturn PackageManager.SIGNATURE_MATCH
        }
        val repository = SystemUpdateRepository(context)
        assertThat(repository.getSystemUpdateIntent()?.component?.packageName).isEqualTo(UPDATER_PACKAGE)

        updater.activityInfo.enabled = false
        assertThat(repository.getSystemUpdateIntent()?.component?.packageName).isEqualTo("native.update")
        updater.activityInfo.enabled = true
        packageManager.stub {
            on { checkSignatures("android", UPDATER_PACKAGE) } doReturn PackageManager.SIGNATURE_NO_MATCH
        }
        assertThat(repository.getSystemUpdateIntent()?.component?.packageName).isEqualTo("native.update")
        packageManager.stub {
            on {
                resolveActivity(
                    argThat { component?.packageName == UPDATER_PACKAGE },
                    eq(PackageManager.MATCH_SYSTEM_ONLY),
                )
            } doReturn null
        }
        assertThat(repository.getSystemUpdateIntent()?.component?.packageName).isEqualTo("native.update")
    }

    private companion object {
        const val UPDATER_PACKAGE = "org.uwuaosp.updater"
        const val UPDATER_ACTIVITY = "org.uwuaosp.updater.UpdaterActivity"
    }
}
