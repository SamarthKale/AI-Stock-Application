package com.stockpredictor.app.ui.screens.learn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.stockpredictor.app.data.learn.LearnCategory
import com.stockpredictor.app.data.learn.LearnContent
import com.stockpredictor.app.data.learn.LearnItem
import com.stockpredictor.app.data.learn.LearnType
import com.stockpredictor.app.ui.components.ClayAppBar
import com.stockpredictor.app.ui.components.EmptyState
import com.stockpredictor.app.ui.theme.ClayColor
import com.stockpredictor.app.ui.theme.ClayElevation
import com.stockpredictor.app.ui.theme.ClayShapes
import com.stockpredictor.app.ui.theme.ClaySpacing
import com.stockpredictor.app.ui.theme.clayShadow

/**
 * Bundled tutorials and demo. [category] null shows both sections; otherwise only that one (the
 * Profile rows "Tutorials" and "Demo" each open this filtered). Content is the static
 * [LearnContent] list -- it ships inside the APK, so this screen works fully offline.
 */
@Composable
fun LearnScreen(
    category: LearnCategory?,
    onBack: () -> Unit,
    onOpenItem: (LearnItem) -> Unit,
) {
    val sections = LearnCategory.entries
        .filter { category == null || it == category }
        .map { it to LearnContent.byCategory(it) }
        .filter { it.second.isNotEmpty() }

    Column(modifier = Modifier.fillMaxSize().background(ClayColor.Background)) {
        ClayAppBar(
            title = when (category) {
                LearnCategory.TUTORIAL -> "Tutorials"
                LearnCategory.DEMO -> "Demo"
                null -> "Learn"
            },
            onBack = onBack,
        )
        if (sections.isEmpty()) {
            EmptyState(message = "Nothing here yet.", modifier = Modifier.weight(1f))
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(ClaySpacing.Lg),
                verticalArrangement = Arrangement.spacedBy(ClaySpacing.Md),
            ) {
                sections.forEach { (cat, content) ->
                    if (category == null) {
                        item(key = "header_$cat") {
                            Column(modifier = Modifier.padding(top = ClaySpacing.Sm)) {
                                Text(sectionTitle(cat), color = ClayColor.TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(sectionSubtitle(cat), color = ClayColor.TextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    items(content, key = { it.id }) { item -> LearnCard(item, onClick = { onOpenItem(item) }) }
                }
            }
        }
    }
}

private fun sectionTitle(c: LearnCategory) = when (c) {
    LearnCategory.TUTORIAL -> "Tutorials"
    LearnCategory.DEMO -> "Demo"
}

private fun sectionSubtitle(c: LearnCategory) = when (c) {
    LearnCategory.TUTORIAL -> "Crypto trading and how to use crypto"
    LearnCategory.DEMO -> "See the app in action"
}

@Composable
private fun LearnCard(item: LearnItem, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clayShadow(shape = ClayShapes.Large, elevation = ClayElevation.Default)
            .background(Brush.horizontalGradient(listOf(ClayColor.AccentGold, ClayColor.AccentPrimary)), ClayShapes.Large)
            .clickable(onClick = onClick),
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(ClaySpacing.Lg), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(ClayColor.OnDark.copy(alpha = 0.22f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (item.type == LearnType.VIDEO) Icons.Filled.PlayArrow else Icons.Filled.PictureAsPdf,
                    contentDescription = if (item.type == LearnType.VIDEO) "Video" else "PDF document",
                    tint = ClayColor.OnDark,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(ClaySpacing.Lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.title, color = ClayColor.OnDark, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(ClaySpacing.Xs))
                Text(item.subtitle, color = ClayColor.OnDarkMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Spacer(Modifier.height(ClaySpacing.Sm))
                Text(
                    (if (item.type == LearnType.VIDEO) "VIDEO" else "PDF") + " · " + item.durationLabel,
                    color = ClayColor.OnDark,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .background(ClayColor.OnDark.copy(alpha = 0.22f), ClayShapes.Pill)
                        .padding(horizontal = ClaySpacing.Md, vertical = ClaySpacing.Xs),
                )
            }
        }
    }
}
