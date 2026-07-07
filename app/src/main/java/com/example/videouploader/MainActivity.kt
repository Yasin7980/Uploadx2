package com.example.videouploader

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private var pendingUrl: String = ""

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val cookies = result.data?.getStringExtra("cookies")
        lifecycleScope.launch {
            try {
                val file = downloadFileWithAuth(pendingUrl, cookies)
                val resultUrl = uploadToFileIO(file)
                findViewById<TextView>(R.id.tvResultLink).text = resultUrl
                findViewById<TextView>(R.id.tvStatus).text = "تمام شد ✅"
            } catch (e: Exception) {
                findViewById<TextView>(R.id.tvStatus).text = "خطا: ${e.message}"
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val etLink = findViewById<EditText>(R.id.etLink)
        val btnStart = findViewById<Button>(R.id.btnStart)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvResult = findViewById<TextView>(R.id.tvResultLink)

        btnStart.setOnClickListener {
            val url = etLink.text.toString().trim()
            if (url.isEmpty()) {
                Toast.makeText(this, "لینک را وارد کنید", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            pendingUrl = url

            lifecycleScope.launch {
                try {
                    tvStatus.text = "در حال دانلود..."
                    val file = downloadFile(url)
                    tvStatus.text = "در حال آپلود..."
                    val resultUrl = uploadToFileIO(file)
                    tvStatus.text = "تمام شد ✅"
                    tvResult.text = resultUrl
                } catch (e: Exception) {
                    tvStatus.text = "نیاز به لاگین دارد، در حال باز کردن..."
                    val intent = Intent(this@MainActivity, LoginActivity::class.java)
                    intent.putExtra("url", url)
                    loginLauncher.launch(intent)
                }
            }
        }
    }

    private suspend fun downloadFile(url: String): File = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(url).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw Exception("دانلود ناموفق بود")

        val outFile = File(cacheDir, "video_${System.currentTimeMillis()}.mp4")
        response.body?.byteStream()?.use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
        }
        outFile
    }

    private suspend fun downloadFileWithAuth(url: String, cookies: String?): File =
        withContext(Dispatchers.IO) {
            val requestBuilder = Request.Builder().url(url)
            if (!cookies.isNullOrEmpty()) {
                requestBuilder.addHeader("Cookie", cookies)
            }
            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) throw Exception("دانلود ناموفق بود (کد ${response.code})")

            val outFile = File(cacheDir, "video_${System.currentTimeMillis()}.mp4")
            response.body?.byteStream()?.use { input ->
                FileOutputStream(outFile).use { output -> input.copyTo(output) }
            }
            outFile
        }

    private suspend fun uploadToFileIO(file: File): String = withContext(Dispatchers.IO) {
        val mediaType = "video/mp4".toMediaTypeOrNull()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, RequestBody.create(mediaType, file))
            .build()

        val request = Request.Builder()
            .url("https://file.io")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val json = JSONObject(response.body?.string() ?: "{}")
        if (json.getBoolean("success")) {
            json.getString("link")
        } else {
            throw Exception("آپلود ناموفق بود")
        }
    }
}
