// Import SDK Firebase Realtime Database
import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-app.js";
import { getDatabase, ref, push, onValue } from "https://www.gstatic.com/firebasejs/10.12.2/firebase-database.js";

// ========================================================
// KONFIGURASI FIREBASE
// ========================================================
const firebaseConfig = {
  apiKey: "AIzaSyDnAat9eADWe6NgGCWRnm9rBtT9hyF1mF0",
  authDomain: "kusuma-motors.firebaseapp.com",
  databaseURL: "https://kusuma-motors-default-rtdb.asia-southeast1.firebasedatabase.app",
  projectId: "kusuma-motors",
  storageBucket: "kusuma-motors.firebasestorage.app",
  messagingSenderId: "948628013371",
  appId: "1:948628013371:web:9b1c8b5e373e48ae8f371e",
  measurementId: "G-L4VVED6ZR5"
};

// Inisialisasi Firebase App & Database
const app = initializeApp(firebaseConfig);
const db = getDatabase(app);

// Variabel Penampung Global
let latitude = null;
let longitude = null;
let masterJamList = [];
let allReservasiData = [];

// Element Handles
const btnLokasi = document.getElementById("btnLokasi");
const statusLokasi = document.getElementById("statusLokasi");
const formReservasi = document.getElementById("formReservasi");
const layananSelect = document.getElementById("layanan");
const sectionLokasi = document.getElementById("sectionLokasi");
const inputTanggal = document.getElementById("tanggalReservasi");
const selectJam = document.getElementById("jamServis");
const btnSubmit = document.getElementById("btnSubmit");

// 1. SETTING DEFAULT TANGGAL
function updateTanggalBerdasarkanLayanan(layanan) {
    const hariIni = new Date();
    const besok = new Date();
    besok.setDate(hariIni.getDate() + 1);

    const strHariIni = hariIni.toISOString().split('T')[0];
    const strBesok = besok.toISOString().split('T')[0];

    if (layanan === "Home Service") {
        inputTanggal.value = strBesok;
        inputTanggal.min = strBesok;
        sectionLokasi.style.display = "block";
    } else {
        inputTanggal.value = strHariIni;
        inputTanggal.min = strHariIni;
        sectionLokasi.style.display = "none";
    }
    renderJamOptions();
}

// 2. RENDERING DROPDOWN JAM (WITH DISABLED CHECK)
function renderJamOptions() {
    selectJam.innerHTML = '<option value="">-- Pilih Jam Servis --</option>';

    const tglDipilih = inputTanggal.value;

    // Filter jam yang SUDAH DIBOOKING pada tanggal yang dipilih (dan status bukan "Batal")
    const bookedHours = new Set();
    allReservasiData.forEach((res) => {
        if (res.tanggalReservasi === tglDipilih && res.status !== "Batal") {
            const jam = res.jamServis || res.waktuServis;
            if (jam) bookedHours.add(jam);
        }
    });

    const jamSource = masterJamList.length > 0 
        ? masterJamList 
        : ["07:00 WIB", "09:00 WIB", "11:00 WIB", "13:00 WIB", "15:00 WIB"];

    jamSource.forEach((jam) => {
        const option = document.createElement("option");
        option.value = jam;

        if (bookedHours.has(jam)) {
            // JIKA SUDAH DIBOOKING: DISABLE & GREY OUT
            option.textContent = `${jam} (Penuh / Sudah Di-booking)`;
            option.disabled = true;
            option.style.color = "#9ca3af";
            option.style.backgroundColor = "#f3f4f6";
        } else {
            option.textContent = jam;
        }

        selectJam.appendChild(option);
    });
}

// 3. LISTEN FIREBASE SETTINGS (JAM OPERASIONAL & TOKO OPEN/CLOSE)
const settingsRef = ref(db, 'settings');
onValue(settingsRef, (snapshot) => {
    if (snapshot.exists()) {
        const dataSettings = snapshot.val();
        
        const isOpen = dataSettings.isOpen ?? true;
        if (!isOpen) {
            btnSubmit.disabled = true;
            btnSubmit.innerText = "Bengkel Sedang Libur / Tutup";
        } else {
            btnSubmit.disabled = false;
            btnSubmit.innerText = "Kirim Reservasi Sekarang";
        }

        const rawJam = dataSettings.jamOperasional;
        if (Array.isArray(rawJam)) {
            masterJamList = rawJam;
        } else if (typeof rawJam === 'object' && rawJam !== null) {
            masterJamList = Object.values(rawJam);
        }
    }
    renderJamOptions();
});

