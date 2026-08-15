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
    var layanan: String = "",
    var status: String = "",
    var keluhan: String = "",
    var catatan: String = "",
    var alamat: String = "",
    var tanggalReservasi: String = "",
    var waktuDibuat: String = ""
) {

    val displayNama: String get() = namaPelanggan.ifEmpty { nama }
    val displayUnit: String get() = jenisUnit.ifEmpty { jenisMobil }
    val displayWa: String get() = nomorWhatsapp.ifEmpty { whatsapp }
    val displayCatatan: String get() = catatan.ifEmpty { keluhan }
}