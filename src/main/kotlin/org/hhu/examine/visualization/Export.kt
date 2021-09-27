package org.hhu.examine.visualization.export

import javafx.embed.swing.SwingFXUtils
import javafx.scene.SnapshotParameters
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.layout.Region
import javafx.scene.paint.Color
import javafx.scene.transform.Transform
import javafx.stage.FileChooser
import tornadofx.FileChooserMode
import tornadofx.chooseFile
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.ceil
import kotlin.math.max

fun regionToImage(contentRegion: Region, padding: Double, vararg legendRegions: Region): WritableImage {
    val pixelScale = 2.0

    // Snapshot images.
    val parameters = SnapshotParameters()
    parameters.setTransform(Transform.scale(pixelScale, pixelScale))
    parameters.fill = Color.TRANSPARENT
    
    val contentImage = contentRegion.snapshot(parameters, null)
    val legendImages = legendRegions.map { it.snapshot(parameters, null) }

    // Allocate image with encompassing dimensions.
    val imageWidth = contentImage.width + padding + (legendImages.map(WritableImage::getWidth).maxOrNull() ?: 0.0)
    val imageHeight = max(
            contentImage.height,
            legendImages.map(WritableImage::getHeight).sum() + max(0, legendImages.size - 1) * padding
    )
    val image = WritableImage(ceil(pixelScale * imageWidth).toInt(), ceil(pixelScale * imageHeight).toInt())

    // Write the images to a joint image.
    image.pixelWriter.setPixels(
            0,
            0,
            contentImage.width.toInt(),
            contentImage.height.toInt(),
            contentImage.pixelReader,
            0,
            0)

    val legendX = contentImage.width + padding
    var legendY = 0.0
    legendImages.forEach { legendImage ->
        image.pixelWriter.setPixels(
                legendX.toInt(),
                legendY.toInt(),
                legendImage.width.toInt(),
                legendImage.height.toInt(),
                legendImage.pixelReader,
                0,
                0)
        legendY += legendImage.height + padding
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