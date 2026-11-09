package com.example.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ExpressiveShapes = Shapes(
    extraSmall = CircleShape,
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

val PillShape = CircleShape
val CardShape = RoundedCornerShape(32.dp)
val SheetShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
val DialogShape = RoundedCornerShape(40.dp)
