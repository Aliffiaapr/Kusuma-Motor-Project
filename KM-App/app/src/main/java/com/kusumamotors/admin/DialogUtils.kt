package com.kusumamotors.admin.utils

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.kusumamotors.admin.databinding.DialogSuccessAddBinding

// Extension Function agar BISA DIPANGGIL DARI FRAGMENT MANAPUN!
fun Fragment.showSuccessDialog(pesan: String) {
    val successBinding = DialogSuccessAddBinding.inflate(layoutInflater)
    val builder = AlertDialog.Builder(requireContext())
    builder.setView(successBinding.root)
    val successDialog = builder.create()

    successDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    successDialog.setCancelable(false)

    // Set pesan dinamis
    successBinding.tvMessageSuccess.text = pesan

    successBinding.btnOkSuccess.setOnClickListener {
        successDialog.dismiss()
    }

    successDialog.show()
}