// Import SDK Firebase Realtime Database
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getDatabase, ref, push } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-database.js";

// ========================================================
// KONFIGURASI FIREBASE (SESUAIKAN DENGAN API KEY MILIKMU)
// ========================================================
const firebaseConfig = {
    apiKey: "PASTE_API_KEY_KAMU",
    authDomain: "kusuma-motors.firebaseapp.com",
    databaseURL: "https://kusuma-motors-default-rtdb.asia-southeast1.firebasedatabase.app",
    projectId: "kusuma-motors",
    storageBucket: "kusuma-motors.firebasestorage.app",
    messagingSenderId: "948628013371",
    appId: "PASTE_APP_ID_KAMU"
};

// Inisialisasi Firebase App & Database
const app = initializeApp(firebaseConfig);
const db = getDatabase(app);

// Variabel penampung koordinat lokasi GPS
let latitude = null;
let longitude = null;

// Mengambil Element HTML berdasarkan ID
const btnLokasi = document.getElementById("btnLokasi");
const statusLokasi = document.getElementById("statusLokasi");
const formReservasi = document.getElementById("formReservasi");
const layananSelect = document.getElementById("layanan");
const sectionLokasi = document.getElementById("sectionLokasi");

// 1. Tampilkan/Sembunyikan Box GPS berdasarkan Jenis Layanan
layananSelect.addEventListener("change", (e) => {
    if (e.target.value === "Home Service") {
        sectionLokasi.style.display = "block";
    } else {
        sectionLokasi.style.display = "none";
    }
});

// 2. Fungsi Fitur LBS (Location-Based Service) - Mengambil GPS Browser
btnLokasi.addEventListener("click", () => {
    if (navigator.geolocation) {
        statusLokasi.innerText = "Mendeteksi titik lokasi...";
        navigator.geolocation.getCurrentPosition(
            (position) => {
                latitude = position.coords.latitude;
                longitude = position.coords.longitude;
                statusLokasi.innerHTML = `<span style="color: #16a34a; font-weight: bold;">✓ Lokasi berhasil diambil!</span><br>(${latitude.toFixed(5)}, ${longitude.toFixed(5)})`;
            },
            (error) => {
                statusLokasi.innerText = "❌ Gagal mengambil lokasi. Izinkan akses GPS browser Anda.";
            }
        );
    } else {
        statusLokasi.innerText = "Browser tidak mendukung fitur GPS.";
    }
});

// 3. Fungsi Kirim Data Reservasi ke Firebase Database
formReservasi.addEventListener("submit", (e) => {
    e.preventDefault();

    // Validasi: Jika Home Service, wajib klik tombol GPS dulu
    if (layananSelect.value === "Home Service" && (!latitude || !longitude)) {
        alert("Silakan klik tombol 'Ambil Titik Lokasi Saya' terlebih dahulu untuk layanan Home Service!");
        return;
    }

    // Menghitung tanggal jadwal servis H+1
    const besok = new Date();
    besok.setDate(besok.getDate() + 1);
    const tanggalReservasi = besok.toISOString().split('T')[0];

    // Objek Data Reservasi (JSON Schema)
    const dataReservasi = {
        nama: document.getElementById("nama").value,
        whatsapp: document.getElementById("whatsapp").value,
        platNomor: document.getElementById("platNomor").value.toUpperCase(),
        jenisMobil: document.getElementById("jenisMobil").value,
        layanan: layananSelect.value,
        latitude: latitude,
        longitude: longitude,
        keluhan: document.getElementById("keluhan").value,
        tanggalReservasi: tanggalReservasi,
        waktuDibuat: new Date().toLocaleString("id-ID"),
        status: "Pending"
    };

    // Push data ke node 'reservasi'
    const dbRef = ref(db, 'reservasi');
    push(dbRef, dataReservasi)
        .then(() => {
            alert("🎉 Reservasi Berhasil Terkirim!\nJadwal servis diset untuk esok hari (H+1). Pihak Kusuma Motors akan menghubungi Anda via WhatsApp.");
            formReservasi.reset();
            statusLokasi.innerText = "Lokasi belum diambil";
            latitude = null;
            longitude = null;
        })
        .catch((error) => {
            alert("Gagal mengirim reservasi: " + error.message);
        });
});