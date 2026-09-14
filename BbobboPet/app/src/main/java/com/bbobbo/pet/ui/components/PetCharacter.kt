package com.bbobbo.pet.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import com.bbobbo.pet.domain.PetAnim
import com.bbobbo.pet.ui.theme.Palette
import kotlin.math.sin

/**
 * 뽀뽀 캐릭터. 외부 이미지 에셋 없이 Canvas로 직접 그린다.
 * 에셋(Lottie/Rive)이 준비되면 이 컴포저블만 교체하면 된다.
 */
@Composable
fun PetCharacter(
    modifier: Modifier = Modifier,
    anim: PetAnim = PetAnim.IDLE,
    hatId: String = "hat_none",
    clothId: String = "cloth_scarf",
    accId: String = "acc_none",
) {
    val t = rememberInfiniteTransition(label = "pet")

    val breathe by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "breathe"
    )
    val bounce by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(480), RepeatMode.Reverse),
        label = "bounce"
    )
    val sway by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "sway"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val unit = minOf(w, h) / 100f

        // 애니메이션별 변위
        val jumpY = when (anim) {
            PetAnim.JUMP -> -bounce * 14f * unit
            PetAnim.WALK -> -bounce * 5f * unit
            else -> 0f
        }
        val bodyStretch = when (anim) {
            PetAnim.IDLE, PetAnim.EAT -> 1f + breathe * 0.022f
            PetAnim.JUMP -> 1f + (1f - bounce) * 0.06f
            PetAnim.SAD -> 0.96f
            else -> 1f
        }
        val tilt = when (anim) {
            PetAnim.SLEEP -> 8f
            PetAnim.WALK -> (sway - 0.5f) * 8f
            PetAnim.JUMP -> (bounce - 0.5f) * 6f
            else -> (sway - 0.5f) * 2f
        }
        val baseY = when (anim) {
            PetAnim.SLEEP -> 8f * unit
            PetAnim.SAD -> 4f * unit
            else -> 0f
        }

        if (anim == PetAnim.BATH) drawTub(unit, w, h)

        translate(top = jumpY + baseY) {
            rotate(tilt, pivot = Offset(w / 2f, h * 0.82f)) {
                drawBbobbo(unit, w, h, bodyStretch, anim, hatId, clothId, accId)
            }
        }

        when (anim) {
            PetAnim.SLEEP -> drawZzz(unit, w, h, sway)
            PetAnim.EAT -> drawBowl(unit, w, h)
            PetAnim.BATH -> drawBubbles(unit, w, h, sway)
            PetAnim.JUMP, PetAnim.WALK -> drawHearts(unit, w, h, sway)
            else -> Unit
        }
    }
}

