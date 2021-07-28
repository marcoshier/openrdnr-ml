import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.colorBuffer
import org.openrndr.draw.depthBuffer
import org.openrndr.draw.isolatedWithTarget
import org.openrndr.draw.renderTarget
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

fun main() = application {
    configure {
        width = 800
        height = 800
    }
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
            it.frame.copyTo(
                videoImage,
                targetRectangle = IntRectangle(xOffset, videoImage.height - yOffset, it.frame.width, -it.frame.height)
            )
        }
        val rt = renderTarget(width, height) {
            depthBuffer()
            colorBuffer()
        }
        extend {
            video.draw(drawer, blind = true)
            //rt.clearColor(0, ColorRGBa.TRANSPARENT)
            //drawer.image(videoImage)
            val regions = detector.detect(videoImage)

            for (region in regions) {
                computeRoi(region)

                val lms = landmarks.extract(drawer, region, videoImage)
                drawer.fill = null
                drawer.isolatedWithTarget(rt) {
                    for (lm in lms.indices) {

                        drawer.stroke = null
                        drawer.fill = rgb(0.3, 0.5, cos(seconds * 0.4) + 1.0 * 0.5)

                        drawer.circle(lms[lm].imagePosition, 3.0)
                    }
                }
            }
            drawer.image(rt.colorBuffer(0))

        }
    }
}
