package edu.skku.cs.final_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val actionBar = supportActionBar
        actionBar?.hide()

        val username = intent.getStringExtra(Login.USERNAME)
        val car = intent.getStringExtra(Login.USERCAR)

        val textViewUsername = findViewById<TextView>(R.id.textViewUsername)
        textViewUsername.text = username+"님 환영합니다!"
        val textViewCar = findViewById<TextView>(R.id.textViewCar)
        textViewCar.text = "차종: "+car

        val buttonAlbum = findViewById<Button>(R.id.buttonAlbum)
        buttonAlbum.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivity(intent)
        }
        val buttonCamera = findViewById<Button>(R.id.buttonCamera)

    }
}