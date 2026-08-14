package com.kusumamotors.admin

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.kusumamotors.admin.databinding.ItemAntreanBinding
import com.kusumamotors.admin.model.Antrean

class AntreanAdapter(
    private val context: Context,
    private var listAntrean: List<Antrean> = emptyList(), // Ditambahkan default emptyList()
    private val onItemClick: (Antrean) -> Unit,
    private val onStatusChanged: (Antrean, String) -> Unit
) : RecyclerView.Adapter<AntreanAdapter.AntreanViewHolder>() {

    inner class AntreanViewHolder(val binding: ItemAntreanBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AntreanViewHolder {
        val binding = ItemAntreanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AntreanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AntreanViewHolder, position: Int) {
        val item = listAntrean[position]

        with(holder.binding) {
            // Set Teks Data Pelanggan
            tvPlatDanNama.text = "${item.platNomor} - ${item.namaPelanggan}"
            tvWaktuDanLayanan.text = "${item.waktuServis} | ${item.layanan}"
            tvStatusBadge.text = item.status

            // 1. Pasang 1 background shape dasar
            btnStatusBadge.setBackgroundResource(R.drawable.bg_badge_rounded)

            // 2. Tentukan warna HEX sesuai status
            val warnaHex = when (item.status) {
                "Pending" -> "#FBC02D"   // Kuning
                "Diproses" -> "#4CAF50"  // Hijau
                "Ditunda" -> "#E53935"   // Merah
                "Selesai" -> "#2196F3"   // Biru
                else -> "#FBC02D"
            }

            // 3. Set warna background-nya secara dinamis
            btnStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(warnaHex))

            // Klik Kartu Utama -> Buka Dialog Detail (On-Site / Home Service)
            cardAntrean.setOnClickListener {
                onItemClick(item)
            }

            // Klik Badge Status -> Tampilkan Dropdown Popup Menu
            btnStatusBadge.setOnClickListener { view ->
                val popup = PopupMenu(context, view)
                popup.menu.add("Pending")
                popup.menu.add("Diproses")
                popup.menu.add("Ditunda")
                popup.menu.add("Selesai")

                popup.setOnMenuItemClickListener { menuItem ->
                    val statusBaru = menuItem.title.toString()
                    onStatusChanged(item, statusBaru)
                    true
                }
                popup.show()
            }
        }
    }

    override fun getItemCount(): Int = listAntrean.size

    // FUNGSI BARU: Agar panggilan submitList(...) di DashboardFragment TIDAK MERAH LAGI!
    fun submitList(newList: List<Antrean>) {
        listAntrean = newList
        notifyDataSetChanged()
    }

    // Dipertahankan agar tidak membingungkan jika ada yang memanggil updateList(...)
    fun updateList(newList: List<Antrean>) {
        submitList(newList)
    }
}