package edu.skku.cs.final_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isInvisible
import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.w3c.dom.Text
import java.io.IOException

class Register : AppCompatActivity() {
    data class Config(val username: String?)
    data class Req(var username: String?, var password: String?, var carName: String?, var company: String?, var carYear: String?)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val actionBar = supportActionBar
        actionBar?.hide()

        val url = "http://3.38.151.134:8000"

        val editTextUsername = findViewById<TextView>(R.id.editTextUsername)
        val editTextPwd = findViewById<TextView>(R.id.editTextPwd)
        val editTextConfig = findViewById<TextView>(R.id.editTextConfig)

        val editTextCarName = findViewById<EditText>(R.id.editTextCarName)
        val editTextCompany = findViewById<EditText>(R.id.editTextCompany)
        val editTextCarYear = findViewById<EditText>(R.id.editTextCarYear)

        val textViewConfig = findViewById<TextView>(R.id.textViewConfig)
        textViewConfig.visibility = TextView.INVISIBLE

        var configTF: Boolean = false

        val buttonConfig = findViewById<Button>(R.id.buttonConfig)
        buttonConfig.setOnClickListener {
            val username = editTextUsername.text.toString()
            val json = Gson().toJson(Config(username))

            val request = Request.Builder()
                .url("$url/config")
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
                        println(res)
                        if(res.contains("success")){
                            configTF = true
                            runOnUiThread{
                                textViewConfig.visibility = TextView.VISIBLE
                                textViewConfig.text = "사용 가능한 아이디입니다!"
                                textViewConfig.setTextColor(getColor(R.color.green))
                            }
                        }else if(res.contains("fail")){
                            configTF = false
                            runOnUiThread{
                                textViewConfig.visibility = TextView.VISIBLE
                                textViewConfig.text="사용 불가한 아이디입니다!"
                                textViewConfig.setTextColor(getColor(R.color.red))
                            }
                        }
                    }
                }
            })
        }

        val buttonSubmit = findViewById<Button>(R.id.buttonSubmit)
        buttonSubmit.setOnClickListener {
            val username = editTextUsername.text.toString()
            val password = editTextPwd.text.toString()
            val config = editTextConfig.text.toString()

            val carName = editTextCarName.text.toString()
            val company = editTextCompany.text.toString()
            val carYear = editTextCarYear.text.toString()

            if(!configTF){
                Toast.makeText(applicationContext, "아이디 확인을 진행해주세요 !", Toast.LENGTH_SHORT).show()
            }
            else if(password != config){
                Toast.makeText(applicationContext, "비밀번호를 확인해주세요 !", Toast.LENGTH_SHORT).show()
            }
            else{
                val json = Gson().toJson(Req(username, password, carName, company, carYear))
                val request = Request.Builder()
                    .url("$url/register")
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
                                val intent = Intent(applicationContext, Login::class.java)
                                startActivity(intent)
                            }else{
                                Toast.makeText(applicationContext, "알 수 없는 오류 발생", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                })
            }
        }
    }
}