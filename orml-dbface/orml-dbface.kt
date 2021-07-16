import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.draw.colorBuffer
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.math.Vector2
import org.openrndr.orml.dbface.DBFaceDetector
import org.openrndr.shape.IntRectangle
import org.openrndr.shape.Rectangle

fun main() = application {
  
    configure {
        width = 640
        height = 480
    }
    
    
        program {
          
            val dbface = DBFaceDetector.load()
            val video = loadVideoDevice(width = width, height = height)
            video.play()

            val videoFrame = colorBuffer(video.width, video.height)
            video.newFrame.listen {
                it.frame.copyTo(videoFrame, targetRectangle = IntRectangle(0, videoFrame.height - (videoFrame.height - height) / 2, it.frame.width, -it.frame.height))
            }
            
            
            extend {
                video.draw(drawer, blind = true)
                val rectangles = dbface.detectFaces(videoFrame)
                drawer.fill = null
                drawer.stroke = ColorRGBa.PINK
                for (r in rectangles) {
                    drawer.rectangle(r.area * Vector2(width / 1.0, height / 1.0))
                    for (l in r.landmarks) {
                        drawer.circle(l.x * width / 1.0, l.y * height / 1.0, 3.0)
                    }
                }
            }
        }
    
}

private operator fun Rectangle.times(scale: Vector2): Rectangle {
    return Rectangle(x * scale.x, y * scale.y, width * scale.x, height * scale.y)
}
