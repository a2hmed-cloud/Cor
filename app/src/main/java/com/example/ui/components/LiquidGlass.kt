package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalCreovaStrings
import com.example.domain.model.PricingTier
import com.example.ui.theme.AmberFreemium
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.LocalCreovaGlass
import com.example.ui.theme.LocalReducedMotion
import com.example.ui.theme.RosePaid
import com.example.ui.theme.SkyOpenSource
import com.example.ui.theme.VerifiedGold

@Composable
fun LiquidGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalCreovaGlass.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(glass.canvas)
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glass.ambientViolet, Color.Transparent),
                        center = Offset(size.width * 0.85f, size.height * 0.12f),
                        radius = size.maxDimension * 0.55f
                    ),
                    radius = size.maxDimension * 0.55f,
                    center = Offset(size.width * 0.85f, size.height * 0.12f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glass.ambientCyan, Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.45f),
                        radius = size.maxDimension * 0.5f
                    ),
                    radius = size.maxDimension * 0.5f,
                    center = Offset(size.width * 0.12f, size.height * 0.45f)
                )
            },
        content = content
    )
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    elevated: Boolean = false,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val glass = LocalCreovaGlass.current
    val reducedMotion = LocalReducedMotion.current
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && !reducedMotion && onClick != null) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "glass_press"
    )

    val surfaceBrush = Brush.linearGradient(
        colors = if (elevated) {
            listOf(glass.glassElevated, glass.glassSurface)
        } else {
            listOf(glass.glassSurface, glass.glassSurface.copy(alpha = glass.glassSurface.alpha * 0.92f))
        }
    )
    val borderBrush = Brush.linearGradient(
        colors = listOf(glass.glassBorderStart, glass.glassBorderEnd)
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(brush = surfaceBrush, shape = shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.material3.ripple(),
                        role = Role.Button,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            )
            .padding(contentPadding),
        content = content
    )
}

@Composable
fun LiquidGlassPill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null
) {
    val glass = LocalCreovaGlass.current
    val shape = RoundedCornerShape(999.dp)
    val bgBrush = if (selected) {
        Brush.horizontalGradient(
            colors = listOf(
                glass.violetAccent.copy(alpha = if (glass.isDark) 0.34f else 0.18f),
                glass.cyanAccent.copy(alpha = if (glass.isDark) 0.24f else 0.14f)
            )
        )
    } else {
        Brush.horizontalGradient(listOf(glass.glassSurface, glass.glassSurface))
    }
    val borderColor = if (selected) glass.cyanAccent.copy(alpha = 0.7f) else glass.glassBorderStart

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(shape)
            .background(brush = bgBrush, shape = shape)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (selected) glass.cyanAccent else glass.textSecondary
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) glass.textPrimary else glass.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun PricingBadge(tier: PricingTier, modifier: Modifier = Modifier) {
    val strings = LocalCreovaStrings.current
    val (label, color) = when (tier) {
        PricingTier.FREE -> strings.pricingFree to EmeraldFree
        PricingTier.FREEMIUM -> strings.pricingFreemium to AmberFreemium
        PricingTier.PAID -> strings.pricingPaid to RosePaid
        PricingTier.OPEN_SOURCE -> strings.pricingOpenSource to SkyOpenSource
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = color.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

@Composable
fun VerifiedPill(modifier: Modifier = Modifier) {
    val strings = LocalCreovaStrings.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = VerifiedGold.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, VerifiedGold.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = strings.verifiedBadge,
                tint = VerifiedGold,
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = strings.verifiedBadge,
                style = MaterialTheme.typography.labelSmall,
                color = VerifiedGold
            )
        }
    }
}

@Composable
fun EditorialSectionHeader(
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val glass = LocalCreovaGlass.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = glass.textPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
            }
        }
        if (actionLabel != null && onActionClick != null) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge,
                color = glass.cyanAccent,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onActionClick)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun CreovaTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(glass.glassSurface)
                    .border(1.dp, glass.glassBorderStart, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.back,
                    tint = glass.textPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = glass.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        actions()
    }
}

@Composable
fun LiquidGlassHeroArtwork(modifier: Modifier = Modifier) {
    val glass = LocalCreovaGlass.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glass.violetAccent.copy(alpha = 0.42f), Color.Transparent),
                center = Offset(w * 0.78f, h * 0.35f),
                radius = w * 0.5f
            ),
            center = Offset(w * 0.78f, h * 0.35f),
            radius = w * 0.5f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glass.cyanAccent.copy(alpha = 0.32f), Color.Transparent),
                center = Offset(w * 0.25f, h * 0.72f),
                radius = w * 0.42f
            ),
            center = Offset(w * 0.25f, h * 0.72f),
            radius = w * 0.42f
        )
        // Translucent geometric glass prism ribbons
        val prismPath = Path().apply {
            moveTo(w * 0.64f, h * 0.14f)
            lineTo(w * 0.92f, h * 0.48f)
            lineTo(w * 0.70f, h * 0.86f)
            lineTo(w * 0.48f, h * 0.52f)
            close()
        }
        drawPath(
            path = prismPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    glass.cyanAccent.copy(alpha = 0.28f),
                    glass.violetAccent.copy(alpha = 0.14f)
                )
            )
        )
        drawPath(
            path = prismPath,
            color = Color.White.copy(alpha = 0.28f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun LiquidGlassAiOrb(modifier: Modifier = Modifier) {
    val glass = LocalCreovaGlass.current
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    glass.cyanAccent.copy(alpha = 0.75f),
                    glass.violetAccent.copy(alpha = 0.55f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            ),
            radius = radius,
            center = center
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = radius * 0.68f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx())
        )
    }
}

@Composable
fun EmptyStateGlassCard(
    title: String,
    body: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    ctaLabel: String? = null,
    onCtaClick: (() -> Unit)? = null
) {
    val glass = LocalCreovaGlass.current
    LiquidGlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                glass.violetAccent.copy(alpha = 0.22f),
                                glass.cyanAccent.copy(alpha = 0.22f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = glass.cyanAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = glass.textPrimary
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = glass.textSecondary
            )
            if (ctaLabel != null && onCtaClick != null) {
                Spacer(modifier = Modifier.height(4.dp))
                LiquidGlassPill(
                    text = ctaLabel,
                    selected = true,
                    onClick = onCtaClick
                )
            }
        }
    }
}
