# Fitur Power-Up Jurus & Animasi Pipa Hancur Berkeping-keping

Rencana implementasi fitur Power-Up interaktif berupa proyektil jurus anime khas tiap pahlawan, mekanika pengambilan item bintang di antara celah pipa, dan efek kehancuran pipa (*pipe shatter debris*) disertai hentakan getar layar (*screen shake*).

---

### User Review & Critical Decisions

> [!IMPORTANT]
> Berdasarkan sesi diskusi dan klarifikasi pilihan Anda:
> - **Tipe Power-Up**: Tembakan proyektil jurus karakter yang menembak maju dan meledakkan pipa di depan.
> - **Gaya Animasi Hancur**: Pipa pecah menjadi bongkahan berkeping-keping (*shattered debris blocks*) dengan efek getar layar (*screen shake*).
> - **Cara Mendapatkan**: Mengambil item bintang emas bersinar (*floating star item*) yang melayang di antara celah pipa.

- **Keputusan Tambahan**:
  - Tombol aksi tembak `[ 💥 JURUS ]` (ukuran sentuh 56dp) akan muncul di layar saat Power-Up aktif, sehingga pemain tetap bisa mengetuk layar untuk mengepakkan sayap (*flap*) secara terpisah tanpa memicu tembakan yang tidak disengaja.
  - Setiap karakter memiliki gaya visual proyektil jurus unik (misal: Tebasan Api Hinokami untuk Tanjiro, Rasengan biru untuk Naruto, Tinju Gomu-Gomu untuk Luffy, Hollow Purple untuk Gojo).

---

### 1. Overview & Core Concept

- **Fitur Utama**:
  1. **Item Bintang Melayang (*Star Collectable*)**: Bintang bersinar dengan efek animasi mengambang lembut (*bobbing motion*) muncul sesekali di celah pipa.
  2. **Mode Jurus Aktif (*Hero Blast Mode*)**: Mengambil bintang mengaktifkan aura karakter bersinar, durasi bar timer (8 detik), amunisi jurus, dan memunculkan tombol aksi tembak `[ 💥 JURUS ]`.
  3. **Proyektil Jurus Karakter (*Hero Projectile*)**: Meluncur cepat ke depan dengan jejak partikel (*trail particles*).
  4. **Penghancuran Pipa (*Shattered Pipe Physics*)**: Saat proyektil mengenai pipa, pipa pecah berkeping-keping menjadi 8–12 bongkahan balok hijau dengan gravitasi dan putaran (*angular rotation*), semprotan percikan api & asap, serta hentakan kamera (*screen shake*). Pipa yang hancur tidak lagi menjadi rintangan mematikan dan memberikan bonus skor.

---

### 2. User Experience & Visual Design

```
+-----------------------------------------------------------+
| [Avatar] [Skor: 14]     [⏸ JEDA]     [⚡ JURUS: 6.2s]      |
|                                                           |
|                     [⭐ Bintang Melayang]                  |
|                                                           |
|       [Karakter Aura] ===> [🔥 Proyektil Jurus]           |
|                                   |                       |
|                                   v                       |
|                        [PIPA PECAH BERKEPING]             |
|                        💥 +5 SMASH!                       |
|                        (Debris Balok Berjatuhan)          |
|                                                           |
|  [Ketuk Layar: Terbang]                 [💥 TEMBAK (5)]   |
+-----------------------------------------------------------+
```

- **Aesthetic Direction**: Arcade Retro Anime Action yang energik, penuh partikel, namun tetap responsif dan jernih (*clear visual hierarchy*).
- **Indikator HUD**:
  - Bar status kuning-oranye neon di bawah skor menunjukkan sisa waktu jurus aktif.
  - Tombol tembak `[ 💥 JURUS ]` dengan badge sisa amunisi di sudut kanan bawah.
- **Efek Hancur (*Shatter & Screen Shake*)**:
  - Kamera bergetar cepat selama 150ms dengan pergeseran acak (±8dp).
  - Pipa dipecah menjadi grid balok 2D yang memiliki kecepatan dorong (*explosion impulse*), gravitasi, dan sudut rotasi.
  - Teks pop-up angka emas `+5 SMASH!` melayang dan memudar ke atas.

---

### 3. Key Product Decisions & Trade-Offs

- **Kontrol Tembakan: Tombol Tembak Terpisah vs Ketukan Layar Ganda**:
  - *Pendekatan*: Menyediakan tombol tembak melayang yang nyaman dijangkau ibu jari kanan saat Power-Up aktif.
  - *Alasan*: Menjaga mekanika terbang (*flapping*) tetap presisi. Jika tembakan digabung dengan ketukan terbang, pemain akan kesulitan mengatur ketinggian burung.
- **Fisika Pipa Hancur: Custom Debris Particles di Canvas**:
  - *Pendekatan*: Menghasilkan pecahan pipa (*debris*) secara procedual di Canvas menggunakan DrawScope, memanfaatkan sistem partikel yang sudah terbukti ringan dan tanpa *memory leak*.
  - *Alasan*: Menghasilkan animasi 60 FPS tanpa lag dan tidak membebani memori Android.

---

### 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────┐
│                   GameViewModel                        │
│ - powerUpState: Active / Inactive / Timer / Ammo       │
│ - starItems: List<PowerUpItem>                         │
│ - projectiles: List<Projectile>                        │
│ - pipeDebris: List<PipeDebris>                         │
│ - pipes: List<PipePair> (dengan status isDestroyed)    │
│ - soundManager.playPowerUp(), playShoot(), playExplode │
└───────────────────────┬────────────────────────────────┘
                        │ UI State Flow
                        ▼
┌────────────────────────────────────────────────────────┐
│                    GameCanvas                          │
│ - drawPowerUpStars() (efek mengambang & kilau)         │
│ - drawProjectiles() (efek laser/rasengan/api)          │
│ - drawPipeDebris() (balok hijau pecah berputar)       │
│ - drawScoreSmashText() (+5 SMASH!)                     │
│ - drawHeroAura() (lingkaran cahaya di burung)          │
└───────────────────────┬────────────────────────────────┘
                        │
                        ▼
┌────────────────────────────────────────────────────────┐
│               GameHud & Controls Layer                 │
│ - Power-Up Timer Bar                                   │
│ - Floating Shoot Button [ 💥 JURUS ]                   │
└────────────────────────────────────────────────────────┘
```

#### Struktur Data Baru:
1. `data class PowerUpItem(val id: Long, var x: Float, var y: Float, val radius: Float, var collected: Boolean = false)`
2. `data class Projectile(val id: Long, var x: Float, var y: Float, val vx: Float, val radius: Float, val color: Color, val trail: List<Offset>)`
3. `data class PipeDebris(var x: Float, var y: Float, var vx: Float, var vy: Float, var rotation: Float, var rotSpeed: Float, val width: Float, val height: Float, val color: Color, var alpha: Float)`
4. Modifikasi `PipePair`: Menambahkan properti `var isDestroyed: Boolean = false`. Pipa yang hancur tidak lagi memicu tabrakan mematikan dan digantikan oleh partikel pecahan *debris*.
