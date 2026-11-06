package com.serg.mafia

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PlayStoreAssetsTest {

    private val locales = listOf("uk", "en-US", "ru-RU")
    private val rootDir: File by lazy {
        var dir = File(".").canonicalFile
        while (dir.parentFile != null && !File(dir, "store").exists()) {
            dir = dir.parentFile
        }
        dir
    }

    @Test
    fun `проверка наличия основных документов и графики store play`() {
        val storePlay = File(rootDir, "store/play")
        assertTrue("Каталог store/play должен существовать", storePlay.exists())

        val icon = File(storePlay, "icon.png")
        assertTrue("store/play/icon.png должен существовать", icon.exists())
        assertTrue("Размер icon.png <= 1MB", icon.length() in 1..(1024 * 1024))

        val feat = File(storePlay, "featureGraphic.png")
        assertTrue("store/play/featureGraphic.png должен существовать", feat.exists())
        assertTrue("Размер featureGraphic.png <= 15MB", feat.length() in 1..(15 * 1024 * 1024))

        val dataSafety = File(storePlay, "data-safety.md")
        assertTrue("data-safety.md должен существовать", dataSafety.exists())

        val screenshots = File(storePlay, "SCREENSHOTS.md")
        assertTrue("SCREENSHOTS.md должен существовать", screenshots.exists())
    }

    @Test
    fun `проверка структуры метаданных и ограничений длины текстов Google Play`() {
        val metadataDir = File(rootDir, "store/play/metadata")
        assertTrue("store/play/metadata должен существовать", metadataDir.exists())

        val latinIRegex = Regex("([а-яА-ЯёЁіїєґІЇЄҐ]*[iI][а-яА-ЯёЁіїєґІЇЄҐ]+|[а-яА-ЯёЁіїєґІЇЄҐ]+[iI][а-яА-ЯёЁіїєґІЇЄҐ]*)")

        locales.forEach { loc ->
            val locDir = File(metadataDir, loc)
            assertTrue("Локаль $loc должна присутствовать", locDir.exists())

            val titleFile = File(locDir, "title.txt")
            assertTrue("title.txt должен существовать для $loc", titleFile.exists())
            val title = titleFile.readText().trim()
            assertTrue("title не пустой для $loc", title.isNotEmpty())
            assertTrue("title <= 30 символов ($loc: ${title.length}): '$title'", title.length <= 30)

            val shortFile = File(locDir, "short-description.txt")
            assertTrue("short-description.txt должен существовать для $loc", shortFile.exists())
            val short = shortFile.readText().trim()
            assertTrue("short-description не пустой для $loc", short.isNotEmpty())
            assertTrue("short-description <= 80 символов ($loc: ${short.length}): '$short'", short.length <= 80)

            val fullFile = File(locDir, "full-description.txt")
            assertTrue("full-description.txt должен существовать для $loc", fullFile.exists())
            val full = fullFile.readText().trim()
            assertTrue("full-description не пустой для $loc", full.isNotEmpty())
            assertTrue("full-description <= 4000 символов ($loc: ${full.length})", full.length <= 4000)

            val imagesDir = File(locDir, "images")
            assertTrue("images/ должен существовать для $loc", imagesDir.exists())
            assertTrue("images/icon.png должен существовать для $loc", File(imagesDir, "icon.png").exists())
            assertTrue("images/featureGraphic.png должен существовать для $loc", File(imagesDir, "featureGraphic.png").exists())

            if (loc == "uk") {
                assertFalse("Украинский title не должен содержать латинскую i", latinIRegex.containsMatchIn(title))
                assertFalse("Украинский short-description не должен содержать латинскую i", latinIRegex.containsMatchIn(short))
                assertFalse("Украинский full-description не должен содержать латинскую i", latinIRegex.containsMatchIn(full))
            }
        }
    }
}
