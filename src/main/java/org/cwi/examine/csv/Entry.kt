package org.cwi.examine.csv

/**
 * Row element fields that occur for both nodes and annotations.
 */
interface Entry {
    var identifier: String? // Unique identifier.
    var name: String?       // User-friendly name.
    var url: String?        // Reference URL.
    var score: Double?      // Some significance score.
}

class AnnotationEntry : Entry {
    override var identifier: String? = null
    override var name: String? = null
    override var url: String? = null
    override var score: Double? = .0
    var category: String? = null   // Category identifier.
}

class NodeEntry : Entry {
    override var identifier: String? = null
    override var name: String? = null
    override var url: String? = null
    override var score: Double? = .0
    var module: String? = null
    var processes: String? = null
    var functions: String? = null
    var components: String? = null
    var pathways: String? = null
}