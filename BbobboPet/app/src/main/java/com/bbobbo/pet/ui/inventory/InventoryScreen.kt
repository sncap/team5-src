package com.bbobbo.pet.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.domain.Foods
import com.bbobbo.pet.domain.Furnitures
import com.bbobbo.pet.domain.Wardrobe
import com.bbobbo.pet.ui.actions.ScreenHeader
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.PillTabs
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.theme.Palette

/** 보관함에 한 줄로 표시할 아이템. 카테고리별 마스터 테이블을 공통 형태로 모은다. */
private data class Owned(
    val id: String,
    val name: String,
    val emoji: String,
    val sub: String,
    val count: Int,
    val equipped: Boolean,
)

/** 기획서 S-12 보관함 */
@Composable
fun InventoryScreen(vm: PetViewModel, onOpenWardrobe: () -> Unit, onOpenRoom: () -> Unit, onBack: () -> Unit) {
    val inventory by vm.inventory.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("먹이", "의상", "가구")

    val rows = remember(inventory, tab) {
        inventory.mapNotNull { inv ->
            when (tab) {
                0 -> Foods.ALL.firstOrNull { it.id == inv.itemId }?.let {
                    Owned(it.id, it.name, it.emoji, it.desc, inv.count, false)
                }
                1 -> Wardrobe.byId(inv.itemId)?.let {
                    Owned(it.id, it.name, it.slot.emoji, it.perk ?: it.slot.label, inv.count, inv.equipped)
                }
                else -> Furnitures.byId(inv.itemId)?.let {
                    Owned(it.id, it.name, it.emoji, it.perk ?: "아늑함 +${it.cozy}", inv.count, false)
                }
            }
        }.sortedBy { it.name }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        ScreenHeader("보관함", onBack)
        Spacer(Modifier.height(10.dp))
        PillTabs(tabs, tab, { tab = it }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))

        if (rows.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "아직 가진 ${tabs[tab]}이(가) 없어요.\n상점에서 만나볼까요?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Palette.SubText,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
            ) {
                items(rows, key = { it.id }) { row ->
                    val jump = if (tab == 1) onOpenWardrobe else if (tab == 2) onOpenRoom else null
                    OwnedRow(row, jump)
                }
            }
        }
    }
}

@Composable
private fun OwnedRow(row: Owned, onClick: (() -> Unit)?) {
    SoftCard(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        radius = 20,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.CardBeige),
                contentAlignment = Alignment.Center,
            ) { Text(row.emoji, style = MaterialTheme.typography.titleLarge) }

            Column(
                Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(row.name, style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
                    if (row.equipped) {
                        Box(
                            Modifier
                                .padding(start = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Palette.SubPink)
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text("착용 중", style = MaterialTheme.typography.labelSmall, color = Palette.PrimaryDeep)
                        }
                    }
                }
                Text(row.sub, style = MaterialTheme.typography.labelSmall, color = Palette.SubText)
            }
            Text(
                "×${row.count}",
                style = MaterialTheme.typography.labelLarge,
                color = Palette.PrimaryDeep,
            )
            if (onClick != null) {
                Text("  ›", style = MaterialTheme.typography.titleLarge, color = Palette.SubText)
            }
        }
    }
}
