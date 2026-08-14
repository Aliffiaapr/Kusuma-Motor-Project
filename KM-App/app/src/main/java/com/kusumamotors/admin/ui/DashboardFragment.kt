package com.kusumamotors.admin.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kusumamotors.admin.AntreanAdapter
import com.kusumamotors.admin.R
import com.kusumamotors.admin.databinding.DialogSuccessAddBinding
import com.kusumamotors.admin.databinding.DialogTambahAntreanBinding
import com.kusumamotors.admin.databinding.FragmentDashboardBinding
import com.kusumamotors.admin.model.Antrean
import com.kusumamotors.admin.utils.showSuccessDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    // Setup ViewBinding agar aman dari NullPointerException
    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var antreanAdapter: AntreanAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCurrentDate()
        setupRecyclerView()
        setupListeners()
    }

    // 1. Mengatur Tanggal Otomatis Sesuai Hari Ini (Bahasa Indonesia)
    private fun setupCurrentDate() {
        val calendar = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        val currentDateString = dateFormat.format(calendar)
        binding.tvCurrentDate.text = currentDateString
    }

    // 2. Inisialisasi RecyclerView
    private fun setupRecyclerView() {
        antreanAdapter = AntreanAdapter(
            context = requireContext(),
            onItemClick = { itemAntrean ->
                // Buka Dialog Detail saat kartu diklik
            },
            onStatusChanged = { itemAntrean, statusBaru ->
                // Update status antrean ke Firebase saat status diubah via PopupMenu
                updateStatusAntreanInFirebase(itemAntrean.id, statusBaru)
            }
        )

        binding.rvAntrean.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = antreanAdapter
        }
    }

    // 3. Listener untuk Tombol & Chip Filter
    private fun setupListeners() {
        // FAB Tambah Antrean Manual
        binding.fabAddAntrean.setOnClickListener {
            showDialogTambahAntrean()
        }

        // Filter Group 1: Status (All, Ditunda, Pending, Diproses)
        binding.chipGroupStatus.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds[0]) {
                    R.id.chipAll -> filterDataByStatus("All")
                    R.id.chipDitunda -> filterDataByStatus("Ditunda")
                    R.id.chipPending -> filterDataByStatus("Pending")
                    R.id.chipDiproses -> filterDataByStatus("Diproses")
                }
            }
        }

        // Filter Group 2: Layanan (On-Site, Home Service)
        binding.chipGroupLayanan.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds[0]) {
                    R.id.chipOnSite -> filterDataByLayanan("On-Site")
                    R.id.chipHomeService -> filterDataByLayanan("Home Service")
                }
            }
        }
    }

    private fun updateStatusAntreanInFirebase(antreanId: String, statusBaru: String) {
        if (antreanId.isEmpty()) return

        val dbRef = FirebaseDatabase.getInstance().getReference("reservasi").child(antreanId)

        // Update status di Firebase
        dbRef.child("status").setValue(statusBaru)
            .addOnSuccessListener {
                // Karena kita pakai Realtime ValueEventListener di getDataAntreanHariIni(),
                // UI RecyclerView & Angka Badge di Chip otomatis berubah sendiri!
            }
    }

    private fun updateChipCounts(listAntrean: List<Antrean>) {
        // 1. Hitung jumlah data berdasarkan status & jenis layanan
        val totalAll = listAntrean.size
        val countDitunda = listAntrean.count { it.status.equals("Ditunda", ignoreCase = true) }
        val countPending = listAntrean.count { it.status.equals("Pending", ignoreCase = true) }
        val countDiproses = listAntrean.count { it.status.equals("Diproses", ignoreCase = true) }

        val countOnSite = listAntrean.count { it.layanan.equals("On-Site", ignoreCase = true) }
        val countHomeService = listAntrean.count { it.layanan.equals("Home Service", ignoreCase = true) }

        // 2. Setel teks Chip secara dinamis
        binding.chipAll.text = if (totalAll > 0) "All ($totalAll)" else "All"
        binding.chipDitunda.text = if (countDitunda > 0) "Ditunda ($countDitunda)" else "Ditunda"
        binding.chipPending.text = if (countPending > 0) "Pending ($countPending)" else "Pending"
        binding.chipDiproses.text = if (countDiproses > 0) "Diproses ($countDiproses)" else "Diproses"

        binding.chipOnSite.text = if (countOnSite > 0) "On-Site ($countOnSite)" else "On-Site"
        binding.chipHomeService.text = if (countHomeService > 0) "Home Service ($countHomeService)" else "Home Service"
    }
    private fun filterDataByStatus(status: String) {
        // TODO: Logika menyaring list data berdasarkan status
    }

    private fun filterDataByLayanan(layanan: String) {
        // TODO: Logika menyaring list data berdasarkan jenis layanan
    }

    private fun getDataAntreanHariIni() {
        val dbRef = FirebaseDatabase.getInstance().getReference("reservasi")

        // Format tanggal hari ini (yyyy-MM-dd)
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listAntreanHariIni = mutableListOf<Antrean>()

                for (data in snapshot.children) {
                    val item = data.getValue(Antrean::class.java)
                    if (item != null) {
                        item.id = data.key ?: ""

                        // Filter: Hanya ambil antrean untuk HARI INI
                        if (item.tanggalReservasi == todayDate) {
                            listAntreanHariIni.add(item)
                        }
                    }
                }

                // 1. Tampilkan ke RecyclerView Adapter
                antreanAdapter.submitList(listAntreanHariIni)

                // 2. Perbarui Angka Badge di Chip Group atas secara otomatis!
                updateChipCounts(listAntreanHariIni)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error jika koneksi bermasalah
            }
        })
    }


    private fun showDialogTambahAntrean() {
        val dialogBinding = DialogTambahAntreanBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val calendar = Calendar.getInstance()
        var selectedDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        var selectedTimeFormatted = "08:30" // Default Slot Jam

        // 1. FITUR TANGGAL (HARI SEBELUMNYA DI-DISABLE / ABU-ABU)
        dialogBinding.btnPilihTanggal.setOnClickListener {
            val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                val displayDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
                dialogBinding.btnPilihTanggal.text = "$displayDate ▼"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

            // Kunci Tanggal Minimal = Hari Ini (Tanggal lalu otomatis abu-abu & tidak bisa diklik)
            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            datePickerDialog.show()
        }

        // 2. FITUR SLOT JAM OPERASIONAL BENGKEL
        val slotJamBengkel = arrayOf(
            "07:00 WIB",
            "08:30 WIB",
            "10:00 WIB",
            "11:30 WIB",
            "13:00 WIB",
            "14:30 WIB",
            "16:00 WIB"
        )

        dialogBinding.btnPilihJam.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Pilih Jam Service")
                .setItems(slotJamBengkel) { _, which ->
                    selectedTimeFormatted = slotJamBengkel[which]
                    dialogBinding.btnPilihJam.text = "$selectedTimeFormatted ▼"
                }
                .show()
        }

        // Event Close & Batal
        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnBatal.setOnClickListener { dialog.dismiss() }

        // Event Simpan ke Firebase
        dialogBinding.btnSimpan.setOnClickListener {
            val plat = dialogBinding.etPlatNomor.text.toString().trim()
            val nama = dialogBinding.etNama.text.toString().trim()
            val wa = dialogBinding.etWhatsapp.text.toString().trim()
            val unit = dialogBinding.etJenisMobil.text.toString().trim()
            val keluhan = dialogBinding.etKeluhan.text.toString().trim()

            if (plat.isEmpty()) {
                dialogBinding.etPlatNomor.error = "Plat nomor wajib diisi"
                return@setOnClickListener
            }
            if (nama.isEmpty()) {
                dialogBinding.etNama.error = "Nama wajib diisi"
                return@setOnClickListener
            }

            // Simpan Data
            val dbRef = FirebaseDatabase.getInstance().getReference("reservasi")
            val newKey = dbRef.push().key ?: System.currentTimeMillis().toString()

            val currentTime = SimpleDateFormat("dd/M/yyyy, HH.mm.ss", Locale.getDefault()).format(
                Date())

            val dataAntrean = HashMap<String, Any>()
            dataAntrean["namaPelanggan"] = nama
            dataAntrean["nomorWhatsapp"] = wa.ifEmpty { "-" }
            dataAntrean["platNomor"] = plat
            dataAntrean["jenisUnit"] = unit.ifEmpty { "-" }
            dataAntrean["waktuServis"] = selectedTimeFormatted
            dataAntrean["layanan"] = "On-Site" // Default Manual = On-Site
            dataAntrean["status"] = "Pending"  // Default Baru = Pending
            dataAntrean["catatan"] = keluhan.ifEmpty { "Service Rutin" }
            dataAntrean["alamat"] = "-"
            dataAntrean["tanggalReservasi"] = selectedDateFormatted
            dataAntrean["waktuDibuat"] = currentTime

            dbRef.child(newKey).setValue(dataAntrean).addOnSuccessListener {
                dialog.dismiss()
                showSuccessDialog("Antrian berhasil di\ntambahkan !")
            }
        }

        dialog.show()
    }

    // Bersihkan binding saat view dihancurkan agar tidak memory leak
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}