package com.sketchydesignanddevelopment.amply_test

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.sketchydesignanddevelopment.amply_test.fragments.MainFragment
import com.sketchydesignanddevelopment.amply_test.fragments.SearchFragment

class MainActivity : AppCompatActivity(), MainFragment.OnButtonPressed {

    private var mainFragmentTag = "mainFragmentTag"
    private var searchFragmentTag = "searchFragmentTag"
    private lateinit var mainFragment: MainFragment
    private lateinit var searchFragment: SearchFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainFragment = MainFragment.newInstance()
        searchFragment = SearchFragment.newInstance()

        //load main fragment
        supportFragmentManager
            .beginTransaction()
            .add(R.id.mainFragmentView, mainFragment, mainFragmentTag)
            .commit()
    }

    //change to search screen with button press in main fragment
    override fun onButtonPressed() {
        supportFragmentManager
            .beginTransaction()
            .addToBackStack(mainFragmentTag)
            .replace(R.id.mainFragmentView, searchFragment, searchFragmentTag)
            .commit()
    }
}