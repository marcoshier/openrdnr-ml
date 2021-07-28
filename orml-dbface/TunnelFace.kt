import org.openrndr.application
import org.openrndr.color.ColorRGBa
import org.openrndr.color.rgb
import org.openrndr.draw.*
import org.openrndr.ffmpeg.loadVideoDevice
import org.openrndr.math.Matrix44
import org.openrndr.orml.facemesh.*
import org.openrndr.shape.IntRectangle
import org.openrndr.extra.compositor.*
import org.openrndr.extra.fx.blend.Add
import org.openrndr.extra.fx.distort.Perturb
import org.openrndr.extra.gui.GUI
import org.openrndr.extra.parameters.DoubleParameter
import org.openrndr.extras.meshgenerators.boxMesh
import org.openrndr.math.Vector3
import org.openrndr.math.Vector4
import org.openrndr.math.transforms.rotateY
import org.openrndr.math.transforms.translate
import org.openrndr.poissonfill.PoissonFill
import kotlin.math.cos

fun main() {
    application {
        configure {
            width = 640
            height = 640
        }
        program {

            val video = loadVideoDevice(width = 640, height = 480)
            video.play()

            val contourRt = renderTarget(width, height) {
                depthBuffer()
                colorBuffer()
            }

            val squareImage = colorBuffer(640, 640)

            video.newFrame.listen {
                it.frame.copyTo(squareImage, targetRectangle = IntRectangle(0, squareImage.height - (squareImage.height - 480) / 2, it.frame.width, -it.frame.height))
            }

            val blazeFace = BlazeFace.load()
            val faceMesh = FaceMesh.load()

            var poseSmooth = Matrix44.IDENTITY
            val final = renderTarget(width, height) {
                colorBuffer()

            }



            extend {

                val c = compose {

                    video.draw(drawer, blind = true)
                    val rectangles = blazeFace.detectFaces(squareImage)
                    val mask =  layer {
                        draw {
                            for (rectangle in rectangles) {

                                drawer.fill = ColorRGBa.BLACK

                                val landmarks = faceMesh.extractLandmarks(squareImage, rectangle)
                                val pose = faceMesh.extractPose(landmarks)

                                poseSmooth = poseSmooth * 0.9 + pose * 0.1

                                val contour = landmarks.contourOf(faceSilhouette)

                                contourRt.clearColor(0, ColorRGBa.TRANSPARENT)
                                drawer.isolatedWithTarget(contourRt) {

                                    drawer.contour(contour)

                                }

                                drawer.image(contourRt.colorBuffer(0), width / 1.0, 0.0, -width / 1.0, height / 1.0 )

                            }
                        }

                    }
                    layer {


                        use(mask)
                        blend(Add()) {
                            clip = true
                        }
                        layer {

                            layer {
                                draw {
                                    for (rectangle in rectangles) {

                                        val landmarks = faceMesh.extractLandmarks(squareImage, rectangle)
                                        val pose = faceMesh.extractPose(landmarks)

                                        poseSmooth = poseSmooth * 0.9 + pose * 0.1

                                        val contour = landmarks.contourOf(faceSilhouette)

                                        contourRt.clearColor(0, ColorRGBa.TRANSPARENT)

                                        final.clearColor(0, ColorRGBa.BLACK)
                                        for (j in 1 until 10) {


                                            drawer.defaults()
                                            drawer.translate(width / 2.0 - rectangle.area.width / 2.0, height / 2.0 - rectangle.area.height / 2.0)
                                            drawer.scale(0.1 * j)
                                            drawer.translate(-width / 2.0 + rectangle.area.width / 2.0, -height / 2.0 + rectangle.area.height / 2.0)
                                            //drawer.defaults()
                                            drawer.isolatedWithTarget(contourRt) {

                                                drawer.fill = null
                                                drawer.stroke = ColorRGBa.GREEN
                                                drawer.contour(contour)

                                            }

                                            val target = IntRectangle(width, 0, -width , height)
                                            contourRt.colorBuffer(0).copyTo(final.colorBuffer(0), targetRectangle = target)
                                            drawer.image(final.colorBuffer(0))

                                        }


                                    }
                                }

                            }


                        }

                    }








                }

                c.draw(drawer)


            }
        }
    }
}