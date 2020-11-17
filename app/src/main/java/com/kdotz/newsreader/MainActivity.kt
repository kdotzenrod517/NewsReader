package com.kdotz.newsreader

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteStatement
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        articlesDB = this.openOrCreateDatabase("Articles", Context.MODE_PRIVATE, null)

        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)")

        val task = DownloadTask()
        try {

//            task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty")

        } catch (e: Exception) {
            e.printStackTrace()
        }

        val listView = findViewById<ListView>(R.id.listView)
        arrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, titles)
        listView.adapter = arrayAdapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val intent = Intent(this, ArticleActivity::class.java)
            intent.putExtra("content", content[position])
            startActivity(intent)
        }

        updateListView()
    }


    class DownloadTask : AsyncTask<String, Void, String>() {

        override fun doInBackground(vararg urls: String?): String {

            var result = ""
            var url: URL
            var urlConnection: HttpURLConnection? = null

            try {
                url = URL(urls[0])

                urlConnection = url.openConnection() as HttpURLConnection

                var inputStream = urlConnection.inputStream

                var inputStreamReader = InputStreamReader(inputStream)

                var data = inputStreamReader.read()

                while (data != -1) {
                    val current = data.toChar()
                    result += current
                    data = inputStreamReader.read()
                }

                val jsonArray = JSONArray(result)

                var numberOfItems = 20

                if (jsonArray.length() < 20) {
                    numberOfItems = jsonArray.length()
                }

                articlesDB.execSQL("DELETE FROM articles")

                for (i in 0..numberOfItems) {

                    val articleId = jsonArray.getString(i)
                    url = URL("https://hacker-news.firebaseio.com/v0/item/$articleId.json?print=pretty")
                    urlConnection = url.openConnection() as HttpURLConnection

                    inputStream = urlConnection.inputStream
                    inputStreamReader = InputStreamReader(inputStream)

                    data = inputStreamReader.read()

                    var articleInfo = ""

                    while (data != -1) {
                        val current = data.toChar()
                        articleInfo += current
                        data = inputStreamReader.read()
                    }

                    val jsonObject = JSONObject(articleInfo)

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url") && !jsonObject.getString("title").contains("airbnb", ignoreCase = true)) {

                        val articleTitle = jsonObject.getString("title")
                        val articleUrl = jsonObject.getString("url")

                        url = URL(articleUrl)
                        urlConnection = url.openConnection() as HttpURLConnection
                        inputStream = urlConnection.inputStream
                        inputStreamReader = InputStreamReader(inputStream)
                        data = inputStreamReader.read()
                        var articleContent = ""
                        while (data != -1) {
                            val current = data.toChar()
                            articleContent += current
                            data = inputStreamReader.read()
                        }

                        Log.i("HTML", articleContent)

                        val sql = "INSERT INTO articles (articleId, title, content) VALUES (?, ?, ?)"

                        val statement: SQLiteStatement = articlesDB.compileStatement(sql)

                        statement.bindString(1, articleId)
                        statement.bindString(2, articleTitle)
                        statement.bindString(3, articleContent)

                        statement.execute()
                    }
                }

                Log.i("URL Content: ", result)
                return result

            } catch (e: Exception) {
                e.printStackTrace()
            }

            return ""
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            updateListView()
        }

    }

    companion object {
        lateinit var articlesDB: SQLiteDatabase
        var titles = mutableListOf<String>()
        var content = mutableListOf<String>()

        lateinit var arrayAdapter: ArrayAdapter<String>

        fun updateListView() {
            val c: Cursor = articlesDB.rawQuery("SELECT * FROM articles", null)
            val contentIndex = c.getColumnIndex("content")
            val titleIndex = c.getColumnIndex("title")

            if (c.moveToFirst()) {
                titles.clear()
                content.clear()

                do {
                    titles.add(c.getString(titleIndex))
                    content.add(c.getString(contentIndex))
                } while (c.moveToNext())

                arrayAdapter.notifyDataSetChanged()
            }
        }
    }

}
