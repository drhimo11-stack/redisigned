package com.shield.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// A slightly larger, more confident corner-radius scale than Material's
// defaults — reads as a calmer, more "app-like" surface than the sharp
// system-settings look the rest of the OS uses, which suits a dashboard
// the user is meant to trust and return to often.
val ShieldShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp)
)
