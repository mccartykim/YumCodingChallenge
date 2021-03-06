package com.mccartykim.yumcodingchallenge

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.mccartykim.yumcodingchallenge.ui.ticker.StockTickerFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.container, StockTickerFragment.newInstance())
                    .commitNow()
        }
    }
}