// 4. LISTEN FIREBASE RESERVASI (UNTUK PROSES CEK JAM PENUH)
const reservasiRef = ref(db, 'reservasi');
onValue(reservasiRef, (snapshot) => {
    allReservasiData = [];
    if (snapshot.exists()) {
        snapshot.forEach((child) => {
            allReservasiData.push(child.val());
        });
    }
    renderJamOptions();
});

// EVENT LISTENERS
updateTanggalBerdasarkanLayanan(layananSelect.value);

layananSelect.addEventListener("change", (e) => {
    updateTanggalBerdasarkanLayanan(e.target.value);
});

inputTanggal.addEventListener("change", () => {
    renderJamOptions();
});

// FITUR LBS (GPS)
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
            },
            { enableHighAccuracy: true }
        );
    } else {
        statusLokasi.innerText = "Browser tidak mendukung fitur GPS.";
    }
});

// SUBMIT FORM
// SUBMIT FORM
formReservasi.addEventListener("submit", (e) => {
    e.preventDefault();

    // 1. Validasi Home Service & GPS
    if (layananSelect.value === "Home Service" && (!latitude || !longitude)) {
        alert("Silakan klik tombol 'Ambil Titik Lokasi Saya' terlebih dahulu untuk layanan Home Service!");
        return;
    }

    // 2. Validasi Pilihan Jam
    if (!selectJam.value) {
        alert("Silakan pilih Jam Servis yang masih tersedia!");
        return;
    }

    // 3. VALIDASI DUPILKASI (TARUH DI SINI): Cek Plat Nomor & Tanggal yang Sama
    const inputPlat = document.getElementById("platNomor").value.toUpperCase().trim();
    const inputTgl = inputTanggal.value;

    const isAlreadyBooked = allReservasiData.some(res => 
        res.platNomor === inputPlat && 
        res.tanggalReservasi === inputTgl && 
        (res.status === "Pending" || res.status === "Diproses")
    );

    if (isAlreadyBooked) {
        alert(`⚠️ Plat nomor ${inputPlat} sudah memiliki jadwal reservasi aktif pada tanggal ${inputTgl}.\nSilakan tunggu proses dari bengkel atau pilih tanggal lain.`);
        return; // Menghentikan proses agar tidak tersimpan ke Firebase
    }

    // 4. BUAT OBJEK DATA (Ditaruh setelah semua validasi LULUS)
    const dataReservasi = {
        nama: document.getElementById("nama").value,
        whatsapp: document.getElementById("whatsapp").value,
        platNomor: inputPlat,
        jenisMobil: document.getElementById("jenisMobil").value,
        layanan: layananSelect.value,
        tanggalReservasi: inputTgl,
        jamServis: selectJam.value,
        waktuServis: selectJam.value,
        latitude: latitude,
        longitude: longitude,
        keluhan: document.getElementById("keluhan").value,
        waktuDibuat: new Date().toLocaleString("id-ID"),
        status: "Pending"
    };

    // 5. KIRIM KE FIREBASE
    push(reservasiRef, dataReservasi)
        .then(() => {
            alert(`🎉 Reservasi Berhasil Terkirim!\nJadwal: ${dataReservasi.tanggalReservasi} jam ${dataReservasi.jamServis}.\nPihak Kusuma Motors akan menghubungi Anda via WhatsApp.`);
            formReservasi.reset();
            updateTanggalBerdasarkanLayanan(layananSelect.value);
            statusLokasi.innerText = "Lokasi belum diambil";
            latitude = null;
            longitude = null;
        })
        .catch((error) => {
            alert("Gagal mengirim reservasi: " + error.message);
        });
});