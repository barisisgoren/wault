package app.wault

import androidx.compose.ui.graphics.Color
import app.wault.data.CardKind
import app.wault.data.InkMode
import app.wault.design.CardArts
import app.wault.design.colorFromArgbLong
import app.wault.design.contrastRatio
import app.wault.design.hsvColor
import app.wault.design.parseHexColor
import app.wault.design.prefersLightInk
import app.wault.design.toArgbLong
import app.wault.design.toHex
import app.wault.design.toHsv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import androidx.compose.ui.graphics.luminance

class ColorMathTest {

    @Test fun hexRoundTrip() {
        listOf("#2F6BE0", "#FFFFFF", "#000000", "#EC6740").forEach {
            assertEquals(it, parseHexColor(it)!!.toHex())
        }
    }

    @Test fun hexAcceptsShortFormAndMissingHash() {
        assertEquals("#FF8800", parseHexColor("f80")!!.toHex())
        assertEquals("#112233", parseHexColor("  112233 ")!!.toHex())
        assertEquals("#AABBCC", parseHexColor("#abc")!!.toHex())
    }

    @Test fun hexRejectsGarbage() {
        listOf("", "#", "#12", "#1234", "#12345", "#1234567", "#GGHHII", "red", "#12 456").forEach {
            assertNull("'$it' geçersiz sayılmalı", parseHexColor(it))
        }
    }

    @Test fun argbLongRoundTrip() {
        val c = Color(0xFF2F6BE0)
        assertEquals(0xFF2F6BE0L, c.toArgbLong())
        assertEquals("#2F6BE0", colorFromArgbLong(c.toArgbLong()).toHex())
    }

    @Test fun hsvRoundTrip() {
        listOf(Triple(0f, 1f, 1f), Triple(120f, 0.5f, 0.8f), Triple(215f, 0.9f, 0.75f), Triple(330f, 0.3f, 1f), Triple(48f, 0.85f, 0.95f)).forEach { (h, s, v) ->
            val back = hsvColor(h, s, v).toHsv()
            assertEquals(s, back.s, 0.01f)
            assertEquals(v, back.v, 0.01f)
            if (s > 0f) assertEquals(h, back.h, 1.0f)
        }
    }

    @Test fun contrastRatioMatchesWcag() {
        assertEquals(21f, contrastRatio(Color.White, Color.Black), 0.1f)
        assertEquals(1f, contrastRatio(Color.White, Color.White), 0.001f)
    }

    @Test fun inkFollowsBackground() {
        assertTrue(prefersLightInk(Color.Black))
        assertTrue(prefersLightInk(Color(0xFF2F6BE0)))
        assertFalse(prefersLightInk(Color.White))
        assertFalse(prefersLightInk(Color(0xFFEDB93C))) // sarı: koyu yazı
    }

    @Test fun everyPresetIsReadable() {
        CardArts.presets.forEach { art ->
            val ratio = contrastRatio(art.ink, art.base)
            assertTrue("${art.id}: yazı kontrastı $ratio < 3", ratio >= 3f)
        }
    }

    @Test fun presetIdsAreUniqueAndKeepLegacyIds() {
        val ids = CardArts.presets.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        // Eski gradyan görsellerin kimlikleri: kayıtlı kartlar bozulmamalı
        listOf("midnight", "graphite", "ocean", "indigo", "teal", "forest", "plum", "sunset", "crimson", "gold", "rose", "silver", "ivory")
            .forEach { assertTrue("$it kayıp", it in ids) }
    }

    @Test fun defaultsExistForEveryKind() {
        CardKind.entries.forEach { kind ->
            val id = CardArts.defaultFor(kind)
            assertTrue(id == "auto" || CardArts.presets.any { it.id == id })
        }
    }

    @Test fun unknownArtFallsBackToMidnight() {
        assertEquals("midnight", CardArts.byId("yok-boyle-bir-sey").id)
        assertEquals("midnight", CardArts.byId(null).id)
    }

    @Test fun inkOverrideWinsOverAutomatic() {
        val ocean = CardArts.byId("ocean")
        assertTrue(ocean.lightInk)
        assertFalse(ocean.withInk(InkMode.DARK).lightInk)
        assertTrue(ocean.withInk(InkMode.AUTO).lightInk)
        val gold = CardArts.byId("gold")
        assertFalse(gold.lightInk)
        assertTrue(gold.withInk(InkMode.LIGHT).lightInk)
    }

    @Test fun gradientTonesAreDerivedFromTheFlatBase() {
        CardArts.presets.forEach { art ->
            assertTrue("${art.id}: üst ton alttan açık olmalı", art.top.luminance() >= art.bottom.luminance())
            if (art.id !in setOf("white", "ink")) assertNotEquals(art.top, art.bottom)
        }
    }

    @Test fun accentIsReadableOnBothThemes() {
        CardArts.presets.forEach { art ->
            assertNotNull(art.accentOn(true))
            assertNotNull(art.accentOn(false))
        }
        val navy = CardArts.byId("indigo")
        assertTrue(navy.accentOn(true).luminance() > navy.base.luminance()) // koyu temada parlatılır
    }
}
