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
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import com.google.gson.Gson
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

fun base64ToBitmap(b64: String): Bitmap {
    val imageBytes = Base64.decode(b64, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
}
class DamageInfoAdapter(private val context: Context, private val dataSource: List<Report.DamageInfo>) : BaseAdapter() {

    private val inflater: LayoutInflater
        get() = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getCount(): Int = dataSource.size

    override fun getItem(position: Int): Any = dataSource[position]

    override fun getItemId(position: Int): Long = position.toLong()

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: ViewHolder

        if (convertView == null) {
            view = inflater.inflate(R.layout.list_item, parent, false)
            holder = ViewHolder()
            holder.tvPart = view.findViewById(R.id.textViewPart)
            holder.imgPart = view.findViewById(R.id.imageViewPart)
            holder.tvDamageInfo = view.findViewById(R.id.textViewDamageInfo)
            holder.tvRepairMethod = view.findViewById(R.id.textViewRepairMethod)
            holder.tvRepairCost = view.findViewById(R.id.textViewRepairCost)
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val damageInfo = getItem(position) as Report.DamageInfo

        holder.tvPart.text = "Part: ${damageInfo.part}"
        holder.imgPart.setImageBitmap(base64ToBitmap(damageInfo.part_img)) // base64ToBitmap 함수는 별도로 구현해야 함
        holder.tvDamageInfo.text = "Damage Info: ${damageInfo.damage_info.joinToString(", ")}"
        holder.tvRepairMethod.text = "Repair Method: ${damageInfo.repair_method}"
        holder.tvRepairCost.text = "Repair Cost: ${damageInfo.repair_cost}"

        return view
    }

    private class ViewHolder {
        lateinit var tvPart: TextView
        lateinit var imgPart: ImageView
        lateinit var tvDamageInfo: TextView
        lateinit var tvRepairMethod: TextView
        lateinit var tvRepairCost: TextView
    }
}

class Report : AppCompatActivity() {
    data class Req(var img_string: String?, var firstday: String?, var company: String?)
    data class CheckboxInfo(
        val disable: Boolean,
        val color: String
    )
    data class DamageInfo(
        val part: String,
        val part_img: String,
        val damage_mask: List<String>,
        val damage_info: List<String>,
        val checkbox_info: Map<String, CheckboxInfo>,
        val repair_method: String,
        val repair_cost: String
    )
    data class ApiResponse(
        val origImage: String,
        val part: List<String>,
        val info: List<DamageInfo>
    )
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