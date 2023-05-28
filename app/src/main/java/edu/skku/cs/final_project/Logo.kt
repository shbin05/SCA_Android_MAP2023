package edu.skku.cs.final_project

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Button

class Logo : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logo)

        val actionBar = supportActionBar
        actionBar?.hide()

        val intent = Intent(applicationContext, Start::class.java)
        val handler = Handler()
        handler.postDelayed(Runnable {
            startActivity(intent)
        }, 1500)

    }
}