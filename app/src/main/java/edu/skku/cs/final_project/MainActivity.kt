package edu.skku.cs.final_project

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
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.Base64

class MainActivity : AppCompatActivity() {
    val PICK_IMAGE_REQUEST = 1
    var imgBitmap: Bitmap? = null
    companion object {
        const val USERNAME = "name"
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val actionBar = supportActionBar
        actionBar?.hide()

        val username = intent.getStringExtra(Login.USERNAME)

        val buttonProfile = findViewById<ImageButton>(R.id.buttonProfile)
        buttonProfile.setOnClickListener {
            val intent = Intent(applicationContext, MyInfo::class.java).apply {
                putExtra(USERNAME, username)
            }
            startActivity(intent)
        }

        val buttonAlbum = findViewById<Button>(R.id.buttonAlbum)
        buttonAlbum.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
        val buttonCamera = findViewById<Button>(R.id.buttonCamera)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null){
            val imgUri: Uri? = data.data
            imgBitmap = imgUri?.let { getBitmap(it) }
        }
    }
}