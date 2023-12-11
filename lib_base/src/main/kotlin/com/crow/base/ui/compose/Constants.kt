package com.crow.base.ui.compose

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp

val topSmallPaddingValues = PaddingValues(top = MaterialTheme.padding.small)

const val ReadItemAlpha = .38f
const val SecondaryItemAlpha = .78f

class Padding {

    val extraLarge = 32.dp

    val large = 24.dp

    val medium = 16.dp

    val small = 8.dp

    val tiny = 4.dp
}

val MaterialTheme.padding: Padding
    get() = Padding()
