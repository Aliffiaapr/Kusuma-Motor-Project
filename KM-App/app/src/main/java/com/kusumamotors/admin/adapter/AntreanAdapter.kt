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
    private var listAntrean: List<Antrean> = emptyList(),
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
            // 1. Tampilkan Plat & Nama (Gunakan displayNama agar membaca 'nama' dari web & 'namaPelanggan' manual)
            tvPlatDanNama.text = "${item.platNomor} - ${item.displayNama}"

            // 2. Tampilkan Tanggal + Jam + Layanan
            val jamStr = item.displayJam
            val infoWaktu = if (jamStr.isNotEmpty()) {
                "${item.tanggalReservasi} ($jamStr)"
            } else {
                item.tanggalReservasi
            }
            tvWaktuDanLayanan.text = "$infoWaktu | ${item.layanan}"

            // 3. Status Badge Text
            tvStatusBadge.text = item.status

            // Pasang background shape dasar
            btnStatusBadge.setBackgroundResource(R.drawable.bg_badge_rounded)

            // Tentukan warna HEX sesuai status
            val warnaHex = when (item.status) {
                "Pending" -> "#FBC02D"   // Kuning
                "Diproses" -> "#4CAF50"  // Hijau
                "Ditunda" -> "#E53935"   // Merah
                "Selesai" -> "#2196F3"   // Biru
                else -> "#FBC02D"
            }

            // Set warna background dinamis
            btnStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(warnaHex))

            // Klik Kartu Utama -> Buka Dialog Detail
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

    fun submitList(newList: List<Antrean>) {
        listAntrean = newList
        notifyDataSetChanged()
    }

    fun updateList(newList: List<Antrean>) {
        submitList(newList)
    }
}