private fun DrawScope.drawBbobbo(
    u: Float, w: Float, h: Float, stretch: Float,
    anim: PetAnim, hatId: String, clothId: String, accId: String,
) {
    val cx = w / 2f
    val bodyCy = h * 0.66f
    val bodyRx = 30f * u
    val bodyRy = 27f * u * stretch

    // 그림자
    drawOval(
        color = Color(0x14000000),
        topLeft = Offset(cx - bodyRx * 0.85f, h * 0.88f),
        size = Size(bodyRx * 1.7f, 7f * u),
    )

    // 몸통
    drawOval(
        color = Palette.Fur,
        topLeft = Offset(cx - bodyRx, bodyCy - bodyRy),
        size = Size(bodyRx * 2, bodyRy * 2),
    )

    // 발
    val pawY = bodyCy + bodyRy * 0.68f
    listOf(-1f, 1f).forEach { s ->
        val px = cx + s * 15f * u
        drawOval(
            color = Palette.Fur,
            topLeft = Offset(px - 9f * u, pawY - 6f * u),
            size = Size(18f * u, 12f * u),
        )
        // 육구
        drawOval(
            color = Palette.Muzzle,
            topLeft = Offset(px - 4f * u, pawY - 2.5f * u),
            size = Size(8f * u, 6f * u),
        )
        listOf(-4.2f, 0f, 4.2f).forEach { dx ->
            drawCircle(
                color = Palette.Muzzle,
                radius = 1.7f * u,
                center = Offset(px + dx * u, pawY - 4.6f * u),
            )
        }
    }

    // 팔 (점프/걷기일 때 들어올림)
    val armLift = if (anim == PetAnim.JUMP || anim == PetAnim.WALK) -6f * u else 0f
    listOf(-1f, 1f).forEach { s ->
        drawOval(
            color = Palette.Fur,
            topLeft = Offset(cx + s * 26f * u - 7f * u, bodyCy - 4f * u + armLift),
            size = Size(14f * u, 16f * u),
        )
    }

    val headCy = h * 0.38f
    val headR = 24f * u

    // 귀 + 머리끈
    listOf(-1f to Palette.SkyBlue, 1f to Palette.Primary).forEach { (s, tieColor) ->
        val ex = cx + s * 17f * u
        val ey = headCy - headR * 0.86f
        drawCircle(Palette.Fur, 7.5f * u, Offset(ex, ey - 3f * u))
        drawOval(
            color = tieColor,
            topLeft = Offset(ex - 6.5f * u, ey + 2.2f * u),
            size = Size(13f * u, 3.4f * u),
        )
    }

    // 머리
    drawCircle(Palette.Fur, headR, Offset(cx, headCy))

    // 주둥이
    drawOval(
        color = Palette.Muzzle,
        topLeft = Offset(cx - 8.5f * u, headCy + 1.5f * u),
        size = Size(17f * u, 13f * u),
    )

    // 눈
    val eyeY = headCy - 3f * u
    listOf(-1f, 1f).forEach { s ->
        val ex = cx + s * 10f * u
        when (anim) {
            PetAnim.SAD -> drawLine(
                color = Palette.TextBrown,
                start = Offset(ex - 4f * u, eyeY + 1.5f * u),
                end = Offset(ex + 4f * u, eyeY + 1.5f * u),
                strokeWidth = 2f * u, cap = StrokeCap.Round,
            )
            else -> {
                // ˘ ˘ 형태의 감은 눈
                val path = Path().apply {
                    moveTo(ex - 4.5f * u, eyeY + 2f * u)
                    quadraticBezierTo(ex, eyeY - 4f * u, ex + 4.5f * u, eyeY + 2f * u)
                }
                drawPath(path, Palette.TextBrown, style = Stroke(2.4f * u, cap = StrokeCap.Round))
            }
        }
    }

    // 볼터치
    listOf(-1f, 1f).forEach { s ->
        drawOval(
            color = Palette.Blush.copy(alpha = 0.55f),
            topLeft = Offset(cx + s * 17f * u - 4.5f * u, headCy + 2f * u),
            size = Size(9f * u, 6f * u),
        )
    }

    drawCloth(u, cx, headCy, headR, clothId)
    drawAcc(u, cx, headCy, accId)
    drawHat(u, cx, headCy, headR, hatId)
}

private fun DrawScope.drawCloth(u: Float, cx: Float, headCy: Float, headR: Float, id: String) {
    val y = headCy + headR * 0.82f
    when (id) {
        "cloth_scarf" -> {
            val path = Path().apply {
                moveTo(cx - 20f * u, y)
                lineTo(cx + 20f * u, y)
                lineTo(cx + 13f * u, y + 11f * u)
                lineTo(cx - 13f * u, y + 11f * u)
                close()
            }
            drawPath(path, Palette.ScarfRed)
            // 체크 패턴
            for (i in -3..3) {
                drawLine(
                    Color.White.copy(alpha = 0.55f),
                    Offset(cx + i * 5f * u, y),
                    Offset(cx + i * 4f * u, y + 11f * u),
                    strokeWidth = 1.6f * u
                )
            }
            drawLine(Color.White.copy(alpha = 0.5f), Offset(cx - 18f * u, y + 5f * u), Offset(cx + 18f * u, y + 5f * u), strokeWidth = 1.6f * u)
        }
        "cloth_apron" -> {
            drawRoundRectCompat(Palette.SubPink, cx - 16f * u, y + 2f * u, 32f * u, 26f * u, 6f * u)
            drawLine(Palette.PrimaryDeep, Offset(cx - 12f * u, y + 2f * u), Offset(cx + 12f * u, y + 2f * u), strokeWidth = 2f * u)
        }
        "cloth_cape" -> {
            val path = Path().apply {
                moveTo(cx - 22f * u, y)
                lineTo(cx + 22f * u, y)
                lineTo(cx + 26f * u, y + 30f * u)
                lineTo(cx - 26f * u, y + 30f * u)
                close()
            }
            drawPath(path, Palette.Lavender)
            listOf(-12f to 8f, 6f to 16f, 14f to 6f).forEach { (dx, dy) ->
                drawStar(cx + dx * u, y + dy * u, 2.6f * u, Color.White)
            }
        }
    }
}

