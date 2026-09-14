package com.bbobbo.pet.ui.room

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.data.local.RoomPlacementEntity
import com.bbobbo.pet.domain.Currency
import com.bbobbo.pet.domain.Furniture
import com.bbobbo.pet.domain.FurnitureCategory
import com.bbobbo.pet.domain.Furnitures
import com.bbobbo.pet.domain.PetAnim
import com.bbobbo.pet.domain.WearSlot
import com.bbobbo.pet.ui.actions.ScreenHeader
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.BouncyButton
import com.bbobbo.pet.ui.components.PetCharacter
import com.bbobbo.pet.ui.components.PillTabs
import com.bbobbo.pet.ui.theme.Palette

/** 방 꾸미기 (기획서 S-08). 꾸미기 모드와 상점 모드를 토글한다. */
@Composable
fun RoomDecorScreen(vm: PetViewModel, onBack: () -> Unit) {
    val pet by vm.pet.collectAsState()
    val placements by vm.placements.collectAsState()
    val inventory by vm.inventory.collectAsState()
    var shopMode by remember { mutableStateOf(false) }
    var category by remember { mutableIntStateOf(0) }
    var selectedPlacement by remember { mutableStateOf<Long?>(null) }

    val categories = FurnitureCategory.entries.toList()
    val cozy = placements.sumOf { Furnitures.byId(it.itemId)?.cozy ?: 0 }
    val (grade, bonus) = Furnitures.grade(cozy)

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        ScreenHeader("방꾸미기", onBack)
        Spacer(Modifier.height(8.dp))

        PillTabs(
            items = listOf("꾸미기", "상점"),
            selected = if (shopMode) 1 else 0,
            onSelect = { shopMode = it == 1 },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

        // ---- 방 미리보기 ----
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(300.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.verticalGradient(listOf(Color(0xFFF6E7D2), Color(0xFFEBD9C0)))
                )
        ) {
            val w = maxWidth
            val h = maxHeight

            placements.forEach { p ->
                val f = Furnitures.byId(p.itemId) ?: return@forEach
                val selected = selectedPlacement == p.id
                Box(
                    Modifier
                        .offset(x = w * p.x - 26.dp, y = h * p.y - 26.dp)
                        .size(52.dp)
                        .then(
                            if (selected)
                                Modifier.border(2.dp, Palette.Primary, RoundedCornerShape(12.dp))
                            else Modifier
                        )
                        .clickable { selectedPlacement = if (selected) null else p.id }
                        .pointerInput(p.id) {
                            detectDragGestures { change, drag ->
                                change.consume()
                                val nx = (p.x + drag.x / size.width.toFloat() / 6f).coerceIn(0.05f, 0.95f)
                                val ny = (p.y + drag.y / size.height.toFloat() / 6f).coerceIn(0.05f, 0.95f)
                                vm.movePlacement(p, nx, ny)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(f.emoji, style = MaterialTheme.typography.displaySmall)
                }
            }

            PetCharacter(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.BottomCenter),
                anim = PetAnim.IDLE,
                hatId = vm.equippedId(WearSlot.HAT),
                clothId = vm.equippedId(WearSlot.CLOTH),
                accId = vm.equippedId(WearSlot.ACC),
            )

            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White.copy(alpha = 0.85f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "방 등급 $grade  ·  아늑함 $cozy  ·  일일 🪙$bonus",
                    style = MaterialTheme.typography.labelSmall,
                    color = Palette.TextBrown
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        PillTabs(
            items = categories.map { it.label },
            selected = category,
            onSelect = { category = it },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(Furnitures.byCategory(categories[category])) { f ->
                val owned = inventory.any { it.itemId == f.id && it.count > 0 } || f.price == 0
                FurnitureCell(
                    f = f,
                    owned = owned,
                    shopMode = shopMode,
                    onClick = {
                        if (shopMode) {
                            if (!owned) vm.buyFurniture(f)
                        } else if (owned) {
                            vm.placeFurniture(f.id, 0.5f, 0.45f)
                        }
                    }
                )
            }
        }

        if (!shopMode) {
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BouncyButton(
                    onClick = { selectedPlacement?.let { vm.removePlacement(it); selectedPlacement = null } },
                    modifier = Modifier.weight(1f).height(50.dp),
                    enabled = selectedPlacement != null,
                    color = Palette.CardBeige
                ) {
                    Text("선택 삭제", style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
                }
                BouncyButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f).height(50.dp),
                    color = Palette.Primary
                ) {
                    Text("배치하기", style = MaterialTheme.typography.titleMedium, color = Color.White)
                }
            }
        } else {
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FurnitureCell(
    f: Furniture,
    owned: Boolean,
    shopMode: Boolean,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(76.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Palette.CardWhite)
                .clickable(onClick = onClick)
                .alpha(if (!owned && !shopMode) 0.4f else 1f),
            contentAlignment = Alignment.Center
        ) {
            Text(f.emoji, style = MaterialTheme.typography.displaySmall)
        }
        Text(
            f.name,
            style = MaterialTheme.typography.labelSmall,
            color = Palette.TextBrown,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            when {
                owned -> "보유중"
                f.currency == Currency.HEART -> "💗 ${f.price}"
                else -> "🪙 %,d".format(f.price)
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (owned) Palette.SubText else Palette.PrimaryDeep
        )
    }
}
