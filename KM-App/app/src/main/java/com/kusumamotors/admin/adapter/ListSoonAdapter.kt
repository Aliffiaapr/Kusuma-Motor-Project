package com.kusumamotors.admin.adapter

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.kusumamotors.admin.databinding.ItemAntreanSoonBinding
import com.kusumamotors.admin.model.Antrean

class ListSoonAdapter(
    private val context: Context,
    private var listAntrean: List<Antrean> = emptyList(),
    private val onItemClick: (Antrean) -> Unit,
    private val onStatusChanged: (Antrean, String) -> Unit
) : RecyclerView.Adapter<ListSoonAdapter.SoonViewHolder>() {

    inner class SoonViewHolder(val binding: ItemAntreanSoonBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SoonViewHolder {
        val binding = ItemAntreanSoonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SoonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SoonViewHolder, position: Int) {
        val currentItem = listAntrean[position]
        val prevItem = if (position > 0) listAntrean[position - 1] else null

        with(holder.binding) {
            // Pengelompokan Header Tanggal
            if (prevItem == null || prevItem.tanggalReservasi != currentItem.tanggalReservasi) {
                tvHeaderTanggalGroup.visibility = View.VISIBLE
                tvHeaderTanggalGroup.text = currentItem.tanggalReservasi
            } else {
                tvHeaderTanggalGroup.visibility = View.GONE
            }

            // Teks Card
            tvPlatDanNamaSoon.text = "${currentItem.platNomor} - ${currentItem.displayNama}"
            val jamStr = currentItem.displayJam
            tvWaktuLayananSoon.text = if (jamStr.isNotEmpty()) {
                "$jamStr | ${currentItem.layanan}"
            } else {
                currentItem.layanan
            }

            // Status Badge (Standardisasi ACC / Soon / Ditunda)
            val currentStatus = when (currentItem.status) {
                "ACC", "Diproses", "Selesai" -> "ACC"
                "Ditunda", "Batal" -> "Ditunda"
                else -> "Soon"
            }
            tvStatusSoonBadge.text = currentStatus

            // Warna Badge Khas List Soon
            val warnaHex = when (currentStatus) {
                "ACC" -> "#00E676"     // Hijau Cerah
                "Soon" -> "#FFC107"    // Kuning Cerah
                "Ditunda" -> "#E53935" // Merah
                else -> "#FFC107"
            }
            btnStatusSoon.backgroundTintList = ColorStateList.valueOf(Color.parseColor(warnaHex))

            // Event Klik Card
            cardSoonItem.setOnClickListener { onItemClick(currentItem) }

            // Event Popup Menu Dropdown Badge (3 Opsi Sesuai Figma)
            btnStatusSoon.setOnClickListener { view ->
                val popup = PopupMenu(context, view)
                popup.menu.add("ACC")
                popup.menu.add("Soon")
                popup.menu.add("Ditunda")

                popup.setOnMenuItemClickListener { menuItem ->
                    val statusBaru = menuItem.title.toString()
                    onStatusChanged(currentItem, statusBaru)
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
}