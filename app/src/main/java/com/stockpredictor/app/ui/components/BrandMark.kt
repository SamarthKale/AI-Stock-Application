package com.stockpredictor.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stockpredictor.app.R

/** The app logo (gold coin with an upward trend line), for auth/onboarding headers. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 72.dp) {
    Image(
        painter = painterResource(R.drawable.ic_brand_mark),
        contentDescription = "AI Crypto Predictor logo",
        modifier = modifier.size(size),
    )
}
