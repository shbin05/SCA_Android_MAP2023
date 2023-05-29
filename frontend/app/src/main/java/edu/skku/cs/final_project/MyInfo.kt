package edu.skku.cs.final_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
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
    companion object {
        const val USERNAME = "name"
    }
    data class Req1(var username: String?)
    data class Res1(var success: Boolean?, var password: String?, var carName: String?, var company: String?, var carYear: String?)
    data class Req2(var username: String?, var password: String?, var carName: String?, var company: String?, var carYear: String?)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_info)

        val actionBar = supportActionBar
        actionBar?.hide()

        val url = "http://3.38.151.134:8000"

        val username = intent.getStringExtra(MainActivity.USERNAME)

        val textViewUsername = findViewById<TextView>(R.id.textViewUsername)
        textViewUsername.text = "아이디:  $username"

        val editTextPwd = findViewById<EditText>(R.id.editTextPwd)
        val editTextCarName = findViewById<EditText>(R.id.editTextCarName)
        val editTextCompany = findViewById<EditText>(R.id.editTextCompany)
        val editTextCarYear = findViewById<EditText>(R.id.editTextCarYear)

        val json = Gson().toJson(Req1(username))

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
                    val data = Gson().fromJson(res, Res1::class.java)

                    runOnUiThread {
                        editTextPwd.setText(data.password.toString())
                        editTextCarName.setText(data.carName.toString())
                        editTextCompany.setText(data.company.toString())
                        editTextCarYear.setText(data.carYear.toString())
                    }
                }
            }
        })

        val buttonSubmit = findViewById<Button>(R.id.buttonSubmit)
        buttonSubmit.setOnClickListener {
            val password = editTextPwd.text.toString()
            val carName = editTextCarName.text.toString()
            val company = editTextCompany.text.toString()
            val carYear = editTextCarYear.text.toString()

            val json = Gson().toJson(Req2(username, password, carName, company, carYear))

            val request = Request.Builder()
                .url("$url/changeinfo")
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

                        if(res.contains("success")){
                            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                                putExtra(USERNAME, username)
                            }
                            startActivity(intent)
                        }
                    }
                }
            })
        }
    }
}