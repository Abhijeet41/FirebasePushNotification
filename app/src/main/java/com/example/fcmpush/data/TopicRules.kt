package com.example.fcmpush.data

object TopicRules {
    private val allowedTopic = Regex("[a-zA-Z0-9-_.~%]{1,900}")

    fun normalize(rawTopic: String): String {
        val normalized = rawTopic.trim().removePrefix("/topics/")
        require(allowedTopic.matches(normalized)) {
            "Use 1–900 letters, numbers, or these characters: - _ . ~ %"
        }
        return normalized
    }
}
