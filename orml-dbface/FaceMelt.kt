import org.openrndr.application
import org.openrndr.color.ColorRGBa
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

            val effectLayer = renderTarget(width, height) {
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

            val box = boxMesh()

            extend {

                val c = compose {

                    video.draw(drawer, blind = true)
                    val rectangles = blazeFace.detectFaces(squareImage)
                    val mask =  layer {
                        draw {
                            for (rectangle in rectangles) {

                                drawer.fill = ColorRGBa.BLACK
                                drawer.stroke = null

                                val landmarks = faceMesh.extractLandmarks(squareImage, rectangle)
                                val pose = faceMesh.extractPose(landmarks)

                                poseSmooth = poseSmooth * 0.9 + pose * 0.1

                                val contour = landmarks.contourOf(faceSilhouette)

                                contourRt.clearColor(0, ColorRGBa.TRANSPARENT)
                                drawer.isolatedWithTarget(contourRt) {

                                    drawer.contour(contour)

                                }

                                drawer.image(contourRt.colorBuffer(0), width / 1.0, 0.0, -width / 1.0, height / 1.0 )


                                drawer.isolatedWithTarget(effectLayer) {
                                    drawer.clear(ColorRGBa.TRANSPARENT)
                                    drawer.defaults()
                                    drawer.depthWrite = true
                                    drawer.depthTestPass = DepthTestPass.LESS_OR_EQUAL

                                    val pose = poseSmooth
                                    val t = pose[3].xy.xy0 / (squareImage.width.toDouble()/2.0) - Vector3(1.0, 1.0, 0.0)
                                    val persp = Matrix44.translate(t * Vector3(-1.0, -1.0, 1.0))  * org.openrndr.math.transforms.perspective(90.0, 1.0, 0.1, 100.0)
                                    drawer.projection = persp
                                    drawer.model = Matrix44.translate(0.0, 0.0, -20.0) *
                                            Matrix44.fromColumnVectors(Vector4.UNIT_X * 1.0, Vector4.UNIT_Y, Vector4.UNIT_Z, Vector4.UNIT_W) * Matrix44.fromColumnVectors(pose[0], pose[1], pose[2], Vector4.UNIT_W) *
                                            Matrix44.rotateY(45.0)

                                    drawer.shadeStyle = shadeStyle {
                                        fragmentTransform = """
                                             x_fill.rgb = vec3(1, 1, 1);
                                        """.trimIndent()
                                    }
                                    drawer.vertexBuffer(box, DrawPrimitive.LINES)


                                }
                                drawer.image(effectLayer.colorBuffer(0))

                            }
                        }

                    }
                    layer {

                        use(mask)
                        blend(Add()) {
                            clip = true
                        }
                        layer {

                            drawer.stroke = ColorRGBa.PINK
                            use(mask)

                            post (Perturb()) {
                                phase = seconds * 0.004 * 0.5 // * 0.25 * 0.5
                            }
                            post(PoissonFill())
                            post (Perturb()) {
                                phase = seconds * 0.05
                                scale = 1.429
                                gain = 0.1
                                decay = 0.341 + cos(seconds) * 0.05
                            }
                            layer {
                                blend(Add()) {
                                    clip = true
                                }

                                draw {

                                    drawer.image(squareImage, squareImage.width*1.0, 0.0, -squareImage.width*1.0, squareImage.height*1.0)
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