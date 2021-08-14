import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.orml.facemesh.BlazeFace
import org.openrndr.orml.facemesh.FaceMesh
import org.openrndr.orml.ssd.*
import org.openrndr.shape.IntRectangle

/**
 * Demonstration of BlazeFace + ObjectTracker
 */
fun main() {
    application {
        configure {
            width = 640
            height = 640
        }
        program {
            val video = loadVideoDevice()
            video.play()
            val squareImage = colorBuffer(640, 640)
            video.newFrame.listen {
                it.frame.copyTo(squareImage, targetRectangle = IntRectangle(-(video.width - width) / 2, squareImage.height - (squareImage.height - 480) / 2, it.frame.width, -it.frame.height))
            }
            val objectTracker = ObjectTracker()
            objectTracker.newObject.listen {
                println("new FACE!")
            }
            objectTracker.lostObject.listen {
                println("lost FACE!")
            }
            val blazeFace = BlazeFace.load()
            val faceMesh = FaceMesh.load()

            extend {
                video.draw(drawer, blind = true)
                val rectangles = blazeFace.detectFaces(squareImage, filter = false)
                //drawer.image(squareImage)
                objectTracker.update(rectangles)

                drawer.stroke = null
                drawer.fill = null
                drawer.rectangle(objectTracker.objectSmoothRectangle * 640.0)

                if (objectTracker.hasObject) {
                    val landmarks = faceMesh.extractLandmarks(squareImage, objectTracker.objectSmoothSSDRectangle)
                    for (l in landmarks) {

                        drawer.fill = ColorRGBa.PINK
                        drawer.circle(l.position.xy, 2.0)
                    }
                }
            }
        }
    }
}
