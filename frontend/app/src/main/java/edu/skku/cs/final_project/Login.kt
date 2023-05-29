package edu.skku.cs.final_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class Login : AppCompatActivity() {
    companion object {
        const val USERNAME = "name"
    }
    data class Req(var username: String?, var password: String?)
    data class Res(var success: Boolean?, var username: String?)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val actionBar = supportActionBar
        actionBar?.hide()

        val url = "http://3.38.151.134:8000"

        val editTextId = findViewById<EditText>(R.id.editTextId)
        val editTextPwd = findViewById<EditText>(R.id.editTextPwd)

        val buttonSubmit = findViewById<Button>(R.id.buttonSubmit)
        buttonSubmit.setOnClickListener {
            val username = editTextId.text.toString()
            val password = editTextPwd.text.toString()

            val json = Gson().toJson(Req(username, password))

            val request = Request.Builder()
                .url("$url/login")
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
                        val data = Gson().fromJson(res, Res::class.java)
                        if (data.success == true) {
                            val intent = Intent(applicationContext, MainActivity::class.java).apply{
                                putExtra(USERNAME, data.username)
                            }
                            startActivity(intent)
                        } else {
                            runOnUiThread {
                                Toast.makeText(applicationContext, "로그인 실패", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            })
        }
    }
}