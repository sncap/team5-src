package com.bbobbo.pet.ui.shop

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bbobbo.pet.data.repo.PetRepository
import com.bbobbo.pet.domain.Currency
import com.bbobbo.pet.domain.Foods
import com.bbobbo.pet.domain.Furnitures
import com.bbobbo.pet.domain.StatEngine
import com.bbobbo.pet.domain.Wardrobe
import com.bbobbo.pet.ui.actions.ScreenHeader
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.BouncyButton
import com.bbobbo.pet.ui.components.CurrencyChip
import com.bbobbo.pet.ui.components.PillTabs
import com.bbobbo.pet.ui.components.SoftCard
import com.bbobbo.pet.ui.theme.Palette
import kotlin.math.abs

/** 상점에 늘어놓을 상품의 공통 형태. */
private data class Offer(
    val id: String,
    val name: String,
    val emoji: String,
    val sub: String,
    val currency: Currency,
    val price: Int,
    val deal: Boolean = false,
)

/**
 * 기획서 S-12 상점. 탭 = 추천 / 먹이 / 의상 / 가구.
 * '추천'은 날짜를 시드로 고정 추첨한 일일 특가 3종이라 자정(게임 기준 오전 5시)마다 바뀐다.
 */
@Composable
fun ShopScreen(vm: PetViewModel, onBack: () -> Unit) {
    val pet by vm.pet.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("추천", "먹이", "의상", "가구")

    val foods = remember {
        Foods.ALL.filter { it.currency != Currency.FREE }
            .map { Offer(it.id, it.name, it.emoji, it.effect.summary(), it.currency, it.price) }
    }
    val wears = remember {
        Wardrobe.ALL.filter { it.currency != Currency.FREE }
            .map { Offer(it.id, it.name, it.slot.emoji, it.perk ?: "Lv.${it.unlockLevel} 부터", it.currency, it.price) }
    }
    val furniture = remember {
        Furnitures.ALL.filter { it.currency != Currency.FREE }
            .map { Offer(it.id, it.name, it.emoji, it.perk ?: "아늑함 +${it.cozy}", it.currency, it.price) }
    }
    val daily = remember { pickDailyDeals(foods + wears + furniture) }

    val offers = when (tab) {
        0 -> daily
        1 -> foods
        2 -> wears
        else -> furniture
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        ScreenHeader("상점", onBack)
        Spacer(Modifier.height(8.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CurrencyChip("🪙", "%,d".format(pet.coin))
            CurrencyChip("💗", "${pet.heart}")
        }

        Spacer(Modifier.height(10.dp))
        PillTabs(tabs, tab, { tab = it }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))

        if (tab == 0) {
            Text(
                "오늘의 특가 · 매일 오전 5시에 바뀌어요",
                style = MaterialTheme.typography.labelSmall,
                color = Palette.SubText,
            )
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp),
        ) {
            items(offers.size, key = { offers[it].id + tab }) { i ->
                val offer = offers[i]
                OfferRow(
                    offer = offer,
                    owned = vm.owns(offer.id) && Foods.ALL.none { it.id == offer.id },
                    affordable = StatEngine.canAfford(pet, offer.currency, offer.price),
                    onBuy = { buy(vm, offer.id) },
                )
            }
        }
    }
}

/** 마스터 테이블의 id 로 어느 구매 경로를 탈지 고른다. */
private fun buy(vm: PetViewModel, id: String) {
    Foods.ALL.firstOrNull { it.id == id }?.let { vm.feed(it); return }
    Wardrobe.byId(id)?.let { vm.buyAndEquip(it); return }
    Furnitures.byId(id)?.let { vm.buyFurniture(it) }
}

@Composable
private fun OfferRow(offer: Offer, owned: Boolean, affordable: Boolean, onBuy: () -> Unit) {
    SoftCard(
        Modifier.fillMaxWidth(),
        color = if (offer.deal) Palette.SubPink else Palette.CardWhite,
        radius = 20,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Palette.CardBeige),
                contentAlignment = Alignment.Center,
            ) { Text(offer.emoji, style = MaterialTheme.typography.titleLarge) }

            Column(
                Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(offer.name, style = MaterialTheme.typography.titleMedium, color = Palette.TextBrown)
                    if (offer.deal) {
                        Text(
                            "  특가",
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.PrimaryDeep,
                        )
                    }
                }
                Text(offer.sub, style = MaterialTheme.typography.labelSmall, color = Palette.SubText)
            }

            BouncyButton(
                onClick = onBuy,
                modifier = Modifier.height(38.dp),
                enabled = !owned && affordable,
                color = if (owned) Palette.CardBeige else Palette.Primary,
            ) {
                Text(
                    when {
                        owned -> "보유중"
                        offer.currency == Currency.HEART -> "💗 ${offer.price}"
                        else -> "🪙 ${offer.price}"
                    },
                    modifier = Modifier.padding(horizontal = 14.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (owned || !affordable) Palette.SubText else Color.White,
                )
            }
        }
    }
}

/** 날짜 문자열을 시드로 3종을 고정 추첨한다. 같은 날에는 몇 번을 열어도 같은 목록이 나온다. */
private fun pickDailyDeals(all: List<Offer>): List<Offer> {
    if (all.isEmpty()) return emptyList()
    val seed = abs(PetRepository.today().hashCode())
    return (0 until minOf(3, all.size))
        .map { all[(seed / (it + 1) + it * 7) % all.size] }
        .distinctBy { it.id }
        .map { it.copy(deal = true) }
}
