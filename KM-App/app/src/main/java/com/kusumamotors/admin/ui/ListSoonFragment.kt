package com.kusumamotors.admin.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.*
import com.kusumamotors.admin.adapter.ListSoonAdapter
import com.kusumamotors.admin.databinding.DialogDetailAntreanBinding
import com.kusumamotors.admin.databinding.FragmentListSoonBinding
import com.kusumamotors.admin.model.Antrean
import java.text.SimpleDateFormat
import java.util.*

class ListSoonFragment : Fragment() {

    private var _binding: FragmentListSoonBinding? = null
    private val binding get() = _binding!!

    private lateinit var dbRefReservasi: DatabaseReference
    private lateinit var adapterSoon: ListSoonAdapter
    private val masterListSoon = mutableListOf<Antrean>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListSoonBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbRefReservasi = FirebaseDatabase.getInstance().getReference("reservasi")

        setupRecyclerView()
        setupCalendarHeader()
        fetchListSoonFromFirebase()
    }

    private fun setupRecyclerView() {
        adapterSoon = ListSoonAdapter(
            context = requireContext(),
            onItemClick = { antrean -> showDialogDetailListSoon(antrean) },
            onStatusChanged = { antrean, statusBaru -> updateStatusFirebase(antrean, statusBaru) }
        )
        binding.rvListSoon.layoutManager = LinearLayoutManager(requireContext())
        binding.rvListSoon.adapter = adapterSoon
    }

    private fun setupCalendarHeader() {
        val sdfBulan = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        binding.tvBulanTahun.text = sdfBulan.format(Date()).uppercase()
    }

    private fun fetchListSoonFromFirebase() {
        val sdfHariIni = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val strHariIni = sdfHariIni.format(Date())

        dbRefReservasi.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                masterListSoon.clear()
                for (child in snapshot.children) {
                    val item = child.getValue(Antrean::class.java)
                    if (item != null) {
                        item.id = child.key ?: ""

                        // Kriteria List Soon: Tanggal Reservasi > Hari Ini (H+1 dan seterusnya)
                        if (item.tanggalReservasi > strHariIni) {
                            masterListSoon.add(item)
                        }
                    }
                }

                // Urutkan berdasarkan tanggal & jam
                masterListSoon.sortBy { "${it.tanggalReservasi} ${it.displayJam}" }
                adapterSoon.submitList(masterListSoon)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateStatusFirebase(antrean: Antrean, statusBaru: String) {
        if (antrean.id.isNotEmpty()) {
            dbRefReservasi.child(antrean.id).child("status").setValue(statusBaru)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Status diperbarui ke $statusBaru", Toast.LENGTH_SHORT).show()
                }
        }
    }

    // DIALOG DETAIL SESUAI SKETSA FIGMA
    private fun showDialogDetailListSoon(item: Antrean) {
        val dialogBinding = DialogDetailAntreanBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        with(dialogBinding) {
            tvDetailPlat.text = item.platNomor
            tvDetailNama.text = item.displayNama
            tvDetailUnit.text = item.displayUnit
            tvDetailWa.text = item.displayWa

            val jamStr = item.displayJam
            tvDetailTglJam.text = if (jamStr.isNotEmpty()) {
                "${item.tanggalReservasi} | $jamStr"
            } else {
                item.tanggalReservasi
            }

            tvDetailKeluhan.text = item.displayCatatan.ifEmpty { "-" }
            tvDetailLayanan.text = item.layanan

            if (item.isHomeService) {
                rowAlamat.visibility = View.VISIBLE
                btnBukaMaps.visibility = View.VISIBLE
                layoutMapPreview.visibility = View.VISIBLE
                tvDetailAlamat.text = if (item.hasGpsLocation) {
                    "Titik GPS (${item.latitude}, ${item.longitude})"
                } else {
                    item.alamat.ifEmpty { "-" }
                }

                btnBukaMaps.setOnClickListener {
                    val mapUri = if (item.hasGpsLocation) {
                        Uri.parse("geo:${item.latitude},${item.longitude}?q=${item.latitude},${item.longitude}(Home Service)")
                    } else {
                        Uri.parse("geo:0,0?q=${Uri.encode(item.alamat)}")
                    }
                    startActivity(Intent(Intent.ACTION_VIEW, mapUri))
                }
            } else {
                rowAlamat.visibility = View.GONE
                btnBukaMaps.visibility = View.GONE
                layoutMapPreview.visibility = View.GONE
            }

            btnCloseDetail.setOnClickListener { dialog.dismiss() }

            btnHubungiWa.setOnClickListener {
                var wa = item.displayWa.replace("[^0-9]".toRegex(), "")
                if (wa.startsWith("0")) wa = "62" + wa.substring(1)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?phone=$wa")))
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}