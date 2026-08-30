package com.tekphreak.darkbook.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// Google's official test banner ad unit ID — replace with your real banner
// ad unit ID from apps.admob.com before release. See SPEC.md. (The App ID in
// src/free/AndroidManifest.xml needs the same swap.)
private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"

/** Banner shown at the bottom of the Entry List. Free flavor only — see the matching no-op in src/pro. */
@Composable
fun AdBanner() {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = {
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = BANNER_AD_UNIT_ID
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
