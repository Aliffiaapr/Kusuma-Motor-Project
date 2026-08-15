package com.kusumamotors.admin.ui

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kusumamotors.admin.databinding.DialogKonfirmasiUbahQrBinding
import com.kusumamotors.admin.databinding.DialogLogoutBinding
import com.kusumamotors.admin.databinding.DialogQrCodeBinding
import com.kusumamotors.admin.databinding.DialogUbahPasswordBinding
import com.kusumamotors.admin.databinding.FragmentSettingBinding
import com.kusumamotors.admin.utils.showSuccessDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private val dbRefSettings = FirebaseDatabase.getInstance().getReference("settings")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTanggalHariIni()
        loadSettingsFromFirebase()

        binding.switchStatusOperasional.setOnCheckedChangeListener { _, isChecked ->
            updateUIState(isChecked)
        }

        binding.btnJamBuka.setOnClickListener { showTimePicker(isJamBuka = true) }
        binding.btnJamTutup.setOnClickListener { showTimePicker(isJamBuka = false) }
        binding.btnSimpanJam.setOnClickListener { simpanSettingKeFirebase() }

        // LISTENER TOMBOL DIALOG SETTING
        binding.btnQrCodeBooking.setOnClickListener { showDialogQrCode() }
        binding.btnUbahPassword.setOnClickListener { showDialogUbahPassword() }
        binding.btnLogout.setOnClickListener { showDialogLogout() }
    }

    private fun setupTanggalHariIni() {
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        binding.tvTanggalSetting.text = dateFormat.format(Date())
    }

    private fun showTimePicker(isJamBuka: Boolean) {
        val calendar = Calendar.getInstance()
        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)
                if (isJamBuka) binding.btnJamBuka.text = timeFormatted else binding.btnJamTutup.text = timeFormatted
            },
            calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true
        )
        timePickerDialog.show()
    }

    private fun updateUIState(isOpen: Boolean) {
        binding.layoutJamOperasional.alpha = if (isOpen) 1.0f else 0.4f
        binding.btnJamBuka.isEnabled = isOpen
        binding.btnJamTutup.isEnabled = isOpen
        binding.btnSimpanJam.isEnabled = isOpen
    }

    private fun loadSettingsFromFirebase() {
        dbRefSettings.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return
                val isBuka = snapshot.child("isOpen").getValue(Boolean::class.java) ?: true
                val jamBuka = snapshot.child("jamBuka").getValue(String::class.java) ?: "07:00"
                val jamTutup = snapshot.child("jamTutup").getValue(String::class.java) ?: "16:00"

                binding.switchStatusOperasional.isChecked = isBuka
                binding.btnJamBuka.text = jamBuka
                binding.btnJamTutup.text = jamTutup
                updateUIState(isBuka)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun simpanSettingKeFirebase() {
        val isBuka = binding.switchStatusOperasional.isChecked
        val jamBuka = binding.btnJamBuka.text.toString()
        val jamTutup = binding.btnJamTutup.text.toString()

        val settingData = HashMap<String, Any>()
        settingData["isOpen"] = isBuka
        settingData["jamBuka"] = jamBuka
        settingData["jamTutup"] = jamTutup
        settingData["jamOperasional"] = generateSlotJamPer2Jam(jamBuka, jamTutup)

        dbRefSettings.updateChildren(settingData).addOnSuccessListener {
            showSuccessDialog("Pengaturan jam\nberhasil disimpan !")
        }
    }

    private fun generateSlotJamPer2Jam(bukaStr: String, tutupStr: String): List<String> {
        val listSlot = mutableListOf<String>()
        try {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateBuka = sdf.parse(bukaStr) ?: return listSlot
            val dateTutup = sdf.parse(tutupStr) ?: return listSlot

            val calBuka = Calendar.getInstance().apply { time = dateBuka }
            val calTutup = Calendar.getInstance().apply { time = dateTutup }

            while (!calBuka.after(calTutup)) {
                val formattedSlot = String.format(
                    Locale.getDefault(),
                    "%02d:%02d WIB",
                    calBuka.get(Calendar.HOUR_OF_DAY),
                    calBuka.get(Calendar.MINUTE)
                )
                listSlot.add(formattedSlot)
                calBuka.add(Calendar.HOUR_OF_DAY, 2)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return listSlot
    }

    // --- DIALOG QR CODE BOOKING ---
    private fun showDialogQrCode() {
        val bindingDialog = DialogQrCodeBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(bindingDialog.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dbRefSettings.child("qrCode").get().addOnSuccessListener { snapshot ->
            val link = snapshot.child("link").getValue(String::class.java) ?: "https://kusumamotors.com/booking/default"
            val updatedAt = snapshot.child("updatedAt").getValue(String::class.java) ?: "12 Jan 2026, 07:45"

            bindingDialog.tvLinkBooking.text = link
            bindingDialog.tvTerakhirDiperbarui.text = "Terakhir di perbarui $updatedAt"
        }

        bindingDialog.btnCloseQr.setOnClickListener { dialog.dismiss() }
        bindingDialog.btnUbahQr.setOnClickListener {
            dialog.dismiss()
            showDialogKonfirmasiUbahQr()
        }
        bindingDialog.btnUnduhQr.setOnClickListener {
            Toast.makeText(requireContext(), "QR Code berhasil diunduh ke Galeri!", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    // --- DIALOG KONFIRMASI UBAH QR ---
    private fun showDialogKonfirmasiUbahQr() {
        val bindingDialog = DialogKonfirmasiUbahQrBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(bindingDialog.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        bindingDialog.btnCloseKonfirmasiQr.setOnClickListener { dialog.dismiss() }
        bindingDialog.btnBatalUbahQr.setOnClickListener { dialog.dismiss() }

        bindingDialog.btnKonfirmasiUbahQr.setOnClickListener {
            val passwordInput = bindingDialog.etPasswordAdminQr.text.toString().trim()
            if (passwordInput.isEmpty()) {
                bindingDialog.etPasswordAdminQr.error = "Password wajib diisi"
                return@setOnClickListener
            }

            // Generate Token Link Baru
            val randomToken = UUID.randomUUID().toString().take(5)
            val newLink = "https://kusumamotors.com/booking/$randomToken"
            val newTime = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date())

            val qrData = HashMap<String, Any>()
            qrData["link"] = newLink
            qrData["updatedAt"] = newTime

            dbRefSettings.child("qrCode").setValue(qrData).addOnSuccessListener {
                dialog.dismiss()
                showSuccessDialog("QR Code Berhasil\nDiperbarui !")
            }
        }

        dialog.show()
    }

    // --- DIALOG UBAH PASSWORD ---
    private fun showDialogUbahPassword() {
        val bindingDialog = DialogUbahPasswordBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(bindingDialog.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        bindingDialog.btnCloseUbahPass.setOnClickListener { dialog.dismiss() }
        bindingDialog.btnBatalUbahPass.setOnClickListener { dialog.dismiss() }

        bindingDialog.btnSimpanPassBaru.setOnClickListener {
            val passBaru = bindingDialog.etPassBaru.text.toString().trim()
            val konfirmasiPass = bindingDialog.etKonfirmasiPassBaru.text.toString().trim()

            if (passBaru.length < 8) {
                bindingDialog.etPassBaru.error = "Password minimal 8 karakter"
                return@setOnClickListener
            }
            if (passBaru != konfirmasiPass) {
                bindingDialog.etKonfirmasiPassBaru.error = "Konfirmasi password tidak cocok"
                return@setOnClickListener
            }

            // Simpan password baru ke Firebase
            dbRefSettings.child("adminPassword").setValue(passBaru).addOnSuccessListener {
                dialog.dismiss()
                showSuccessDialog("Password Berhasil\nDiperbarui !")
            }
        }

        dialog.show()
    }

    // --- DIALOG LOGOUT ---
    private fun showDialogLogout() {
        val bindingDialog = DialogLogoutBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(bindingDialog.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        bindingDialog.btnBatalLogout.setOnClickListener { dialog.dismiss() }
        bindingDialog.btnKonfirmasiLogout.setOnClickListener {
            dialog.dismiss()
            requireActivity().finish() // Keluar aplikasi
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}