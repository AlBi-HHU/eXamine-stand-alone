package org.hhu.examine.main

import javafx.collections.FXCollections.observableHashMap
import javafx.collections.ObservableMap
import javafx.scene.paint.Color
import javafx.scene.paint.Color.rgb
import org.hhu.examine.data.NetworkAnnotation

/**
 * Assigns colors to annotations and keeps track of a palette of available colors.
 * Colors are assigned in cyclic manner.
 */
internal class AnnotationColors {

    val colorMap: ObservableMap<NetworkAnnotation, Color> = observableHashMap()

    private val availableColors = ArrayList(COLOR_TAG_PALETTE)

    /**
     * Fetch an available color and assign it to the given annotation.
     * No result if no more colors are available.
     */
    fun assignColor(annotation: NetworkAnnotation): Color? {
        assert(!colorMap.containsKey(annotation))

        val optionalColor = availableColors.firstOrNull()
        optionalColor?.apply {
            availableColors.remove(optionalColor)
            colorMap[annotation] = optionalColor
        }
        return optionalColor
    }

    /**
     * Release the color that has been assigned to the given annotation.
     * This color will be available for future use.
     */
    fun releaseColor(annotation: NetworkAnnotation) {
        assert(colorMap.containsKey(annotation))

        availableColors.add(colorMap.remove(annotation))
    }

    /**
     * Release the color of the given annotation if it has an assigned color.
     * Assign a color to the given annotation if it does not have an assigned color.
     */
    fun toggleColor(annotation: NetworkAnnotation) {

        if (colorMap.containsKey(annotation)) {
            releaseColor(annotation)
        } else {
            assignColor(annotation)
        }
    }

    /** Clear all assigned colors, effectively making the entire palette available again.*/
    fun clear() {
        colorMap.clear()
        availableColors.clear()
        availableColors.addAll(COLOR_TAG_PALETTE)
    }

    /** Replace all color assignments by the given mapping. */
    fun putAllColors(annotationsToColors: Map<NetworkAnnotation, Color>) {
        clear()
        colorMap.putAll(annotationsToColors)
    }

}

private val COLOR_TAG_PALETTE = listOf(
        rgb(141, 211, 199),
        rgb(255, 255, 179),
        rgb(190, 186, 218),
        rgb(251, 128, 114),
        rgb(128, 177, 211),
        rgb(253, 180, 98),
        rgb(252, 205, 229),
        rgb(188, 128, 189),
        rgb(204, 235, 197),
        rgb(255, 237, 111)
)