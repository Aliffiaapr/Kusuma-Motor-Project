package com.kusumamotors.admin.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.kusumamotors.admin.ui.DashboardFragment
import com.kusumamotors.admin.R
import com.kusumamotors.admin.ui.MendatangFragment
import com.kusumamotors.admin.ui.RiwayatFragment
import com.kusumamotors.admin.ui.SettingFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Halaman awal default saat aplikasi dibuka: Dashboard
        loadFragment(DashboardFragment())

        // Logika perpindahan halaman saat menu diklik
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(DashboardFragment())
                R.id.nav_soon -> loadFragment(MendatangFragment())
                R.id.nav_riwayat -> loadFragment(RiwayatFragment())
                R.id.nav_setting -> loadFragment(SettingFragment())
            }
            true
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}