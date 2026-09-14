package com.bbobbo.pet.ui.collection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.domain.CollectionEntry
import com.bbobbo.pet.domain.CollectionTab
import com.bbobbo.pet.domain.Collections
import com.bbobbo.pet.ui.actions.ScreenHeader
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.PillTabs
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.theme.Palette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 기획서 S-10 도감 */
@Composable
fun CollectionScreen(vm: PetViewModel, onBack: () -> Unit) {
    val unlocked by vm.collection.collectAsState()
    val unlockedMap = remember(unlocked) { unlocked.associateBy { it.entryId } }
    var tab by remember { mutableIntStateOf(0) }
    var detail by remember { mutableStateOf<CollectionEntry?>(null) }

    val tabs = CollectionTab.entries
    val entries = remember(tab) { Collections.byTab(tabs[tab]) }
    val total = Collections.ALL.size
    val got = unlocked.size

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        ScreenHeader("도감", onBack)
        Spacer(Modifier.height(10.dp))

        // ---- 수집률 ----
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("수집률", style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
                    Text(
                        "$got / $total  (${(vm.collectionRatio() * 100).toInt()}%)",
                        style = MaterialTheme.typography.labelLarge, color = Palette.PrimaryDeep
                    )
                }
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDE2D4))
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(vm.collectionRatio().coerceIn(0f, 1f))
                            .height(10.dp)
                            .clip(CircleShape)
                            .background(Palette.Yellow)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    Collections.MILESTONES.joinToString("   ") { (p, label, _) ->
                        (if (p <= (vm.collectionRatio() * 100).toInt()) "✅" else "⬜") + " $p% $label"
                    },
                    style = MaterialTheme.typography.labelSmall, color = Palette.SubText
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        PillTabs(tabs.map { it.label }, tab, { tab = it }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            items(entries, key = { it.id }) { entry ->
                val isUnlocked = unlockedMap.containsKey(entry.id)
                EntryCard(entry, isUnlocked) { if (isUnlocked) detail = entry }
            }
        }
    }

    // ---- 상세 카드 ----
    detail?.let { entry ->
        val at = unlockedMap[entry.id]?.unlockedAt
        AlertDialog(
            onDismissRequest = { detail = null },
            confirmButton = { TextButton(onClick = { detail = null }) { Text("닫기") } },
            title = { Text("${entry.emoji}  ${entry.name}") },
            text = {
                Column {
                    Text(entry.desc)
                    if (at != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "획득 ${DATE_FMT.format(Date(at))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.SubText
                        )
                    }
                }
            },
            containerColor = Palette.CardWhite,
        )
    }
}

@Composable
private fun EntryCard(entry: CollectionEntry, unlocked: Boolean, onClick: () -> Unit) {
    SoftCard(
        Modifier
            .fillMaxWidth()
            .aspectRatio(0.85f)
            .clickable(onClick = onClick),
        color = if (unlocked) Palette.CardWhite else Palette.CardBeige.copy(alpha = 0.5f),
        radius = 18,
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // 미획득은 실루엣 + ??? 처리 (기획서 S-10)
            Text(
                if (unlocked) entry.emoji else "❔",
                style = MaterialTheme.typography.displaySmall,
                color = if (unlocked) Color.Unspecified else Palette.SubText.copy(alpha = 0.4f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (unlocked) entry.name else "???",
                style = MaterialTheme.typography.labelSmall,
                color = if (unlocked) Palette.TextBrown else Palette.SubText,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private val DATE_FMT = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)
