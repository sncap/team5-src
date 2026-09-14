package com.bbobbo.pet.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 밸런싱 테이블이 서로 어긋나지 않는지 지키는 테스트. */
class GameDataTest {

    @Test
    fun `산책 이벤트 추첨은 가중치 순서를 따른다`() {
        // roll 0 은 첫 이벤트, roll 1 에 가까우면 마지막 이벤트가 나와야 한다.
        assertEquals(Walks.EVENTS.first().id, Walks.pick(0f).id)
        assertEquals(Walks.EVENTS.last().id, Walks.pick(0.999f).id)
    }

    @Test
    fun `산책 이벤트 추첨은 어떤 난수에도 반드시 하나를 돌려준다`() {
        var roll = 0f
        while (roll < 1f) {
            assertNotNull(Walks.pick(roll))
            roll += 0.01f
        }
    }

    @Test
    fun `산책 이벤트의 도감 항목은 전부 도감에 실재한다`() {
        Walks.EVENTS.mapNotNull { it.collectionId }.forEach {
            assertNotNull("도감에 없는 항목: $it", Collections.byId(it))
        }
    }

    @Test
    fun `도감 항목의 id 는 중복되지 않는다`() {
        val ids = Collections.ALL.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `마일스톤은 오름차순이고 100퍼센트까지 있다`() {
        val percents = Collections.MILESTONES.map { it.first }
        assertEquals(percents.sorted(), percents)
        assertEquals(100, percents.last())
    }

    @Test
    fun `세트 보너스가 가리키는 아이템은 전부 옷장에 있다`() {
        OutfitBonuses.ALL.forEach { set ->
            listOf(set.hatId, set.clothId, set.accId).forEach {
                assertNotNull("옷장에 없는 아이템: $it", Wardrobe.byId(it))
            }
        }
    }

    @Test
    fun `방 등급은 아늑함이 오를수록 좋아진다`() {
        assertEquals("C", Furnitures.grade(0).first)
        assertEquals("B", Furnitures.grade(20).first)
        assertEquals("A", Furnitures.grade(40).first)
        assertEquals("S", Furnitures.grade(60).first)
        assertTrue(Furnitures.grade(60).second > Furnitures.grade(20).second)
    }

    @Test
    fun `레벨 칭호는 기획서 구간과 일치한다`() {
        assertEquals("아기 뽀뽀", Balance.title(1))
        assertEquals("쑥쑥 뽀뽀", Balance.title(5))
        assertEquals("행복한 뽀뽀", Balance.title(12))
        assertEquals("사랑둥이 뽀뽀", Balance.title(15))
        assertEquals("반짝반짝 뽀뽀", Balance.title(20))
    }

    @Test
    fun `StatChange 합치기는 항목별로 더해진다`() {
        val sum = StatChange(mood = 15f, exp = 15) + StatChange(mood = 10f, coin = 80)
        assertEquals(25f, sum.mood, 0.01f)
        assertEquals(15, sum.exp)
        assertEquals(80L, sum.coin)
    }

    @Test
    fun `출석 보상은 7일치가 모두 정의돼 있다`() {
        assertEquals(7, Attendance.REWARDS.size)
    }

    @Test
    fun `배치 좌표는 격자에 스냅된다`() {
        val step = 1f / RoomLayout.GRID
        assertEquals(4 * step, RoomLayout.snap(4 * step + step * 0.1f), 0.0001f)
        assertEquals(5 * step, RoomLayout.snap(4 * step + step * 0.9f), 0.0001f)
    }

    @Test
    fun `배치 좌표는 방 밖으로 나가지 않는다`() {
        val step = 1f / RoomLayout.GRID
        assertEquals(step, RoomLayout.snap(-5f), 0.0001f)
        assertEquals(1f - step, RoomLayout.snap(5f), 0.0001f)
    }

    @Test
    fun `벽지와 바닥은 배치 슬롯을 차지하지 않는다`() {
        assertEquals(RoomLayout.MAX_FURNITURE, RoomLayout.limitFor(FurnitureCategory.FURNITURE))
        assertEquals(RoomLayout.MAX_PROP, RoomLayout.limitFor(FurnitureCategory.PROP))
        assertEquals(null, RoomLayout.limitFor(FurnitureCategory.WALL))
        assertEquals(null, RoomLayout.limitFor(FurnitureCategory.FLOOR))
    }
}