private fun DrawScope.drawAcc(u: Float, cx: Float, headCy: Float, id: String) {
    when (id) {
        "acc_glasses" -> {
            listOf(-1f, 1f).forEach { s ->
                drawCircle(
                    color = Palette.TextBrown,
                    radius = 6.5f * u,
                    center = Offset(cx + s * 10f * u, headCy - 2f * u),
                    style = Stroke(1.8f * u)
                )
            }
            drawLine(
                Palette.TextBrown,
                Offset(cx - 3.5f * u, headCy - 2f * u),
                Offset(cx + 3.5f * u, headCy - 2f * u),
                strokeWidth = 1.8f * u
            )
        }
        "acc_ribbon" -> {
            val y = headCy - 20f * u
            listOf(-1f, 1f).forEach { s ->
                drawOval(
                    Palette.Primary,
                    Offset(cx + s * 2f * u - if (s < 0) 8f * u else 0f, y - 3.5f * u),
                    Size(8f * u, 7f * u)
                )
            }
            drawCircle(Palette.PrimaryDeep, 2.2f * u, Offset(cx, y))
        }
        "acc_bell" -> {
            drawCircle(Palette.Yellow, 4f * u, Offset(cx, headCy + 26f * u))
            drawLine(Palette.Muzzle, Offset(cx - 4f * u, headCy + 26f * u), Offset(cx + 4f * u, headCy + 26f * u), strokeWidth = 1.2f * u)
        }
    }
}

private fun DrawScope.drawHat(u: Float, cx: Float, headCy: Float, headR: Float, id: String) {
    val topY = headCy - headR
    when (id) {
        "hat_bear" -> {
            drawArcHood(u, cx, headCy, headR, Color(0xFFC79A6B))
            listOf(-1f, 1f).forEach { s ->
                drawCircle(Color(0xFFC79A6B), 7f * u, Offset(cx + s * 18f * u, topY - 4f * u))
            }
        }
        "hat_rabbit" -> {
            drawArcHood(u, cx, headCy, headR, Palette.SubPink)
            listOf(-1f, 1f).forEach { s ->
                drawOval(
                    Palette.SubPink,
                    Offset(cx + s * 11f * u - 4.5f * u, topY - 22f * u),
                    Size(9f * u, 24f * u)
                )
            }
        }
        "hat_frog" -> {
            drawArcHood(u, cx, headCy, headR, Palette.Mint)
            listOf(-1f, 1f).forEach { s ->
                drawCircle(Palette.Mint, 6.5f * u, Offset(cx + s * 14f * u, topY - 3f * u))
                drawCircle(Color.White, 3.4f * u, Offset(cx + s * 14f * u, topY - 3f * u))
                drawCircle(Palette.TextBrown, 1.7f * u, Offset(cx + s * 14f * u, topY - 3f * u))
            }
        }
        "hat_beret" -> {
            drawOval(Color(0xFFE39BA8), Offset(cx - 20f * u, topY - 8f * u), Size(40f * u, 18f * u))
            drawCircle(Color(0xFFD4808F), 3.2f * u, Offset(cx + 2f * u, topY - 9f * u))
        }
        "hat_party" -> {
            val path = Path().apply {
                moveTo(cx, topY - 26f * u)
                lineTo(cx + 12f * u, topY + 3f * u)
                lineTo(cx - 12f * u, topY + 3f * u)
                close()
            }
            drawPath(path, Palette.SkyBlue)
            drawStar(cx, topY - 8f * u, 3f * u, Color.White)
            drawStar(cx - 5f * u, topY - 1f * u, 2.2f * u, Color.White)
            drawCircle(Palette.Yellow, 3f * u, Offset(cx, topY - 27f * u))
        }
        "hat_crown" -> {
            val path = Path().apply {
                moveTo(cx - 15f * u, topY + 2f * u)
                lineTo(cx - 15f * u, topY - 10f * u)
                lineTo(cx - 7.5f * u, topY - 2f * u)
                lineTo(cx, topY - 14f * u)
                lineTo(cx + 7.5f * u, topY - 2f * u)
                lineTo(cx + 15f * u, topY - 10f * u)
                lineTo(cx + 15f * u, topY + 2f * u)
                close()
            }
            drawPath(path, Palette.Yellow)
            drawCircle(Palette.Primary, 2.4f * u, Offset(cx, topY - 3f * u))
        }
    }
}

