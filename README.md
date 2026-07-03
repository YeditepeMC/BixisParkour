# BixisParkour
Feature-rich parkour plugin for YeditepeMC lobby.

## Features
- Pressure plate based checkpoint system (event-driven, O(1) lookup)
- Per-run leaderboard with DecentHolograms integration
- Parkour hotbar (BixisNavigator API integration)
- GadgetsMenu cosmetic reset on parkur start
- ActionBar timer (mm:ss)
- yaw/pitch preserved on checkpoint teleport
- Active flag — parkur must have end point before players can start

## Commands
### Admin (bixisparkour.admin)
| Komut | Açıklama |
|-------|----------|
| /parkour create <id> <isim> | Yeni parkur oluştur |
| /parkour setstart <id> | Başlangıç plakasını ayarla |
| /parkour addcheckpoint <id> | Checkpoint ekle |
| /parkour setend <id> | Bitiş plakasını ayarla (parkuru aktifleştirir) |
| /parkour placeleaderboard <id> | Leaderboard hologramını yerleştir |
| /parkour removehologram <id> | Leaderboard hologramını kaldır |
| /parkour delete <id> | Parkuru sil |
| /parkour list | Tüm parkurları listele |
| /parkour info <id> | Parkur detayları |
| /parkour tp <id> <n> | Checkpoint'e ışınlan |
| /parkour reset <oyuncu> <id> | Oyuncunun rekorlarını sıfırla |

### Oyuncu (bixisparkour.play)
| Komut | Açıklama |
|-------|----------|
| /p leave | Parkurdan çık |
| /p checkpoint | Son checkpoint'e ışınlan |

## Requirements
- Paper 26.1.2
- Java 25
- BixisNavigator 1.1.0+ (softdepend)
- DecentHolograms 2.8.12+ (softdepend)

## Installation
1. Drop BixisParkour.jar into plugins/
2. Restart server
3. /parkour create <id> <isim>
4. Stand on pressure plate → /parkour setstart <id>
5. Add checkpoints and set end
6. /parkour placeleaderboard <id>