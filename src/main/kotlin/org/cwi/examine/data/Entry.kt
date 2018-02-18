package org.cwi.examine.data

/**
 * Bean entry for loading nodes via CSV library.
 */
class NodeEntry {
    var identifier: String? = null
    var name: String? = null
    var url: String? = null
    var score: Double? = .0
}

/**
 * Bean entry for loading annotations via CSV library.
 */
class AnnotationEntry {
    var identifier: String? = null
    var name: String? = null
    var url: String? = null
    var score: Double? = .0
    var category: String? = null
}