private fun DrawScope.drawArcHood(u: Float, cx: Float, headCy: Float, headR: Float, color: Color) {
    drawArc(
        color = color,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(cx - headR - 2f * u, headCy - headR - 2f * u),
        size = Size((headR + 2f * u) * 2, (headR + 2f * u) * 2),
        style = Stroke(width = 9f * u)
    )
}

private fun DrawScope.drawStar(cx: Float, cy: Float, r: Float, color: Color) {
    val path = Path()
    for (i in 0 until 10) {
        val rad = if (i % 2 == 0) r else r * 0.45f
        val a = Math.toRadians((i * 36 - 90).toDouble())
        val x = cx + rad * kotlin.math.cos(a).toFloat()
        val y = cy + rad * sin(a).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

private fun DrawScope.drawRoundRectCompat(color: Color, x: Float, y: Float, w: Float, h: Float, r: Float) {
    val path = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(Rect(x, y, x + w, y + h), androidx.compose.ui.geometry.CornerRadius(r, r))) }
    drawPath(path, color)
}

private fun DrawScope.drawZzz(u: Float, w: Float, h: Float, p: Float) {
    listOf(0 to 1f, 1 to 0.8f, 2 to 0.6f).forEach { (i, scale) ->
        val x = w * 0.68f + i * 7f * u
        val y = h * 0.28f - i * 7f * u - p * 4f * u
        val s = 6f * u * scale
        drawLine(Palette.SkyBlue, Offset(x, y), Offset(x + s, y), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
        drawLine(Palette.SkyBlue, Offset(x + s, y), Offset(x, y + s), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
        drawLine(Palette.SkyBlue, Offset(x, y + s), Offset(x + s, y + s), strokeWidth = 1.8f * u, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawBowl(u: Float, w: Float, h: Float) {
    val cx = w / 2f
    val y = h * 0.78f
    drawArc(
        color = Palette.ScarfRed,
        startAngle = 0f, sweepAngle = 180f, useCenter = true,
        topLeft = Offset(cx - 14f * u, y - 9f * u),
        size = Size(28f * u, 18f * u)
    )
    drawOval(Color.White, Offset(cx - 13f * u, y - 12f * u), Size(26f * u, 8f * u))
}

private fun DrawScope.drawTub(u: Float, w: Float, h: Float) {
    val cx = w / 2f
    val y = h * 0.72f
    drawRoundRectCompat(Palette.SkyBlue, cx - 38f * u, y, 76f * u, 26f * u, 12f * u)
}

private fun DrawScope.drawBubbles(u: Float, w: Float, h: Float, p: Float) {
    val seeds = listOf(0.22f to 0.48f, 0.78f to 0.4f, 0.3f to 0.24f, 0.7f to 0.62f, 0.5f to 0.18f)
    seeds.forEachIndexed { i, (fx, fy) ->
        val r = (3f + (i % 3) * 1.6f) * u
        val dy = sin((p * 6.28f + i).toDouble()).toFloat() * 3f * u
        drawCircle(Color.White.copy(alpha = 0.75f), r, Offset(w * fx, h * fy + dy))
    }
}

private fun DrawScope.drawHearts(u: Float, w: Float, h: Float, p: Float) {
    listOf(0.18f to 0.42f, 0.82f to 0.36f).forEachIndexed { i, (fx, fy) ->
        val dy = -p * 6f * u - i * 2f * u
        drawHeart(w * fx, h * fy + dy, 5f * u, Palette.Primary)
    }
}

private fun DrawScope.drawHeart(cx: Float, cy: Float, r: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy + r * 0.9f)
        cubicTo(cx - r * 1.6f, cy - r * 0.3f, cx - r * 0.5f, cy - r * 1.3f, cx, cy - r * 0.35f)
        cubicTo(cx + r * 0.5f, cy - r * 1.3f, cx + r * 1.6f, cy - r * 0.3f, cx, cy + r * 0.9f)
        close()
    }
    drawPath(path, color)
}
