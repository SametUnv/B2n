# Board2Notes — Frontend Prototip

Tahta fotoğraflarını okunabilir notlara dönüştüren, **yapay zekâ destekli eğitim uygulaması** konseptinin
Jetpack Compose ile yazılmış çalışan frontend prototipidir.

> ⚠️ Bu sürümde **gerçek OCR / görüntü işleme / yapay zekâ yoktur.** Tüm sonuçlar mock veridir.
> Mimari, ileride **FastAPI** backend'i geldiğinde minimum değişiklikle gerçek sisteme bağlanacak şekilde tasarlanmıştır.

## Teknolojiler
- Kotlin + Jetpack Compose (Material 3)
- MVVM + Navigation Compose
- Paylaşılan `ViewModel` + `StateFlow`
- Coil (görsel yükleme)
- Manuel DI (`AppModule`) — Hilt'e geçişe uygun
- Mock repository (`MockBoardRepository`)

## Açma / Çalıştırma
1. Android Studio (Koala veya üzeri) ile **klasörü** aç.
2. Gradle senkronizasyonunu bekle (ilk seferde bağımlılıklar iner).
3. Bir emulator ya da cihaz seçip **Run**'a bas.

Gereksinimler: JDK 17, Android SDK 35, minSdk 24.

## Ekran Akışı
Welcome → Home → Preview → Processing → Enhancement → (OCR Note | Visual Note) → Export

Kamera ve galeri seçimi **gerçektir** (FileProvider + Activity Result API).
"İyileştirme" görseli, mock olduğu için orijinal görsele bir renk filtresi (taranmış görünüm) uygulanarak gösterilir.

## Klasör Yapısı
```
com.board2notes.app
├── core/ui (theme, components)   → tema + tekrar kullanılan bileşenler
├── core/util                     → FileProvider, Share yardımcıları
├── data/mock                     → sahte veri
├── data/repository               → MockBoardRepository
├── data/remote                   → FastAPI için yer tutucu (ApiConfig)
├── domain/model|repository|usecase
├── di/AppModule.kt               → bağımlılık sağlayıcı (tek nokta)
├── presentation/<ekran>          → her ekranın Composable'ı
├── presentation/shared           → BoardViewModel + BoardUiState
└── presentation/navigation       → rotalar + NavGraph
```

## İleride Backend (FastAPI) Bağlama
Mimari değişmeden 3 adım:
1. `data/remote` altında Retrofit/Ktor ile `BoardApi`'yi tanımla (örnek imzalar `ApiConfig.kt` yorumlarında).
2. `BoardRepository`'yi uygulayan `RemoteBoardRepository` yaz (DTO → domain model dönüşümü).
3. `di/AppModule.kt` içinde tek satırı değiştir:
   ```kotlin
   private val repository: BoardRepository = RemoteBoardRepository(api)
   ```
ViewModel ve UI katmanında **hiçbir değişiklik gerekmez.**

Emulator → bilgisayardaki localhost adresi: `http://10.0.2.2:8000/`

## Font Notu
Spec'te Inter/Poppins isteniyor. Proje sorunsuz derlensin diye şu an sistem fontu kullanılıyor.
Geçiş için `core/ui/theme/Type.kt` dosyasının başındaki açıklamaya bak (res/font/ veya Google Fonts).

## Giriş / Login
Şu an login yok; uygulama doğrudan kullanılır.
Mimari, ileride Google Sign-In eklemek için uygundur (auth katmanı `data`/`domain`'e eklenebilir,
navigasyonda Welcome öncesi bir auth grafiği açılabilir).
