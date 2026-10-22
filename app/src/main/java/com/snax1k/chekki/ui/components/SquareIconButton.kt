package com.snax1k.chekki.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp

/** Кнопка верхней панели: скруглённый квадрат 48 dp с иконкой акцентного цвета. */
@Composable
fun SquareIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
    }
}
