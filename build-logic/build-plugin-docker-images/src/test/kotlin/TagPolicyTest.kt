package com.gitlab.davinkevin.podcastserver.dockerimages

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TagPolicyTest {

    private val refs = Refs(
        branches = listOf("main", "272-perf-youtube"),
        tags = listOf("2026.1.0", "2024.8.0"),
    )

    @Test
    fun `slugify lower-cases the ref`() {
        assertEquals("feat-search", TagPolicy.slugify("Feat-Search"))
    }

    @Test
    fun `slugify replaces everything outside 0-9a-z`() {
        assertEquals("2026-1-0", TagPolicy.slugify("2026.1.0"))
        assertEquals("feat-search-panel", TagPolicy.slugify("feat/search_panel"))
    }

    @Test
    fun `slugify truncates to 63 characters`() {
        val ref = "273-feat-search-advanced-filter-panel-on-the-library-search-starting-with-tags"

        assertEquals("273-feat-search-advanced-filter-panel-on-the-library-search-sta", TagPolicy.slugify(ref))
    }

    @Test
    fun `slugify trims the dash truncation leaves behind`() {
        assertEquals("a".repeat(62), TagPolicy.slugify("a".repeat(62) + ".suffix"))
    }

    @Test
    fun `slugify trims leading dashes`() {
        assertEquals("wip-fix", TagPolicy.slugify("_wip/fix"))
    }

    @Test
    fun `keepList holds both spellings of a git tag`() {
        val keep = TagPolicy.keepList(Refs(branches = emptyList(), tags = listOf("2026.1.0")))

        assertContains(keep, "2026.1.0")
        assertContains(keep, "2026-1-0")
    }

    @Test
    fun `keepList holds the tags no branch produces`() {
        val keep = TagPolicy.keepList(Refs(branches = listOf("main"), tags = emptyList()))

        assertContains(keep, "latest")
        assertContains(keep, "main")
    }

    @Test
    fun `orphans reports the tag of a deleted branch`() {
        val tags = listOf("main", "272-perf-youtube", "252-feat-backend-drop-rtmp-download-support")

        assertEquals(listOf("252-feat-backend-drop-rtmp-download-support"), TagPolicy.orphans(tags, refs))
    }

    @Test
    fun `orphans reports nothing when every tag maps to a ref`() {
        val tags = listOf("main", "latest", "272-perf-youtube", "2026.1.0")

        assertEquals(emptyList(), TagPolicy.orphans(tags, refs))
    }

    @Test
    fun `orphans spares a published release whose git tag is spelled differently`() {
        assertEquals(emptyList(), TagPolicy.orphans(listOf("2024.08.0"), refs))
    }

    @Test
    fun `orphans spares anything shaped like a version`() {
        val tags = listOf("v1.0.0", "3.2", "2027.12.0", "1.2.3-rc1")

        assertEquals(emptyList(), TagPolicy.orphans(tags, refs))
    }

    @Test
    fun `orphans still reports a branch merely starting with digits`() {
        assertEquals(listOf("2026-feat-something"), TagPolicy.orphans(listOf("2026-feat-something"), refs))
    }

    @Test
    fun `orphans deduplicates and sorts`() {
        val tags = listOf("zzz-gone", "aaa-gone", "zzz-gone")

        assertEquals(listOf("aaa-gone", "zzz-gone"), TagPolicy.orphans(tags, refs))
    }

    @Test
    fun `a ref listing too short is not trustworthy`() {
        assertFalse(TagPolicy.isTrustworthy(Refs(branches = listOf("main"), tags = emptyList())))
    }

    @Test
    fun `a plausible ref listing is trustworthy`() {
        val branches = (1..TagPolicy.MIN_TRUSTED_REFS).map { "branch-$it" }

        assertTrue(TagPolicy.isTrustworthy(Refs(branches = branches, tags = emptyList())))
    }
}
