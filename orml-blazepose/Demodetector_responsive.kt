import org.openrndr.Program
import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.*
import org.openrndr.extra.fx.blur.FrameBlur
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.math.IntVector2
import org.openrndr.math.Vector2
import org.openrndr.shape.IntRectangle
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

fun main() = application {
    configure {
        width = 1920
        height = 1080
    }
    program {
        val video = loadVideo("demo-data/jrn.mp4")
        video.play()
        video.ended.listen {
            video.restart()
        }
        val detector = BlazePoseDetector.load()
        val landmarks = BlazePoseLandmarks.fullBody()
        val longestVideoAxis = max(video.width, video.height)


        val videoRt = renderTarget(width / 2, height) {
            colorBuffer()
        }
        val blazeposeRt = renderTarget(width, height) {
            depthBuffer()
            colorBuffer()
        }
        val videoImage = colorBuffer(video.width, video.height)
        

        if(longestVideoAxis == video.width) {
            videoWide(video, videoImage)
        } else {
            videoHigh(video, videoImage)
        }


        extend {
            blazeposeRt.clearColor(0, ColorRGBa.BLACK)

            video.draw(drawer, blind = true)
            videoImage.copyTo(videoRt.colorBuffer(0))

            val regions = detector.detect(videoRt.colorBuffer(0))
            for (region in regions) {
                computeRoi(region)

                drawer.isolatedWithTarget(blazeposeRt) {
                    val lms = landmarks.extract(drawer, region, videoRt.colorBuffer(0))
                    drawer.fill = null
                    for (lm in lms.indices) {

                        drawer.stroke = ColorRGBa.WHITE
                        drawer.fill = rgb(0.0, 0.5, 0.5)

                        drawer.circle(lms[lm].imagePosition, 5.0)
                    }
                }
            }

            drawer.image(blazeposeRt.colorBuffer(0))
            drawer.image(videoRt.colorBuffer(0), width / 2.0, 0.0)
        }
    }
}


private fun Program.videoWide(video: VideoPlayerFFMPEG, videoImage: ColorBuffer) {

    val ratioX = width / 2.0 / video.width * 1.0
    val scaledWidth = video.width * ratioX
    val scaledHeight = (scaledWidth * video.height) / video.width
    val yOffset = height - scaledHeight

    video.newFrame.listen {
        it.frame.copyTo(
            videoImage,
            targetRectangle = IntRectangle(0, (scaledHeight + yOffset).toInt() , scaledWidth.toInt(), -scaledHeight.toInt())
        )

    }
}

private fun Program.videoHigh(video: VideoPlayerFFMPEG, videoImage: ColorBuffer) {
// if video high
    val ratioY = height * 1.0 / video.height * 1.0
    val ratioX = video.width * 1.0 / width / 1.5
    val scaledWidth = video.width * ratioX

    val xOffset = (width / 2.0 - scaledWidth) / 2.0
    video.newFrame.listen {
        it.frame.copyTo(
            videoImage,
            targetRectangle = IntRectangle(xOffset.toInt(), it.frame.height, scaledWidth.toInt(), -(it.frame.height * ratioY).toInt())
        )
        println(ratioX)
    }
}
