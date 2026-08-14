package com.kusumamotors.admin.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kusumamotors.admin.databinding.ActivityLoginBinding


class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            // 1. Validasi Input Kosong
            when {
                // Jika Username Kosong
                username.isEmpty() -> {
                    binding.etUsername.error = "Username wajib diisi!"
                    binding.etUsername.requestFocus() // Kursor otomatis ke sini
                }
                // Jika Password Kosong
                password.isEmpty() -> {
                    binding.etPassword.error = "Password wajib diisi!"
                    binding.etPassword.requestFocus() // Kursor otomatis ke sini
                }
                // Validasi Akun Dummy Admin
                username == "admin" && password == "admin123" -> {
                    Toast.makeText(this, "Login Berhasil! Selamat Datang Admin.", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                // Jika Password/Username Salah
                else -> {
                    Toast.makeText(this, "Username atau Password salah!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}