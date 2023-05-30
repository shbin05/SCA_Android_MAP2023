package edu.skku.cs.final_project

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Base64
import android.util.JsonReader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
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
    companion object{
        const val USERNAME = "name"
    }
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

            val textViewPart = view!!.findViewById<TextView>(R.id.textViewPart)
            textViewPart.text = currentDamageInfo.part

            val imageViewPart = view.findViewById<ImageView>(R.id.imageViewPart)
            Picasso.get().load(currentDamageInfo.part_img).into(imageViewPart)

            val checkboxBreakage = view.findViewById<CheckBox>(R.id.checkBoxBreakage)
            val imageViewBreakage = view.findViewById<ImageView>(R.id.imageViewBreakage)
            Picasso.get().load(currentDamageInfo.damage_mask[0]).into(imageViewBreakage)
            imageViewBreakage.visibility = ImageView.INVISIBLE

            checkboxBreakage.setOnCheckedChangeListener { _, isChecked ->
                imageViewBreakage.visibility = if (isChecked) ImageView.VISIBLE else ImageView.INVISIBLE
            }

            val checkBoxCrushed = view.findViewById<CheckBox>(R.id.checkBoxCrushed)
            val imageViewCrushed = view.findViewById<ImageView>(R.id.imageViewCrushed)
            Picasso.get().load(currentDamageInfo.damage_mask[1]).into(imageViewCrushed)
            imageViewCrushed.visibility = ImageView.INVISIBLE

            checkBoxCrushed.setOnCheckedChangeListener { _, isChecked ->
                imageViewCrushed.visibility = if (isChecked) ImageView.VISIBLE else ImageView.INVISIBLE
            }

            val checkBoxScratched = view.findViewById<CheckBox>(R.id.checkboxScratched)
            val imageViewScratched = view.findViewById<ImageView>(R.id.imageViewScratched)
            Picasso.get().load(currentDamageInfo.damage_mask[2]).into(imageViewScratched)
            imageViewScratched.visibility = ImageView.INVISIBLE

            checkBoxScratched.setOnCheckedChangeListener { _, isChecked ->
                imageViewScratched.visibility = if (isChecked) ImageView.VISIBLE else ImageView.INVISIBLE
            }

            val checkBoxSeparated = view.findViewById<CheckBox>(R.id.checkboxSeparated)
            val imageViewSeparated = view.findViewById<ImageView>(R.id.imageViewSeparated)
            Picasso.get().load(currentDamageInfo.damage_mask[3]).into(imageViewSeparated)
            imageViewSeparated.visibility = ImageView.INVISIBLE

            checkBoxSeparated.setOnCheckedChangeListener { _, isChecked ->
                imageViewSeparated.visibility = if (isChecked) ImageView.VISIBLE else ImageView.INVISIBLE
            }


            val textViewDamage = view.findViewById<TextView>(R.id.textViewDamage)
            textViewDamage.text = currentDamageInfo.damage_info.joinToString("\n")

            if(textViewDamage.text.contains("Breakage: Not detected")){
                checkboxBreakage.isEnabled = false
            }

            if(textViewDamage.text.contains("Crushed: Not detected")){
                checkBoxCrushed.isEnabled = false
            }

            if(textViewDamage.text.contains("Scratched: Not detected")){
                checkBoxScratched.isEnabled = false
            }

            if(textViewDamage.text.contains("Separated: Not detected")){
                checkBoxSeparated.isEnabled = false
            }

            val textViewMethod = view.findViewById<TextView>(R.id.textViewMethod)
            textViewMethod.text = "Repair method: "+ currentDamageInfo.repair_method

            val textViewCost = view.findViewById<TextView>(R.id.textViewCost)
            textViewCost.text = "Cost: "+currentDamageInfo.repair_cost+" 원"

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

        val username = intent.getStringExtra(MainActivity.USERNAME)
        val carName = intent.getStringExtra(MainActivity.CARNAME)
        val company = intent.getStringExtra(MainActivity.COMPANY)
        val carYear = intent.getStringExtra(MainActivity.CARYEAR)
        val imgBitmap = ImageHolder.imgBitmap
        val imgString = imgBitmap?.let { bitmapToBase64(it) }

        val constraintView = findViewById<ConstraintLayout>(R.id.constraintView)
        constraintView.setBackgroundColor(Color.BLACK)
        val imageViewLogo = findViewById<ImageView>(R.id.imageViewLogo)
        val textViewLoading = findViewById<TextView>(R.id.textViewLoading)
        val buttonHome = findViewById<Button>(R.id.buttonHome)
        buttonHome.visibility = Button.INVISIBLE
        buttonHome.setOnClickListener {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                putExtra(USERNAME, username)
            }
            startActivity(intent)
        }

        val json = Gson().toJson(Req(imgString, carYear, company))
        val url = "http://52.78.150.62:8000"
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
                        constraintView.setBackgroundColor(Color.WHITE)
                        imageViewLogo.visibility = ImageView.INVISIBLE
                        textViewLoading.visibility = TextView.INVISIBLE
                        buttonHome.visibility = Button.VISIBLE

                        val listView: ListView = findViewById(R.id.listView)
                        listView.adapter = DamageInfoAdapter(this@Report, responseData.info)
                    }
                }
            }
        })
    }
}