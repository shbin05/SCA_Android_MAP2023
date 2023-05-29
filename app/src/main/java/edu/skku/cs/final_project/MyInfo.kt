package edu.skku.cs.final_project

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.google.gson.Gson
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class MyInfo : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_info)

        val actionBar = supportActionBar
        actionBar?.hide()

        val url = "http://3.38.151.134:8000"

        val username = intent.getStringExtra(MainActivity.USERNAME)

        val textViewUsername = findViewById<TextView>(R.id.textViewUsername)
        textViewUsername.text = "아이디: $username"

        val json = Gson().toJson(Register.Config(username))

        val request = Request.Builder()
            .url("$url/userinfo")
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addHeader("Content-type", "application/json")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println(e)
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val res = response.body!!.string()
                    
                }
            }
        })
    }
}