package com.example.fcmpush.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TopicRulesTest {
    @Test
    fun `normalizes whitespace and topics prefix`() {
        assertEquals("release_updates", TopicRules.normalize("  /topics/release_updates  "))
    }

    @Test
    fun `accepts all characters supported by FCM`() {
        assertEquals("News-1_eu.west~all%25", TopicRules.normalize("News-1_eu.west~all%25"))
    }

    @Test
    fun `rejects spaces and slashes`() {
        assertThrows(IllegalArgumentException::class.java) {
            TopicRules.normalize("invalid topic/name")
        }
    }

    @Test
    fun `rejects an empty topic`() {
        assertThrows(IllegalArgumentException::class.java) {
            TopicRules.normalize("  ")
        }
    }
}
