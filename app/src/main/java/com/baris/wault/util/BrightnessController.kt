package com.baris.wault.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Optical presentation helper that boosts screen brightness to 100% (BRIGHTNESS_OVERRIDE_FULL)
 * and prevents screen dimming while a barcode/QR pass is presented full-screen,
 * automatically restoring original window attributes upon dismissal.
 */
@Composable
fun MaxBrightnessEffect(enabled: Boolean = true) {
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(enabled, context, view) {
        if (!enabled) {
            return@DisposableEffect onDispose { }
        }

        val activityWindow: Window? = context.findActivity()?.window
        val dialogWindow: Window? = (view.parent as? DialogWindowProvider)?.window

        val previousActivityBrightness = activityWindow?.attributes?.screenBrightness
        val previousDialogBrightness = dialogWindow?.attributes?.screenBrightness

        activityWindow?.let { window ->
            val params = window.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            window.attributes = params
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        dialogWindow?.let { window ->
            val params = window.attributes
            params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL
            window.attributes = params
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        onDispose {
            activityWindow?.let { window ->
                val params = window.attributes
                params.screenBrightness =
                    previousActivityBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = params
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            dialogWindow?.let { window ->
                val params = window.attributes
                params.screenBrightness =
                    previousDialogBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                window.attributes = params
                window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
