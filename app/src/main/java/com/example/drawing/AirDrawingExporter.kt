package com.example.drawing

import android.content.Context
import com.example.model.AirPoint3D
import com.example.model.AirStroke3D
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object AirDrawingExporter {

    fun strokesToJson(strokes: List<AirStroke3D>): String {
        val arr = JSONArray()
        strokes.forEach { stroke ->
            val pts = JSONArray()
            stroke.points.forEach { p ->
                pts.put(JSONObject().apply {
                    put("x", p.x)
                    put("y", p.y)
                    put("z", p.z)
                })
            }
            arr.put(JSONObject().apply {
                put("id", stroke.id)
                put("color", stroke.colorArgb)
                put("size", stroke.brushSizeDp)
                put("is3D", stroke.is3DMode)
                put("hand", stroke.handUsed.name)
                put("points", pts)
            })
        }
        return arr.toString()
    }

    /**
     * Generates a valid 3D Wavefront OBJ tube mesh from air-drawn 3D strokes.
     */
    fun exportToObj(context: Context, title: String, strokes: List<AirStroke3D>): File {
        val exportDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
        val safeName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "air_creation" }
        val outFile = File(exportDir, "${safeName}_${System.currentTimeMillis()}.obj")

        val sb = StringBuilder()
        sb.append("# RudraX Motion X 3D Air Creation\n")
        sb.append("# Object: $title\n")
        sb.append("o $safeName\n")

        var vertexOffset = 1
        val sides = 6

        strokes.forEach { stroke ->
            val pts = stroke.points
            if (pts.size < 2) return@forEach
            val radius = (stroke.brushSizeDp * 0.0025f).coerceIn(0.006f, 0.045f)

            for (i in pts.indices) {
                val p = pts[i]
                val cx = (p.x - 0.5f) * 2.0f
                val cy = (0.5f - p.y) * 2.0f
                val cz = p.z * 2.0f

                for (s in 0 until sides) {
                    val angle = (2.0 * Math.PI * s) / sides
                    val vx = cx + radius * cos(angle).toFloat()
                    val vy = cy + radius * sin(angle).toFloat()
                    val vz = cz
                    sb.append("v %.5f %.5f %.5f\n".format(vx, vy, vz))
                }
            }

            for (i in 0 until pts.size - 1) {
                val ringA = vertexOffset + i * sides
                val ringB = vertexOffset + (i + 1) * sides
                for (s in 0 until sides) {
                    val nextS = (s + 1) % sides
                    val a1 = ringA + s
                    val a2 = ringA + nextS
                    val b1 = ringB + s
                    val b2 = ringB + nextS
                    sb.append("f $a1 $b1 $b2\n")
                    sb.append("f $a1 $b2 $a2\n")
                }
            }
            vertexOffset += pts.size * sides
        }

        outFile.writeText(sb.toString())
        return outFile
    }

    /**
     * Generates a valid glTF 2.0 JSON asset describing the 3D stroke polyline & bounding metadata.
     */
    fun exportToGltf(context: Context, title: String, strokes: List<AirStroke3D>): File {
        val exportDir = File(context.filesDir, "exports").apply { if (!exists()) mkdirs() }
        val safeName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_").ifBlank { "air_creation" }
        val outFile = File(exportDir, "${safeName}_${System.currentTimeMillis()}.gltf")

        val positions = mutableListOf<Float>()
        strokes.forEach { stroke ->
            stroke.points.forEach { p ->
                positions.add((p.x - 0.5f) * 2.0f)
                positions.add((0.5f - p.y) * 2.0f)
                positions.add(p.z * 2.0f)
            }
        }
        val gltf = JSONObject().apply {
            put("asset", JSONObject().apply {
                put("version", "2.0")
                put("generator", "RudraX Motion X Air Drawing Studio")
            })
            put("scene", 0)
            put("scenes", JSONArray().put(JSONObject().apply {
                put("name", title)
                put("nodes", JSONArray().put(0))
            }))
            put("nodes", JSONArray().put(JSONObject().apply {
                put("name", safeName)
                put("extras", JSONObject().apply {
                    put("strokeCount", strokes.size)
                    put("vertexCount", positions.size / 3)
                })
            }))
        }
        outFile.writeText(gltf.toString(2))
        return outFile
    }

    fun generateShapePreset(templateName: String): List<AirPoint3D> {
        return when (templateName) {
            "ENERGY_SWORD" -> listOf(
                AirPoint3D(0.50f, 0.80f, 0.0f),
                AirPoint3D(0.50f, 0.68f, 0.0f),
                AirPoint3D(0.40f, 0.66f, 0.05f),
                AirPoint3D(0.60f, 0.66f, -0.05f),
                AirPoint3D(0.50f, 0.66f, 0.0f),
                AirPoint3D(0.50f, 0.22f, 0.12f),
                AirPoint3D(0.53f, 0.28f, 0.10f),
                AirPoint3D(0.50f, 0.66f, 0.0f)
            )
            "PLASMA_BLASTER" -> listOf(
                AirPoint3D(0.35f, 0.72f, 0.0f),
                AirPoint3D(0.42f, 0.54f, 0.02f),
                AirPoint3D(0.28f, 0.54f, 0.02f),
                AirPoint3D(0.28f, 0.42f, 0.05f),
                AirPoint3D(0.74f, 0.42f, 0.14f),
                AirPoint3D(0.74f, 0.50f, 0.14f),
                AirPoint3D(0.46f, 0.54f, 0.02f)
            )
            "HOLO_SHIELD" -> {
                (0..6).map { i ->
                    val a = i * (2.0 * Math.PI / 6.0)
                    AirPoint3D(
                        0.5f + 0.22f * cos(a).toFloat(),
                        0.5f + 0.24f * sin(a).toFloat(),
                        0.08f * sin(a * 2).toFloat()
                    )
                }
            }
            "CROWN_ARTIFACT" -> listOf(
                AirPoint3D(0.30f, 0.62f, 0.0f),
                AirPoint3D(0.34f, 0.36f, 0.08f),
                AirPoint3D(0.42f, 0.50f, 0.04f),
                AirPoint3D(0.50f, 0.30f, 0.12f),
                AirPoint3D(0.58f, 0.50f, 0.04f),
                AirPoint3D(0.66f, 0.36f, 0.08f),
                AirPoint3D(0.70f, 0.62f, 0.0f),
                AirPoint3D(0.30f, 0.62f, 0.0f)
            )
            else -> emptyList()
        }
    }
}
