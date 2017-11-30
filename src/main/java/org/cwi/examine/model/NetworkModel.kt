package org.cwi.examine.model

interface NetworkElement {
    val identifier: String // Unique identifier.
    val name: String       // User-friendly name.
    val url: String        // Reference URL.
    val score: Double      // Some significance score.
}

data class NetworkNode(
        override val identifier: String,
        override val name: String,
        override val url: String,
        override val score: Double,
        val annotations: Set<NetworkAnnotation>
) : NetworkElement


