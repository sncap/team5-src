package com.bbobbo.pet.ui.wardrobe

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
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
import com.bbobbo.pet.data.local.OutfitSetEntity
import com.bbobbo.pet.domain.Currency
import com.bbobbo.pet.domain.OutfitBonuses
import com.bbobbo.pet.domain.PetAnim
import com.bbobbo.pet.domain.WearItem
import com.bbobbo.pet.domain.WearSlot
import com.bbobbo.pet.domain.Wardrobe
import com.bbobbo.pet.ui.actions.ScreenHeader
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.BouncyButton
import com.bbobbo.pet.ui.components.PetCharacter
import com.bbobbo.pet.ui.components.PillTabs
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.components.SpeechBubble
import com.bbobbo.pet.ui.theme.Palette

/** 옷장 (기획서 S-07). 탭에서 고른 아이템은 즉시 미리보기되고, 저장 시 확정된다. */
@Composable
fun WardrobeScreen(vm: PetViewModel, onBack: () -> Unit) {
    val pet by vm.pet.collectAsState()
    val inventory by vm.inventory.collectAsState()
    var tab by remember { mutableIntStateOf(0) }

    val outfitSets by vm.outfitSets.collectAsState()

    val slots = listOf(WearSlot.HAT, WearSlot.CLOTH, WearSlot.ACC)
    val setTabIndex = slots.size
    val slot = slots.getOrElse(tab) { WearSlot.HAT }

    // 미리보기 상태 (저장 전)
    var previewHat by remember { mutableStateOf(vm.equippedId(WearSlot.HAT)) }
    var previewCloth by remember { mutableStateOf(vm.equippedId(WearSlot.CLOTH)) }
    var previewAcc by remember { mutableStateOf(vm.equippedId(WearSlot.ACC)) }

    val currentPreview = when (slot) {
        WearSlot.HAT -> previewHat
        WearSlot.CLOTH -> previewCloth
        WearSlot.ACC -> previewAcc
    }

    // 미저장 이탈 확인 (기획서 S-07)
    val dirty = previewHat != vm.equippedId(WearSlot.HAT) ||
        previewCloth != vm.equippedId(WearSlot.CLOTH) ||
        previewAcc != vm.equippedId(WearSlot.ACC)
    var confirmExit by remember { mutableStateOf(false) }
    var saveSlotTarget by remember { mutableStateOf<Int?>(null) }
    var saveSlotName by remember { mutableStateOf("") }

    val previewBonus = OutfitBonuses.match(previewHat, previewCloth, previewAcc)

    fun commitOutfit() {
        listOf(previewHat, previewCloth, previewAcc).forEach { id ->
            Wardrobe.byId(id)?.let { vm.buyAndEquip(it) }
        }
    }

    BackHandler(enabled = dirty) { confirmExit = true }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        ScreenHeader("옷장", { if (dirty) confirmExit = true else onBack() })
        Spacer(Modifier.height(10.dp))

        PillTabs(
            items = slots.map { it.label } + "세트",
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
            SpeechBubble(
                previewBonus?.let { "${it.name} 완성! ♡" } ?: "귀여운 게 최고야 ♡",
                Modifier.align(Alignment.TopEnd)
            )
        }

        // 세트 보너스 안내 (기획서 T-29)
        previewBonus?.let { bonus ->
            Text(
                "✨ ${bonus.name} · ${bonus.desc}",
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                style = MaterialTheme.typography.labelSmall,
                color = Palette.PrimaryDeep,
                textAlign = TextAlign.Center,
            )
        }

        if (tab == setTabIndex) {
            OutfitSetList(
                sets = outfitSets,
                modifier = Modifier.weight(1f),
                onApply = { set ->
                    vm.applyOutfitSet(set)
                    previewHat = set.hatId; previewCloth = set.clothId; previewAcc = set.accId
                },
                onSave = { slotNo ->
                    saveSlotTarget = slotNo
                    saveSlotName = outfitSets.firstOrNull { it.slot == slotNo }?.setName ?: "코디 $slotNo"
                },
                onDelete = { slotNo -> vm.deleteOutfitSet(slotNo) },
            )
        } else LazyVerticalGrid(
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
            onClick = { commitOutfit(); onBack() },
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 0.dp),
            color = Palette.Primary
        ) {
            Text("내 코디 저장", style = MaterialTheme.typography.titleMedium, color = Color.White)
        }
        Spacer(Modifier.height(16.dp))
    }

    // ---- 미저장 이탈 확인 ----
    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            confirmButton = {
                TextButton(onClick = { confirmExit = false; commitOutfit(); onBack() }) {
                    Text("저장하고 나가기")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmExit = false; onBack() }) { Text("그냥 나가기") }
            },
            title = { Text("저장할까요?") },
            text = { Text("바꾼 코디가 아직 저장되지 않았어요.") },
            containerColor = Palette.CardWhite,
        )
    }

    // ---- 세트 슬롯 저장 ----
    saveSlotTarget?.let { slotNo ->
        AlertDialog(
            onDismissRequest = { saveSlotTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    commitOutfit()
                    vm.saveOutfitSet(slotNo, saveSlotName)
                    saveSlotTarget = null
                }) { Text("저장") }
            },
            dismissButton = { TextButton(onClick = { saveSlotTarget = null }) { Text("취소") } },
            title = { Text("코디 $slotNo 번 슬롯") },
            text = {
                OutlinedTextField(
                    value = saveSlotName,
                    onValueChange = { if (it.length <= 10) saveSlotName = it },
                    singleLine = true,
                    label = { Text("세트 이름 (최대 10자)") },
                )
            },
            containerColor = Palette.CardWhite,
        )
    }
}

/** 코디 세트 슬롯 5개 (기획서 S-07 세트 탭) */
@Composable
private fun OutfitSetList(
    sets: List<OutfitSetEntity>,
    modifier: Modifier = Modifier,
    onApply: (OutfitSetEntity) -> Unit,
    onSave: (Int) -> Unit,
    onDelete: (Int) -> Unit,
) {
    val bySlot = sets.associateBy { it.slot }
    Column(
        modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "현재 입은 코디를 슬롯에 저장해두면 한 번에 갈아입을 수 있어요.",
            style = MaterialTheme.typography.labelSmall,
            color = Palette.SubText,
        )
        (1..OutfitBonuses.SLOT_COUNT).forEach { slotNo ->
            val saved = bySlot[slotNo]
            SoftCard(Modifier.fillMaxWidth(), radius = 20) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            saved?.setName ?: "빈 슬롯 $slotNo",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (saved != null) Palette.TextBrown else Palette.SubText,
                        )
                        Text(
                            saved?.let {
                                listOf(it.hatId, it.clothId, it.accId)
                                    .mapNotNull { id -> Wardrobe.byId(id)?.name }
                                    .joinToString(" · ")
                            } ?: "지금 코디를 여기에 저장해보세요",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.SubText,
                        )
                    }
                    if (saved != null) {
                        BouncyButton(
                            onClick = { onApply(saved) },
                            modifier = Modifier.height(36.dp),
                            color = Palette.Primary,
                        ) {
                            Text(
                                "입기",
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                            )
                        }
                        Text(
                            "  🗑",
                            modifier = Modifier.clickable { onDelete(slotNo) },
                            style = MaterialTheme.typography.titleMedium,
                        )
                    } else {
                        BouncyButton(
                            onClick = { onSave(slotNo) },
                            modifier = Modifier.height(36.dp),
                            color = Palette.CardBeige,
                        ) {
                            Text(
                                "저장",
                                modifier = Modifier.padding(horizontal = 12.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = Palette.TextBrown,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
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
