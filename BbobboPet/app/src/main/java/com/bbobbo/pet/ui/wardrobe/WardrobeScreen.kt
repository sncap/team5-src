package com.bbobbo.pet.ui.wardrobe

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.domain.Currency
import com.bbobbo.pet.domain.PetAnim
import com.bbobbo.pet.domain.WearItem
import com.bbobbo.pet.domain.WearSlot
import com.bbobbo.pet.domain.Wardrobe
import com.bbobbo.pet.ui.actions.ScreenHeader
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.BouncyButton
import com.bbobbo.pet.ui.components.PetCharacter
import com.bbobbo.pet.ui.components.PillTabs
import com.bbobbo.pet.ui.components.SpeechBubble
import com.bbobbo.pet.ui.theme.Palette

/** 옷장 (기획서 S-07). 탭에서 고른 아이템은 즉시 미리보기되고, 저장 시 확정된다. */
@Composable
fun WardrobeScreen(vm: PetViewModel, onBack: () -> Unit) {
    val pet by vm.pet.collectAsState()
    val inventory by vm.inventory.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    val slots = listOf(WearSlot.HAT, WearSlot.CLOTH, WearSlot.ACC)
    val slot = slots[tab]

    // 미리보기 상태 (저장 전)
    var previewHat by remember { mutableStateOf(vm.equippedId(WearSlot.HAT)) }
    var previewCloth by remember { mutableStateOf(vm.equippedId(WearSlot.CLOTH)) }
    var previewAcc by remember { mutableStateOf(vm.equippedId(WearSlot.ACC)) }

    val currentPreview = when (slot) {
        WearSlot.HAT -> previewHat
        WearSlot.CLOTH -> previewCloth
        WearSlot.ACC -> previewAcc
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        ScreenHeader("옷장", onBack)
        Spacer(Modifier.height(10.dp))

        PillTabs(
            items = slots.map { it.label },
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            Modifier.fillMaxWidth().aspectRatio(1.05f),
            contentAlignment = Alignment.Center
        ) {
            PetCharacter(
                modifier = Modifier.fillMaxSize(),
                anim = PetAnim.IDLE,
                hatId = previewHat,
                clothId = previewCloth,
                accId = previewAcc,
            )
            SpeechBubble("귀여운 게 최고야 ♡", Modifier.align(Alignment.TopEnd))
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Wardrobe.bySlot(slot)) { item ->
                val owned = inventory.any { it.itemId == item.id && it.count > 0 } || item.price == 0
                val locked = pet.level < item.unlockLevel
                WearCell(
                    item = item,
                    selected = currentPreview == item.id,
                    owned = owned,
                    locked = locked,
                    onClick = {
                        if (locked) return@WearCell
                        when (slot) {
                            WearSlot.HAT -> previewHat = item.id
                            WearSlot.CLOTH -> previewCloth = item.id
                            WearSlot.ACC -> previewAcc = item.id
                        }
                    }
                )
            }
        }

        BouncyButton(
            onClick = {
                listOf(previewHat, previewCloth, previewAcc).forEach { id ->
                    Wardrobe.byId(id)?.let { vm.buyAndEquip(it) }
                }
                onBack()
            },
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 0.dp),
            color = Palette.Primary
        ) {
            Text("내 코디 저장", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WearCell(
    item: WearItem,
    selected: Boolean,
    owned: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.CardWhite)
                .then(
                    if (selected) Modifier.border(2.5.dp, Palette.Primary, RoundedCornerShape(16.dp))
                    else Modifier
                )
                .clickable(onClick = onClick)
                .alpha(if (locked) 0.4f else 1f),
            contentAlignment = Alignment.Center
        ) {
            Text(iconFor(item.id), style = MaterialTheme.typography.headlineMedium)
            if (locked) {
                Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp)) {
                    Text("🔒Lv.${item.unlockLevel}", style = MaterialTheme.typography.labelSmall, color = Palette.TextBrown)
                }
            }
        }
        Text(
            item.name,
            style = MaterialTheme.typography.labelSmall,
            color = Palette.TextBrown,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        if (!owned && !locked && item.price > 0) {
            Text(
                if (item.currency == Currency.HEART) "💗${item.price}" else "🪙${item.price}",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.PrimaryDeep
            )
        }
    }
}

private fun iconFor(id: String): String = when (id) {
    "hat_none", "cloth_none", "acc_none" -> "⭕"
    "hat_bear" -> "🐻"
    "hat_rabbit" -> "🐰"
    "hat_frog" -> "🐸"
    "hat_beret" -> "🎨"
    "hat_party" -> "🎉"
    "hat_crown" -> "👑"
    "acc_glasses" -> "👓"
    "acc_ribbon" -> "🎀"
    "acc_bell" -> "🔔"
    "cloth_scarf" -> "🧣"
    "cloth_apron" -> "🥻"
    "cloth_cape" -> "🌟"
    else -> "✨"
}
