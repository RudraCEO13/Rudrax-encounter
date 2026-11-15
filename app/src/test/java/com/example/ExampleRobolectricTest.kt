package com.example

import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.connectivity.MotionXStreamingServer
import com.example.connectivity.QrCodeUtils
import com.example.drawing.AirDrawingExporter
import com.example.model.AirPoint3D
import com.example.model.AirStroke3D
import com.example.model.GestureType
import com.example.model.HandSide
import com.example.model.NormalizedLandmarkPoint
import com.example.runtime.GameStorageManager
import com.example.vision.GestureRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private fun buildExtendedHandLandmarks(centerX: Float = 0.5f, centerY: Float = 0.5f): List<NormalizedLandmarkPoint> {
        val pts = MutableList(21) { NormalizedLandmarkPoint(it, centerX, centerY, 0f) }
        pts[0] = NormalizedLandmarkPoint(0, centerX, centerY + 0.2f, 0f)
        // Thumb extended
        pts[1] = NormalizedLandmarkPoint(1, centerX - 0.05f, centerY + 0.14f, 0f)
        pts[2] = NormalizedLandmarkPoint(2, centerX - 0.09f, centerY + 0.08f, 0f)
        pts[3] = NormalizedLandmarkPoint(3, centerX - 0.12f, centerY + 0.03f, 0f)
        pts[4] = NormalizedLandmarkPoint(4, centerX - 0.15f, centerY - 0.02f, 0f)
        // Index, Middle, Ring, Pinky straight upward
        listOf(5 to -0.06f, 9 to -0.02f, 13 to 0.02f, 17 to 0.06f).forEach { (base, dx) ->
            pts[base] = NormalizedLandmarkPoint(base, centerX + dx, centerY + 0.04f, 0f)
            pts[base + 1] = NormalizedLandmarkPoint(base + 1, centerX + dx, centerY - 0.04f, 0f)
            pts[base + 2] = NormalizedLandmarkPoint(base + 2, centerX + dx, centerY - 0.10f, 0f)
            pts[base + 3] = NormalizedLandmarkPoint(base + 3, centerX + dx, centerY - 0.16f, 0f)
        }
        return pts
    }

    @Test
    fun `app name matches RudraX Motion X`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("RudraX Motion X", context.getString(R.string.app_name))
    }

    @Test
    fun `dual hand 42 landmark analysis detects open palm pinch and inter-hand distance`() {
        val leftPts = buildExtendedHandLandmarks(centerX = 0.25f)
        val rightPts = buildExtendedHandLandmarks(centerX = 0.75f).toMutableList()

        val left = GestureRecognizer.analyzeHand(HandSide.LEFT, leftPts, null, 0.95f, 0.5f)
        assertTrue(left.isOpenPalm)
        assertEquals(21, left.landmarks.size)

        // Pinch thumb tip (4) to index tip (8)
        rightPts[4] = NormalizedLandmarkPoint(4, 0.73f, 0.40f, 0f)
        rightPts[8] = NormalizedLandmarkPoint(8, 0.735f, 0.405f, 0f)
        val right = GestureRecognizer.analyzeHand(HandSide.RIGHT, rightPts, null, 0.96f, 0.5f)
        assertTrue(right.isPinching)
        assertEquals(GestureType.PINCH, right.activeGesture)

        val dist = GestureRecognizer.computeHandsDistance(left, right)
        assertNotNull(dist)
        assertTrue(dist!! > 0.4f)
    }

    @Test
    fun `custom ZIP game upload extracts preserves files and blocks zip slip`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = GameStorageManager(context)

        val zipFile = File(context.cacheDir, "test_custom_game.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("index.html"))
            zos.write("<html><body><h1>My Uploaded Game</h1></body></html>".toByteArray())
            zos.closeEntry()
            zos.putNextEntry(ZipEntry("game.js"))
            zos.write("console.log('ready');".toByteArray())
            zos.closeEntry()
        }

        val result = storage.importGameFromUris(listOf(Uri.fromFile(zipFile)))
        assertTrue(result.isSuccess)
        val project = result.getOrThrow()
        assertEquals("index.html", project.entryPoint)
        assertEquals(2, project.files.size)
        assertNotNull(storage.resolveGameFile(project.id, "index.html"))

        // Zip Slip rejection test
        val maliciousZip = File(context.cacheDir, "bad_game.zip")
        ZipOutputStream(FileOutputStream(maliciousZip)).use { zos ->
            zos.putNextEntry(ZipEntry("../../evil.html"))
            zos.write("bad".toByteArray())
            zos.closeEntry()
        }
        val badResult = storage.importGameFromUris(listOf(Uri.fromFile(maliciousZip)))
        assertFalse(badResult.isSuccess)
    }

    @Test
    fun `streaming server serves uploaded game and MotionX JS SDK over HTTP`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val storage = GameStorageManager(context)
        val demo = storage.installCyberArenaTemplate()
        val server = MotionXStreamingServer(storage)

        server.start(httpPort = 0, wsPort = 8799, pin = "123456")
        Thread.sleep(300)

        val gameUrl = server.getLoopbackGameUrl(demo.id, demo.entryPoint)
        val conn = URL(gameUrl).openConnection() as HttpURLConnection
        assertEquals(200, conn.responseCode)
        val html = conn.inputStream.bufferedReader().readText()
        assertTrue(html.contains("Cyber Motion Arena"))

        val sdkUrl = "http://127.0.0.1:${server.getHttpPort()}/sdk/motionx-sdk.js"
        val sdkConn = URL(sdkUrl).openConnection() as HttpURLConnection
        assertEquals(200, sdkConn.responseCode)
        val sdkJs = sdkConn.inputStream.bufferedReader().readText()
        assertTrue(sdkJs.contains("onHandMove"))

        server.stop()
    }

    @Test
    fun `air drawing exports valid OBJ and glTF files and QR pairing parses cleanly`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val stroke = AirStroke3D(
            id = 1L,
            points = listOf(AirPoint3D(0.2f, 0.2f, 0f), AirPoint3D(0.8f, 0.7f, 0.1f)),
            colorArgb = 0xFF00F0FF,
            brushSizeDp = 8f,
            is3DMode = true,
            handUsed = HandSide.RIGHT
        )
        val objFile = AirDrawingExporter.exportToObj(context, "Plasma Sword", listOf(stroke))
        val gltfFile = AirDrawingExporter.exportToGltf(context, "Plasma Sword", listOf(stroke))
        assertTrue(objFile.exists() && objFile.readText().contains("v "))
        assertTrue(gltfFile.exists() && gltfFile.readText().contains("\"version\": \"2.0\""))

        val bitmap = QrCodeUtils.generateQrBitmap("motionx://pair?host=192.168.1.50&port=8765&pin=654321&name=LivingRoomTV", 128)
        assertNotNull(bitmap)
        val parsed = QrCodeUtils.parsePairingPayload("motionx://pair?host=192.168.1.50&port=8765&pin=654321&name=LivingRoomTV")
        assertNotNull(parsed)
        assertEquals("LivingRoomTV", parsed!!.first.name)
        assertEquals("654321", parsed.second)
    }
}
