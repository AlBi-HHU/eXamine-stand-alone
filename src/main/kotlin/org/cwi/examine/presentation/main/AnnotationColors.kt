package org.cwi.examine.presentation.main

import javafx.beans.property.ReadOnlyMapProperty
import javafx.beans.property.SimpleMapProperty
import javafx.collections.FXCollections.observableHashMap
import javafx.scene.paint.Color
import javafx.scene.paint.Color.rgb
import org.cwi.examine.model.NetworkAnnotation
import java.util.*
import java.util.Arrays.asList

internal class AnnotationColors {

    private val availableColors = LinkedList(asList(*PALETTE))
    private val colorMap = SimpleMapProperty(observableHashMap<NetworkAnnotation, Color>())

    /**
     * Fetch an available color and assign it to the given annotation.
     * No result if no more colors are available.
     *
     * @param annotation The annotation to assign a color to.
     * @return A unique color for the given annotation, or Empty if no color is available.
     */
    fun assignColor(annotation: NetworkAnnotation): Optional<Color> {

        assert(!colorMap.containsKey(annotation))

        val optionalColor = Optional.of(availableColors.poll())
        optionalColor.ifPresent { color -> colorMap[annotation] = color }
        return optionalColor
    }

    /**
     * Release the color that has been assigned to the given annotation.
     * This color will be available for future use.
     *
     * @param annotation The annotation to release the color of.
     */
    fun releaseColor(annotation: NetworkAnnotation) {

        assert(colorMap.containsKey(annotation))

        availableColors.add(colorMap.remove(annotation))
    }

    /**
     * Release the color of the given annotation if it has an assigned color.
     * Assign a color to the given annotation if it does not have an assigned color.
     *
     * @param annotation The annotation to toggle a color for.
     */
    fun toggleColor(annotation: NetworkAnnotation) {

        if (colorMap.containsKey(annotation)) {
            releaseColor(annotation)
        } else {
            assignColor(annotation)
        }
    }

    /**
     * Clear all assigned colors, effectively making the entire palette available again.
     */
    fun clear() {
        colorMap.clear()
        availableColors.clear()
        availableColors.addAll(asList(*PALETTE))
    }

    /**
     * @return The mapping of annotations and their assigned colors.
     */
    fun colorMapProperty(): ReadOnlyMapProperty<NetworkAnnotation, Color> {
        return colorMap
    }

    companion object {

        private val PALETTE = arrayOf(rgb(141, 211, 199), rgb(255, 255, 179), rgb(190, 186, 218), rgb(251, 128, 114), rgb(128, 177, 211), rgb(253, 180, 98), rgb(252, 205, 229), rgb(188, 128, 189), rgb(204, 235, 197), rgb(255, 237, 111))
    }

}
