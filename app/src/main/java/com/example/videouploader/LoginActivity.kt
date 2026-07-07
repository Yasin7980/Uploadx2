package com.example.videouploader

import android.content.Intent
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val targetUrl = intent.getStringExtra("url") ?: return
        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                val cookies = CookieManager.getInstance().getCookie(url)
                if (cookies != null && cookies.contains("session")) {
                    val resultIntent = Intent()
                    resultIntent.putExtra("cookies", cookies)
                    setResult(RESULT_OK, resultIntent)
                    finish()
                }
            }
        }
        webView.loadUrl(targetUrl)
    }
}
