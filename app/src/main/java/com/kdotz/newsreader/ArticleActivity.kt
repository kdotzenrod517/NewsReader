package com.kdotz.newsreader

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.webkit.WebView
import android.webkit.WebViewClient

class ArticleActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article)

        val webView = findViewById<WebView>(R.id.webView)

        webView.settings.setAppCacheEnabled(true)
        webView.webViewClient = WebViewClient()

        val intent = intent

        webView.loadData(intent.getStringExtra("content"), "text/html", "UTF-8")
    }
}
