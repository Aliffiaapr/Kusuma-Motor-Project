package com.kusumamotors.admin.model

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Antrean(
    var id: String = "",
    var nama: String = "",
    var namaPelanggan: String = "",
    var whatsapp: String = "",
    var nomorWhatsapp: String = "",
    var platNomor: String = "",
    var jenisMobil: String = "",
    var jenisUnit: String = "",
    var waktuServis: String = "",
    var jamServis: String = "",
    var layanan: String = "",
    var status: String = "",
    var keluhan: String = "",
    var catatan: String = "",
    var alamat: String = "",
    var latitude: Double? = null,
    var longitude: Double? = null,
    var tanggalReservasi: String = "",
    var waktuDibuat: String = ""
) {

    // Helper Fallback (kompatibel untuk data web maupun input manual Android)
    val displayNama: String get() = namaPelanggan.ifEmpty { nama }
    val displayUnit: String get() = jenisUnit.ifEmpty { jenisMobil }
    val displayWa: String get() = nomorWhatsapp.ifEmpty { whatsapp }
    val displayCatatan: String get() = catatan.ifEmpty { keluhan }
    val displayJam: String get() = jamServis.ifEmpty { waktuServis }

    // Helper Fitur Home Service & GPS
    val isHomeService: Boolean get() = layanan.equals("Home Service", ignoreCase = true)
    val hasGpsLocation: Boolean get() = latitude != null && longitude != null
}