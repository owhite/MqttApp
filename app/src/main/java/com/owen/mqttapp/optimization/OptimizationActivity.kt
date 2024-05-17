package com.owen.mqttapp.optimization

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.owen.mqttapp.R
import com.owen.mqttapp.SecondActivity

class OptimizationActivity : FragmentActivity() {
    private val myTaskFragment = OptimizationFragment.newInstance()
    private var activeFragment: Fragment = myTaskFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_optimization)

        val fragmentManager = supportFragmentManager.beginTransaction()
        fragmentManager.replace(R.id.tasks_fragment_container, myTaskFragment)
        fragmentManager.commit()
        activeFragment = myTaskFragment


    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    fun doNext() {
        Intent(this@OptimizationActivity, SecondActivity::class.java).also {
            startActivity(it)
            finish()
        }
    }


}
