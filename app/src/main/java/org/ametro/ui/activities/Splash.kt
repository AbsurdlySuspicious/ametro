package org.ametro.ui.activities

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.ametro.ui.activities.Map

class Splash: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startMain()
    }

    private fun startMain() {
        startActivity(Intent(this, Map::class.java))
        finish()
    }
}
