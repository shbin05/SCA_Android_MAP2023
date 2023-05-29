package edu.skku.cs.final_project

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.JsonReader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.google.gson.Gson
import com.squareup.picasso.Picasso
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.ByteArrayOutputStream
import java.io.IOException

object ImageHolder {
    var imgBitmap: Bitmap? = null
}

class Report : AppCompatActivity() {
    data class Req(var img_string: String?, var firstday: String?, var company: String?)
    data class DamageInfo(
        val part: String,
        val part_img: String,
        val damage_mask: List<String>,
        val damage_info: List<String>,
        val checkbox_info: Map<String, Boolean>,
        val repair_method: String,
        val repair_cost: String
    )
    data class ApiResponse(
        val origImage: String,
        val part: List<String>,
        val info: List<DamageInfo>
    )
    class DamageInfoAdapter(private val context: Context, private val damageInfoList: List<DamageInfo>) :
        ArrayAdapter<DamageInfo>(context, 0, damageInfoList) {
        @SuppressLint("SetTextI18n")
        override fun getView(i: Int, cvtView: View?, parent: ViewGroup): View {
            var view = cvtView
            if (view == null){
                view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
            }

            val currentDamageInfo = damageInfoList[i]

            val partTextView = view!!.findViewById(R.id.textViewPart) as TextView
            partTextView.text = currentDamageInfo.part

            val partImgView = view.findViewById(R.id.imageViewPart) as ImageView
            Picasso.get().load(currentDamageInfo.part_img).into(partImgView)

            val damageInfoTextView = view.findViewById(R.id.textViewDamage) as TextView
            damageInfoTextView.text = currentDamageInfo.damage_info.joinToString("\n")

            val repairMethodTextView = view.findViewById(R.id.textViewMethod) as TextView
            repairMethodTextView.text = "Repair method: "+ currentDamageInfo.repair_method

            val repairCostTextView = view.findViewById(R.id.textViewCost) as TextView
            repairCostTextView.text = "Cost: "+currentDamageInfo.repair_cost

            return view
        }
    }
    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        val actionBar = supportActionBar
        actionBar?.hide()

        val textViewLoading = findViewById<TextView>(R.id.textViewLoading)

        val username = intent.getStringExtra(MainActivity.USERNAME)
        val carName = intent.getStringExtra(MainActivity.CARNAME)
        val company = intent.getStringExtra(MainActivity.COMPANY)
        val carYear = intent.getStringExtra(MainActivity.CARYEAR)
        val imgBitmap = ImageHolder.imgBitmap
        val imgString = imgBitmap?.let { bitmapToBase64(it) }

        val json = Gson().toJson(Req(imgString, carYear, company))
        val url = "http://10.0.2.2:8000"
        val request = Request.Builder()
            .url("$url/api/main")
            .post(json.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
            .addHeader("Access-Control-Allow-Origin", "*")
            .addHeader("Access-Control-Allow-Headers", "*")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println(e)
            }
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = Gson().fromJson(response.body?.string(), ApiResponse::class.java)
                    response.close()
                    runOnUiThread {
                        textViewLoading.visibility = TextView.INVISIBLE

                        val listView: ListView = findViewById(R.id.listView)
                        listView.adapter = DamageInfoAdapter(this@Report, responseData.info)
                    }
                }
            }
        })
    }
}