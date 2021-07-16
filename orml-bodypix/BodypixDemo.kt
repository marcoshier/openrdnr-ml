import org.openrndr.Program
import org.openrndr.application
import org.openrndr.draw.*
import org.openrndr.extra.fx.color.ColorCorrection
import org.openrndr.extra.tensorflow.copyTo
import org.openrndr.ffmpeg.PlayMode
import org.openrndr.ffmpeg.VideoPlayerFFMPEG
import org.openrndr.ffmpeg.loadVideo
import org.openrndr.orml.bodypix.BodyPix
import org.openrndr.orml.bodypix.BodyPixArchitecture
import org.openrndr.shape.IntRectangle
import org.tensorflow.ndarray.Shape
import org.tensorflow.types.TFloat32
import kotlin.math.sqrt

fun main() = application {


    configure {
        width = 1080/2
        height = 1920/2
    }
    
    
    program {
    
    
        val bodyPix = BodyPix.load(architecture = BodyPixArchitecture.MOBILENET)
        val inputTensor = TFloat32.tensorOf(Shape.of(1, 1920/2, 1080/2, 3))

        val video = loadVideo("demo-data/webcam.mp4", PlayMode.VIDEO)
        video.play()

        val segW = 34
        val segH = 60
        val segRatio = sqrt((width / 1.0 * height / 1.0) / (segW * segH))

        val segmentationImage = colorBuffer(segW, segH, type = ColorType.FLOAT32, format = ColorFormat.R)
        val segmentationFlipped =colorBuffer(segW, segH, type = ColorType.FLOAT32, format = ColorFormat.R)
        val downScaleVideo = renderTarget(width, height) {
            colorBuffer(type = ColorType.FLOAT32, format = ColorFormat.RGB)
        }
        val inputFlipped = colorBuffer(1080/2, 1920/2, type = ColorType.FLOAT32, format = ColorFormat.RGB)
        val colorCorr = ColorCorrection()
        val corrected = colorBuffer(width, height, format = ColorFormat.RGB)

        
        extend {
          
            drawer.isolatedWithTarget(downScaleVideo) {
                drawer.ortho(downScaleVideo)
                fitVideo(video)
            }
            
            //drawer.image(downScaleVideo.colorBuffer(0))
            downScaleVideo.colorBuffer(0).copyTo(inputFlipped, targetRectangle = IntRectangle(0, 1920/2, 1080/2, -1920/2 ))
            inputFlipped.copyTo(inputTensor)

            val result = bodyPix.infer(inputTensor)
            result.segmentation.copyTo(segmentationImage)
            segmentationImage.copyTo(segmentationFlipped, targetRectangle = IntRectangle(0, segmentationImage.height, segmentationImage.width, -segmentationImage.height))

            drawer.scale(segRatio)

            colorCorr.hueShift = 140.0
            colorCorr.saturation = 25.0
            colorCorr.apply(segmentationFlipped, corrected)
            drawer.defaults()

            drawer.image(corrected)


        }
    }
}


private fun Program.fitVideo(video: VideoPlayerFFMPEG) {

    val ratio = video.width.toDouble() / video.height.toDouble()
    val scaledWidth = ratio * height / 1.0
    video.draw(drawer, (width - scaledWidth) / 2.0, 0.0, scaledWidth, height / 1.0)

}
