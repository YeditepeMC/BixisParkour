# BixisParkour — Claude Context

## Proje
YeditepeMC lobi parkur sistemi. Paper 26.1.2, Java 25, Maven.
BixisCore'a depend eder (ileride — şimdilik records.json kullan).

## Mimari
- BixisParkourPlugin.java — ana plugin
- manager/ParkourManager.java — parkur ve checkpoint yönetimi
- manager/SessionManager.java — aktif oyuncu oturumları
- listener/ParkourListener.java — event handler'lar
- model/Parkour.java — parkur verisi
- model/Checkpoint.java — checkpoint verisi
- model/ParkourSession.java — aktif oturum
- storage/ParkourStorage.java — JSON okuma/yazma
- hologram/LeaderboardHologram.java — DecentHolograms entegrasyonu
- util/TimeFormatter.java — süre formatlama

## Checkpoint Sistemi
Event bazlı — PlayerInteractEvent ile pressure plate basıldığında tetiklenir.
Her pressure plate tipi geçerlidir (STONE, HEAVY, LIGHT vs.)
Koordinat bazlı kayıt: admin o pressure plate'in konumunu checkpoint olarak kaydeder.
Event'te: basılan pressure plate'in koordinatı kayıtlı mı? → evet ise işle.
Bu yaklaşım PlayerMoveEvent'e göre çok daha verimli (O(1) event başına).

## Veri Modeli

### Parkour
- id (String)
- name (String, display name)
- start (Checkpoint, index=-1) — setstart plate'i, basılınca parkur başlar
- checkpoints (List<Checkpoint>, sıralı, index=0..n, son = bitiş)
- bestTimes (Map<UUID, Long> milisaniye cinsinden)

### Checkpoint  
- index (int; -1 = başlangıç, 0'dan başlar progression, son isEnd=true)
- location (Location, pressure plate'in konumu)
- isEnd (boolean)

### ParkourSession
- playerUUID
- parkourId
- currentCheckpointIndex (int, başlangıçta -1 = AT_START)
- startTime (long, System.currentTimeMillis())
- timerTaskId (int, ActionBar timer görevi; -1 = yok)

## Komutlar (permission: bixisparkour.admin, default: op)
/parkour create <id> <isim> — yeni parkur oluştur
/parkour delete <id> — parkur sil (tüm hologramları da kaldırır)
/parkour setstart <id> — üzerinde durduğun plate'i BAŞLANGIÇ yap
/parkour addcheckpoint <id> — üzerinde durduğun pressure plate'i checkpoint ekle
/parkour setend <id> — üzerinde durduğun pressure plate'i bitiş olarak işaretle
/parkour placeleaderboard <id> — bulunduğun konuma leaderboard hologramı koy
/parkour removehologram <id> — leaderboard hologramını kaldır
/parkour list — tüm parkurları listele
/parkour info <id> — checkpoint listesi ve rekorlar
/parkour tp <id> <checkpoint_no|start> — checkpoint'e ışınlan
/parkour reset <oyuncu> <id> — oyuncunun rekorunu sıfırla

## Oyuncu Komutları (permission: bixisparkour.play, default: true)
/p leave — parkurdan çık (session sil)
/p checkpoint — son geçilen checkpoint'e ışınlan
(Parkura başlama komutla DEĞİL, setstart plate'ine basarak yapılır.)

## Oyun Akışı
1. Oyuncu SETSTART plate'ine basar (PHYSICAL):
   - Zaten parkurdaysa → uyarı, hiçbir şey yapma
   - Değilse başlatma sırası:
     a. console: gmenu reset all <oyuncu>
     b. console: bixisnav toggle <oyuncu>
     c. Parkur hotbar item'ları ver (slot 0 tüy, slot 8 barrier)
     d. Session + timer başlat
     e. ActionBar timer göster
     f. Ses: ENTITY_EXPERIENCE_ORB_PICKUP
2. Oyuncu checkpoint plate'ine basar → sıradaki mi? → session güncelle
   - Ses: BLOCK_NOTE_BLOCK_PLING (pitch 1.2)
3. isEnd plate → süre hesapla → rekor mu? → kaydet → cleanup
   - Ses: UI_TOAST_CHALLENGE_COMPLETE
4. /p leave veya bitiş → cleanup:
   - Hotbar item'ları kaldır (slot 0 ve 8)
   - console: bixisnav toggle <oyuncu>
   - timer iptal + session sil

## Parkur Hotbar (util/ParkourItems)
PDC anahtarı: "bixisparkour:item" (değer = CHECKPOINT | LEAVE)
- Slot 0: FEATHER "&bSon Checkpoint" → sağ tık: son checkpoint'e ışınla
- Slot 8: BARRIER "&cParkuru Terk Et" → sağ tık: /p leave mantığı
Koruma (listener): PlayerDropItem, InventoryClick, InventoryDrag iptal.

## ActionBar Timer
Session başlayınca her saniye (20 tick) ActionBar:
"&e⏱ &f" + TimeFormatter.format(elapsed)
BukkitRunnable ile; taskId ParkourSession'da saklanır; leave/bitişte iptal.

## Hologramlar
Marker'lar (komutla otomatik, ilgili plate üzerinde):
- setstart → "parkour_start_<id>": "&6&l<isim>" + "&a▶ BAŞLA!"
- addcheckpoint → "parkour_cp_<id>_<n>": "&e⬛ Checkpoint #<n>"
- setend → "parkour_end_<id>": "&c&l⬛ BİTİŞ"

Leaderboard "parkour_lb_<id>" (SADECE /parkour placeleaderboard ile):
Top 5, rekor kırılınca otomatik güncellenir.
"&6&l⏱ <parkur ismi>"
"&8&m──────────────"
"&e#1 &f<isim> &7- &a00:32.451"
... (boşsa "&8(Henüz rekor yok)")

## Süre Formatı
mm:ss.SSS (örn. 01:23.456)
TimeFormatter.format(long millis) static metod

## Storage
- plugins/BixisParkour/parkours.json
- plugins/BixisParkour/records.json

## Performans
PlayerInteractEvent → sadece pressure plate action'da çalış
(Action.PHYSICAL ile kontrol et)
Kayıtlı koordinat HashMap'i ile O(1) lookup.

## DecentHolograms
softdepend olarak ekle.
API: DecentHologramsAPI.get().getHologramManager()
Hologram ismi: "parkour_lb_<parkourId>"

## Dil
Tüm oyuncu mesajları Türkçe.

## Notlar
- Jar hiçbir yere kopyalanmayacak
- Oyuncu parkurdayken quit ederse session silinir
- Aynı anda birden fazla parkurda olamazsın
- addcheckpoint: oyuncunun baktığı bloğu değil, ayakta durduğu bloğun
  1 altındaki bloğu kaydet (pressure plate'in kendisi)
- Build: JAVA_HOME=C:\Program Files\Java\jdk-26.0.1
