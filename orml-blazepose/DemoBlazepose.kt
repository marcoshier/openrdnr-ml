import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.shape.IntRectangle
import kotlin.math.max

fun  main() = application {
    configure {
        width = 800
        height = 800
        hideWindowDecorations = true
    }


    val connections: List<Pair<Int, Int>> = listOf(
        Pair(11,12),
        Pair(12,14),
        Pair(14,16),
        Pair(11,13),
        Pair(13,15),
        Pair(11,23),
        Pair(23,24),
        Pair(24,12),
        Pair(23,25),
        Pair(11,12),
        Pair(25,27),
        Pair(24,26),
        Pair(26,28)
    )


    program {
        val video = loadVideoDevice()
        video.play()

        video.ended.listen {
            video.restart()
        }


        val detector = BlazePoseDetector.load()
        val landmarks = BlazePoseLandmarks.fullBody()
        val longestVideoAxis = max(video.width, video.height)
        val videoImage = colorBuffer(longestVideoAxis, longestVideoAxis)

        video.newFrame.listen {
            val xOffset = (longestVideoAxis - it.frame.width) / 2
            val yOffset = (longestVideoAxis - it.frame.height) / 2
            it.frame.copyTo(videoImage, targetRectangle = IntRectangle(xOffset, videoImage.height - yOffset, it.frame.width, -it.frame.height))
        }


        extend {

            video.draw(drawer, blind = true)
            //drawer.image(videoImage)


            val regions = detector.detect(videoImage)

            for (region in regions) {
                computeRoi(region)

                val lms = landmarks.extract(drawer, region,  videoImage)
                drawer.fill = null

                // draw face landmarks
                for (facePoint in 0 until 10){

                    drawer.stroke = null
                    drawer.fill = ColorRGBa.PINK

                    drawer.circle(lms[facePoint].imagePosition, 3.0)
                }

                // draw connections
                connections.forEach { connection ->

                    drawer.stroke = ColorRGBa.PINK
                    drawer.strokeWeight = 3.0

                    val firstPart = lms[connection.first]
                    val secondPart = lms[connection.second]
                    drawer.lineSegment(firstPart.imagePosition, secondPart.imagePosition)

                }
            }

        }

    }

}
