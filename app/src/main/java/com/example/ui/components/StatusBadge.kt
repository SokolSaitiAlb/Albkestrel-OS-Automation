package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.model.ExecutionStatus
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSkipped
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun StatusBadge(
    status: ExecutionStatus,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val (backgroundColor, borderColor, textColor, icon, label) = when (status) {
        ExecutionStatus.SUCCESS -> Quint(
            StatusSuccess.copy(alpha = 0.12f),
            StatusSuccess.copy(alpha = 0.35f),
            StatusSuccess,
            Icons.Default.CheckCircle,
            "Success"
        )
        ExecutionStatus.SKIPPED -> Quint(
            StatusSkipped.copy(alpha = 0.12f),
            StatusSkipped.copy(alpha = 0.25f),
            StatusSkipped,
            Icons.Default.Info,
            "Skipped"
        )
        ExecutionStatus.PERMISSION_REQUIRED -> Quint(
            StatusWarning.copy(alpha = 0.14f),
            StatusWarning.copy(alpha = 0.40f),
            StatusWarning,
            Icons.Default.Warning,
            "Permission Required"
        )
        ExecutionStatus.UNSUPPORTED -> Quint(
            StatusWarning.copy(alpha = 0.14f),
            StatusWarning.copy(alpha = 0.35f),
            StatusWarning,
            Icons.Default.Info,
            "Unsupported"
        )
        ExecutionStatus.FAILED -> Quint(
            StatusError.copy(alpha = 0.14f),
            StatusError.copy(alpha = 0.40f),
            StatusError,
            Icons.Default.Error,
            "Failed"
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .testTag("status_badge_${status.name.lowercase()}"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            if (showLabel) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    color = textColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private data class Quint<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)

