package com.example.frontend_kotlin.utils


import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class Utils {
    companion object {

        fun playAudioFromUrl(context: Context, url: String) {
            val executor = Executors.newSingleThreadExecutor()
            executor.execute {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty("Cache-Control", "no-cache")
                    connection.setRequestProperty("Connection", "close")
                    connection.connect()

                    val responseCode = connection.responseCode
                    val contentLength = connection.contentLength
                    val contentType = connection.contentType
                    Log.d("Utils", "Response Code: $responseCode, Content-Length: $contentLength, Content-Type: $contentType")

                    if (responseCode != 200 || contentLength <= 0 || !contentType.startsWith("audio/")) {
                        throw IOException("Invalid server response")
                    }

                    val inputStream: InputStream = connection.inputStream
                    val outputFile = File(context.cacheDir, "downloaded_audio.mp3")
                    val outputStream = FileOutputStream(outputFile)

                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    var totalBytesRead = 0
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                    }
                    Log.d("Utils", "Download complete. Total bytes read: $totalBytesRead")
                    outputStream.close()
                    inputStream.close()

                    if (totalBytesRead == 0) {
                        throw IOException("Downloaded file is empty")
                    }

                    val mediaPlayer = MediaPlayer()
                    mediaPlayer.setDataSource(outputFile.absolutePath)
                    mediaPlayer.prepare()
                    mediaPlayer.start()

                    mediaPlayer.setOnCompletionListener {
                        mediaPlayer.release()
                    }
                    mediaPlayer.setOnErrorListener { _, _, _ ->
                        mediaPlayer.release()
                        true
                    }

                } catch (e: Exception) {
                    Log.e("Utils", "Error: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }
}