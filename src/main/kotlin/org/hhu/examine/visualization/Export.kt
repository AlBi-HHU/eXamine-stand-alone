package org.hhu.examine.visualization

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.Region
import javafx.stage.FileChooser
import tornadofx.FileChooserMode
import tornadofx.chooseFile
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

fun regionToImage(contentRegion: Region, padding: Double, vararg legendRegions: Region): WritableImage {

    // Allocate image with encompassing dimensions.
    val imageWidth = contentRegion.width + padding + (legendRegions.map(Region::getWidth).max() ?: 0.0)
    val imageHeight = max(contentRegion.height, legendRegions.map(Region::getHeight).max() ?: 0.0)
    val image = WritableImage(ceil(imageWidth).toInt(), ceil(imageHeight).toInt())

    // Write snapshots.
    val contentImage = contentRegion.snapshot(null, image)

    val legendX = contentRegion.width + padding
    var legendY = 0.0
    legendRegions.forEach { region ->
        val legendImage = region.snapshot(null, null)
        contentImage.pixelWriter.setPixels(
                floor(legendX).toInt(),
                floor(legendY).toInt(),
                ceil(legendImage.width).toInt(),
                ceil(legendImage.height).toInt(),
                legendImage.pixelReader,
                0,
                0)
    }

    return image
}

fun imageToClipboard(image: Image) {
    val cc = ClipboardContent()
    cc.putImage(image)
    Clipboard.getSystemClipboard().setContent(cc)
}

fun imageToFileByDialog(image: Image) {
    val files = chooseFile(
            "Image Export",
            arrayOf(FileChooser.ExtensionFilter("PNG", "png")),
            FileChooserMode.Save
    )

    if (files.isNotEmpty()) {
        val bufferedImage = SwingFXUtils.fromFXImage(image, null)
        val file = files[0]
        val fileWithExtension =
                if (file.extension.toLowerCase() == "png")
                    file
                else
                    File(file.canonicalPath + ".png")

        ImageIO.write(bufferedImage, "png", fileWithExtension)
    }
}