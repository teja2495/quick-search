package com.tk.quicksearch

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivityLauncher : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = Intent(this, MainActivity::class.java)

        // Forward existing extras
        intent.putExtra("from_trampoline", true) // Optional debugging
        if (getIntent().extras != null) {
            intent.putExtras(getIntent())
        }

        val options = Bundle()
        // SPLASH_SCREEN_STYLE_ICON = 1
        // This forces the icon to be shown when starting the new activity
        options.putInt("android.activity.splashScreenStyle", 1)

        finish()
        startActivity(intent, options)
    }
}
