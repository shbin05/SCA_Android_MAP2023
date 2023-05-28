package edu.skku.cs.final_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class Start : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start)

        val actionBar = supportActionBar
        actionBar?.hide()

        val buttonLogin = findViewById<Button>(R.id.buttonLogin)
        val buttonRegister = findViewById<Button>(R.id.buttonRegister)

        buttonLogin.setOnClickListener {
            val intent = Intent(applicationContext, Login::class.java)
            startActivity(intent)
        }

        buttonRegister.setOnClickListener {
            val intent = Intent(applicationContext, Register::class.java)
            startActivity(intent)
        }
    }
}