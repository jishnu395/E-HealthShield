package com.ehealthshield.backend.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SseServiceTest {

    private SseService sseService;
    private static final String TEST_MASTER_KEY = "test_sse_master_key_secret_256_bits_123456";

    @BeforeEach
    void setUp() {
        sseService = new SseService(TEST_MASTER_KEY);
    }

    @Test
    @DisplayName("HMAC-SHA256 generates deterministic search tags for identical keywords")
    void testDeterministicTagGeneration() {
        String keyword = "cardiology";

        String tag1 = sseService.generateSearchTag(keyword);
        String tag2 = sseService.generateSearchTag(keyword);

        assertNotNull(tag1);
        assertEquals(64, tag1.length(), "HMAC-SHA256 hex string must be 64 characters");
        assertEquals(tag1, tag2, "Search tags for identical keywords must be deterministic and identical");
    }

    @Test
    @DisplayName("Trapdoor generation produces identical output to search tag generation for matching queries")
    void testTrapdoorMatchesSearchTag() {
        String keyword = "diabetes";

        String searchTag = sseService.generateSearchTag(keyword);
        String trapdoor = sseService.generateTrapdoor(keyword);

        assertEquals(searchTag, trapdoor, "Trapdoor for keyword must match its stored search tag exactly");
    }

    @Test
    @DisplayName("HMAC generates distinct tags for different keywords")
    void testDifferentKeywordsProduceDifferentTags() {
        String tag1 = sseService.generateSearchTag("cardiology");
        String tag2 = sseService.generateSearchTag("neurology");
        String tag3 = sseService.generateSearchTag("oncology");

        assertNotEquals(tag1, tag2);
        assertNotEquals(tag2, tag3);
        assertNotEquals(tag1, tag3);
    }

    @Test
    @DisplayName("Keyword normalization correctly ignores case and whitespace differences")
    void testKeywordNormalization() {
        String tagLower = sseService.generateSearchTag("hypertension");
        String tagUpper = sseService.generateSearchTag("  HYPERTENSION  ");
        String tagMixed = sseService.generateSearchTag(" Hypertension ");

        assertEquals(tagLower, tagUpper, "Tags must be identical regardless of casing and surrounding whitespace");
        assertEquals(tagLower, tagMixed, "Tags must be identical regardless of casing and whitespace");
    }

    @Test
    @DisplayName("Batch keyword tagging generates unique set of tags")
    void testBatchTagGeneration() {
        List<String> keywords = List.of("pediatrics", "immunology", "pediatrics", "  IMMUNOLOGY  ");
        Set<String> tags = sseService.generateSearchTags(keywords);

        assertEquals(2, tags.size(), "Duplicate normalized keywords should resolve to unique tag entries");
    }
}
