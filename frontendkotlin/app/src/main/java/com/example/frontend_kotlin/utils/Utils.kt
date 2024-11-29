package com.example.frontend_kotlin.utils

import android.content.Context
import android.media.MediaPlayer
import android.os.AsyncTask
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class Utils {
    companion object {

        fun downloadAndPlayAudioFile(context: Context, url: String) {
            AsyncTask.execute {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connect()

                    val inputStream: InputStream = connection.inputStream
                    val outputFile = File(context.cacheDir, "downloaded_audio.mp3")
                    val outputStream = FileOutputStream(outputFile)

                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                    outputStream.close()
                    inputStream.close()

                    val mediaPlayer = MediaPlayer()
                    Log.d("MainActivity", "Playing audio from file: ${outputFile.absolutePath}")
                    Log.d("MainActivity", "File size: ${outputFile.length()} bytes")
                    mediaPlayer.setDataSource(outputFile.absolutePath)
                    mediaPlayer.prepare()
                    mediaPlayer.start()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        private fun playAudioFromUrl(audioUrl: String) {
            val mediaPlayer = MediaPlayer().apply {
                setDataSource(audioUrl)
                setOnPreparedListener { start() }
                setOnCompletionListener { release() }
                setOnErrorListener { mp, what, extra ->
                    Log.e("MainActivity", "Error playing audio: what=$what, extra=$extra")
                    mp.release()
                    true
                }
                prepareAsync()
            }
            mediaPlayer.setOnErrorListener { mp, what, extra ->
                Log.e("MainActivity", "Error playing audio: what=$what, extra=$extra")
                mp.release()
                true
            }
        }
    }
}