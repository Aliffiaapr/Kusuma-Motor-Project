package com.kusumamotors.admin.model

data class Antrean(
    var id: String = "",
    var namaPelanggan: String = "",
    var nomorWhatsapp: String = "",
    var platNomor: String = "",
    var jenisUnit: String = "",
    var waktuServis: String = "",
    var layanan: String = "", // "On-Site" atau "Home Service"
    var status: String = "",   // "Pending", "Diproses", "Ditunda", atau "Selesai"
    var catatan: String = "",
    var alamat: String = "",    // Khusus Home Service
    var tanggalReservasi: String = "", // <-- TAMBAHKAN INI (Agar Dashboard tidak merah)
    var waktuDibuat: String = ""       // <-- TAMBAHKAN INI (Untuk histori pencatatan)
)