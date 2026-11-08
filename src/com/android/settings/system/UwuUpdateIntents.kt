/*
 * SPDX-FileCopyrightText: 2026 The uwuAOSP Project
 * SPDX-License-Identifier: Apache-2.0
 */
package com.android.settings.system

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.UserHandle

object UwuUpdateIntents {
    fun get(context: Context): Intent? {
        if (UserHandle.myUserId() != UserHandle.USER_SYSTEM) return null
        val intent = Intent().setClassName("org.uwuaosp.updater", "org.uwuaosp.updater.UpdaterActivity")
        val info = context.packageManager.resolveActivity(intent, PackageManager.MATCH_SYSTEM_ONLY)
            ?.activityInfo ?: return null
        return if (info.enabled && (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
            && context.packageManager.checkSignatures("android", info.packageName) == PackageManager.SIGNATURE_MATCH
        ) intent else null
    }
}
