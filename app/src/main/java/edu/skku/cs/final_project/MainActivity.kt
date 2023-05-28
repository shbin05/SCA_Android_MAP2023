package edu.skku.cs.final_project

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import android.util.Base64

class MainActivity : AppCompatActivity() {
    var username: String? = null

    val PICK_IMAGE_REQUEST = 1
    lateinit var imgBitmap: Bitmap
    lateinit var imgString: String

    lateinit var imageViewCar: ImageView
    lateinit var button1: Button
    lateinit var button2: Button
    companion object {
        const val USERNAME = "name"
        const val IMGSTR = "image"
    }
    fun getBitmap(uri: Uri): Bitmap? {
        var inputStream: InputStream? = null
        try {
            val resolver: ContentResolver = contentResolver
            inputStream = resolver.openInputStream(uri)

            return BitmapFactory.decodeStream(inputStream)
        } catch (e: IOException) {
            println(e)
        } finally {
            inputStream?.close()
        }
        return null
    }
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val actionBar = supportActionBar
        actionBar?.hide()

        username = intent.getStringExtra(Login.USERNAME)

        val buttonProfile = findViewById<ImageButton>(R.id.buttonProfile)
        buttonProfile.setOnClickListener {
            val intent = Intent(applicationContext, MyInfo::class.java).apply {
                putExtra(USERNAME, username)
            }
            startActivity(intent)
        }

        imageViewCar = findViewById(R.id.imageViewCar)
        imageViewCar.visibility = ImageView.INVISIBLE

        button1 = findViewById(R.id.button1)
        button1.text = "사진 가져오기"
        button1.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
        button2 = findViewById<Button>(R.id.button2)
        button2.text = "사진 촬영하기"
    }
    fun initialize() {
        imageViewCar = findViewById(R.id.imageViewCar)
        imageViewCar.visibility = ImageView.INVISIBLE

        button1 = findViewById(R.id.button1)
        button1.text = "사진 가져오기"
        button1.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
        button2 = findViewById<Button>(R.id.button2)
        button2.text = "사진 촬영하기"
    }
    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null){
            val imgUri: Uri? = data.data
            imgBitmap = imgUri?.let { getBitmap(it) }!!
            imgString = bitmapToBase64(imgBitmap)

            imageViewCar.visibility = ImageView.VISIBLE
            imageViewCar.setImageBitmap(imgBitmap)

            button1.text = "검사 진행"
            button1.setOnClickListener {
                val intent = Intent(applicationContext, Report::class.java).apply {
                    putExtra(USERNAME, username)
                    putExtra(IMGSTR, imgString)
                }
                startActivity(intent)
            }
            button2.text = "다시 선택"
            button2.setOnClickListener {
                initialize()
            }
        }
    }
}