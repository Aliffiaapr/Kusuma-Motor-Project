package com.kusumamotors.admin.ui

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.kusumamotors.admin.AntreanAdapter
import com.kusumamotors.admin.R
import com.kusumamotors.admin.databinding.DialogDetailAntreanBinding
import com.kusumamotors.admin.databinding.DialogKonfirmasiSelesaiBinding
import com.kusumamotors.admin.databinding.DialogTambahAntreanBinding
import com.kusumamotors.admin.databinding.FragmentDashboardBinding
import com.kusumamotors.admin.model.Antrean
import com.kusumamotors.admin.utils.showSuccessDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var antreanAdapter: AntreanAdapter
    private var fullListHariIni: List<Antrean> = emptyList()

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
        getDataAntreanHariIni()
    }

    // 1. Tanggal Hari Ini (Format Indonesia)
    private fun setupCurrentDate() {
        val calendar = Calendar.getInstance().time
        val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        binding.tvCurrentDate.text = dateFormat.format(calendar)
    }

    // 2. Inisialisasi RecyclerView & Adapter
    private fun setupRecyclerView() {
        antreanAdapter = AntreanAdapter(
            context = requireContext(),
            onItemClick = { itemAntrean ->
                showDialogDetailAntrean(itemAntrean)
            },
            onStatusChanged = { itemAntrean, statusBaru ->
                if (statusBaru == "Selesai") {
                    showDialogKonfirmasiSelesai(itemAntrean)
                } else {
                    updateStatusAntreanInFirebase(itemAntrean.id, statusBaru)
                }
            }
        )

        binding.rvAntrean.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = antreanAdapter
        }
    }

    // 3. Listener untuk Tombol Tambah & Chip Filters
    private fun setupListeners() {
        binding.fabAddAntrean.setOnClickListener {
            showDialogTambahAntrean()
        }

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

        binding.chipGroupLayanan.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds[0]) {
                    R.id.chipOnSite -> filterDataByLayanan("On-Site")
                    R.id.chipHomeService -> filterDataByLayanan("Home Service")
                }
            }
        }
    }

    // 4. Load Realtime Data dari Firebase
    private fun getDataAntreanHariIni() {
        val dbRef = FirebaseDatabase.getInstance().getReference("reservasi")
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded) return

                val listHariIni = mutableListOf<Antrean>()
                for (data in snapshot.children) {
                    val item = data.getValue(Antrean::class.java)
                    if (item != null) {
                        item.id = data.key ?: ""
                        if (item.tanggalReservasi == todayDate) {
                            listHariIni.add(item)
                        }
                    }
                }

                fullListHariIni = listHariIni
                antreanAdapter.submitList(listHariIni)
                updateChipCounts(listHariIni)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 5. Update Status di Firebase
    private fun updateStatusAntreanInFirebase(antreanId: String, statusBaru: String) {
        if (antreanId.isEmpty()) return
        val dbRef = FirebaseDatabase.getInstance().getReference("reservasi").child(antreanId)
        dbRef.child("status").setValue(statusBaru)
    }

    // 6. Update Angka Badge Chip
    private fun updateChipCounts(listAntrean: List<Antrean>) {
        val totalAll = listAntrean.size
        val countDitunda = listAntrean.count { it.status.equals("Ditunda", ignoreCase = true) }
        val countPending = listAntrean.count { it.status.equals("Pending", ignoreCase = true) }
        val countDiproses = listAntrean.count { it.status.equals("Diproses", ignoreCase = true) }

        val countOnSite = listAntrean.count { it.layanan.equals("On-Site", ignoreCase = true) }
        val countHomeService = listAntrean.count { it.layanan.equals("Home Service", ignoreCase = true) }

        binding.chipAll.text = if (totalAll > 0) "All ($totalAll)" else "All"
        binding.chipDitunda.text = if (countDitunda > 0) "Ditunda ($countDitunda)" else "Ditunda"
        binding.chipPending.text = if (countPending > 0) "Pending ($countPending)" else "Pending"
        binding.chipDiproses.text = if (countDiproses > 0) "Diproses ($countDiproses)" else "Diproses"

        binding.chipOnSite.text = if (countOnSite > 0) "On-Site ($countOnSite)" else "On-Site"
        binding.chipHomeService.text = if (countHomeService > 0) "Home Service ($countHomeService)" else "Home Service"
    }

    // 7. Filter Data Status & Layanan
    private fun filterDataByStatus(status: String) {
        if (status == "All") {
            antreanAdapter.submitList(fullListHariIni)
        } else {
            val filtered = fullListHariIni.filter { it.status.equals(status, ignoreCase = true) }
            antreanAdapter.submitList(filtered)
        }
    }

    private fun filterDataByLayanan(layanan: String) {
        val filtered = fullListHariIni.filter { it.layanan.equals(layanan, ignoreCase = true) }
        antreanAdapter.submitList(filtered)
    }

    // 8. Dialog Detail Antrean (On-Site & Home Service)
    private fun showDialogDetailAntrean(item: Antrean) {
        val bindingDialog = DialogDetailAntreanBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(bindingDialog.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        with(bindingDialog) {
            tvDetailPlat.text = item.platNomor
            tvDetailNama.text = item.displayNama
            tvDetailUnit.text = item.displayUnit
            tvDetailWa.text = item.displayWa

            // 1. Format Tgl & Jam (Aman dari string kosong)
            val jamText = item.displayJam
            tvDetailTglJam.text = if (jamText.isNotEmpty()) {
                "${item.tanggalReservasi} | $jamText"
            } else {
                item.tanggalReservasi
            }

            tvDetailKeluhan.text = item.displayCatatan.ifEmpty { "-" }
            tvDetailLayanan.text = item.layanan

            // 2. Fitur Home Service (Cek Helper `isHomeService` & GPS)
            if (item.isHomeService) {
                rowAlamat.visibility = View.VISIBLE
                btnBukaMaps.visibility = View.VISIBLE
                layoutMapPreview.visibility = View.VISIBLE

                // Tampilkan Teks Alamat / Titik GPS
                tvDetailAlamat.text = when {
                    item.hasGpsLocation -> "Titik GPS (${item.latitude}, ${item.longitude})"
                    item.alamat.isNotEmpty() -> item.alamat
                    else -> "-"
                }

                btnBukaMaps.setOnClickListener {
                    val gmmIntentUri = if (item.hasGpsLocation) {
                        // Navigasi presisi via Titik GPS Laptop/HP
                        Uri.parse("geo:${item.latitude},${item.longitude}?q=${item.latitude},${item.longitude}(Lokasi Home Service)")
                    } else {
                        // Navigasi via Alamat Teks
                        val alamatEncoded = Uri.encode(item.alamat)
                        Uri.parse("geo:0,0?q=$alamatEncoded")
                    }

                    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                        setPackage("com.google.android.apps.maps")
                    }

                    try {
                        startActivity(mapIntent)
                    } catch (e: Exception) {
                        // Fallback membuka via Browser jika aplikasi Google Maps tidak ada
                        val urlWebMap = if (item.hasGpsLocation) {
                            "https://maps.google.com/?q=${item.latitude},${item.longitude}"
                        } else {
                            "https://maps.google.com/?q=${Uri.encode(item.alamat)}"
                        }
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlWebMap)))
                    }
                }
            } else {
                rowAlamat.visibility = View.GONE
                btnBukaMaps.visibility = View.GONE
                layoutMapPreview.visibility = View.GONE
            }

            btnCloseDetail.setOnClickListener { dialog.dismiss() }

            // 3. Format Nomor WhatsApp (Ubah 08xx ke 628xx Otomatis)
            btnHubungiWa.setOnClickListener {
                var nomorWa = item.displayWa.replace("[^0-9]".toRegex(), "")
                if (nomorWa.startsWith("0")) {
                    nomorWa = "62" + nomorWa.substring(1)
                }
                val url = "https://api.whatsapp.com/send?phone=$nomorWa"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            }
        }

        dialog.show()
    }

    // 9. Dialog Konfirmasi Selesai
    private fun showDialogKonfirmasiSelesai(item: Antrean) {
        val bindingDialog = DialogKonfirmasiSelesaiBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(bindingDialog.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        bindingDialog.tvPesanKonfirmasi.text = "Apakah servis untuk [ ${item.platNomor} - ${item.displayNama} ] sudah selesai?"
        bindingDialog.btnBatalSelesai.setOnClickListener { dialog.dismiss() }
        bindingDialog.btnKonfirmasiSelesai.setOnClickListener {
            dialog.dismiss()
            updateStatusAntreanInFirebase(item.id, "Selesai")
        }

        dialog.show()
    }

    // 10. Dialog Tambah Antrean Manual
    private fun showDialogTambahAntrean() {
        val dialogBinding = DialogTambahAntreanBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(requireContext())
        builder.setView(dialogBinding.root)
        val dialog = builder.create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val calendar = Calendar.getInstance()
        var selectedDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
        var selectedTimeFormatted = ""

        dialogBinding.btnPilihTanggal.setOnClickListener {
            val datePickerDialog = DatePickerDialog(requireContext(), { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                selectedDateFormatted = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
                val displayDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
                dialogBinding.btnPilihTanggal.text = "$displayDate ▼"
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))

            datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
            datePickerDialog.show()
        }

        dialogBinding.btnPilihJam.setOnClickListener {
            val settingsRef = FirebaseDatabase.getInstance().getReference("settings/jamOperasional")

            settingsRef.get().addOnSuccessListener { snapshot ->
                val listJam = mutableListOf<String>()
                for (child in snapshot.children) {
                    child.getValue(String::class.java)?.let { listJam.add(it) }
                }

                if (listJam.isNotEmpty()) {
                    val slotJamArray = listJam.toTypedArray()
                    AlertDialog.Builder(requireContext())
                        .setTitle("Pilih Jam Service")
                        .setItems(slotJamArray) { _, which ->
                            selectedTimeFormatted = slotJamArray[which]
                            dialogBinding.btnPilihJam.text = "$selectedTimeFormatted ▼"
                        }
                        .show()
                } else {
                    Toast.makeText(requireContext(), "Jam operasional belum diatur di Setting", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal mengambil data jam operasional", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnBatal.setOnClickListener { dialog.dismiss() }

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
            if (selectedTimeFormatted.isEmpty()) {
                Toast.makeText(requireContext(), "Pilih jam service terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dbRef = FirebaseDatabase.getInstance().getReference("reservasi")
            val newKey = dbRef.push().key ?: System.currentTimeMillis().toString()
            val currentTime = SimpleDateFormat("dd/M/yyyy, HH.mm.ss", Locale.getDefault()).format(Date())

            val dataAntrean = HashMap<String, Any>()
            dataAntrean["namaPelanggan"] = nama
            dataAntrean["nomorWhatsapp"] = wa.ifEmpty { "-" }
            dataAntrean["platNomor"] = plat
            dataAntrean["jenisUnit"] = unit.ifEmpty { "-" }
            dataAntrean["waktuServis"] = selectedTimeFormatted
            dataAntrean["layanan"] = "On-Site"
            dataAntrean["status"] = "Pending"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}