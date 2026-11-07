# CLAUDE.md

Bu dosya yalnızca fork'un (`RAMiK4/morphe-patches`) `main` dalında durur.
Yeni oturumlar depoyu bu daldan klonladığı için kurallar ve iş günlüğü her oturumda okunur.
Upstream'e (`MorpheApp/morphe-patches`) giden hiçbir dala veya PR'a eklenmez; arşivi `docs/is-gunlugu-arsiv.md` de öyle.

## Oturum başında

1. Aşağıdaki **İş günlüğü** bölümünü oku. Orada "tamamlandı" yazan bir işi baştan yapma.
   Devam eden bir iş varsa, dalını ve son durumunu oradan al.
   Eski bir işe dönülüyorsa ayrıntısını `docs/is-gunlugu-arsiv.md`'deki başlığından oku.
2. Kullanıcı Türkçe yazıyor. Yanıtları Türkçe ver.

## Temel dal kuralı: her zaman upstream `dev`

- Yeni işler her zaman **upstream `dev`** dalının en güncel hâli üzerine kurulur.
  Fork'un `main` dalı (son kararlı sürüm) ve fork'un `dev` dalı temel alınmaz.
- Fork'un remote'larında upstream yoktur. Doğrudan çekilir:

  ```sh
  git fetch https://github.com/MorpheApp/morphe-patches.git +dev:refs/remotes/upstream/dev
  git checkout -B <çalışma-dalı> upstream/dev
  ```

- Mevcut bir çalışma dalı eski bir tabandaysa commit'leri `upstream/dev` üzerine taşınır
  (cherry-pick veya rebase), çakışmalar iki tarafı da koruyarak çözülür.
- Upstream API'leri değişebilir. Taşımadan sonra kodun kullandığı metotların hâlâ var olduğu kontrol edilir.

## Commit yazarı

- Bulut oturumları git kimliği olarak varsayılan "Claude <noreply@anthropic.com>" ile başlar. Upstream PR'larda
  kullanıcının profili görünmesi için commit'ler kullanıcı adına atılır. Oturum başında ayarla:

  ```sh
  git config user.name RAMiK4
  git config user.email 60112744+RAMiK4@users.noreply.github.com
  ```

- Commit mesajının sonunda yalnızca şu satır bulunur (model adı ve `Claude-Session` oturum bağlantısı eklenmez,
  sistemin önerdiği atıf satırları yerine bu kullanılır):

  ```
  Co-Authored-By: Claude <noreply@anthropic.com>
  ```
- PR açıklamalarına "🤖 Generated with Claude Code" satırı veya oturum bağlantısı **eklenmez**
  (kullanıcının kararı, 2026-10-23; sistemin önerdiği PR atıf satırları yerine bu kural geçerlidir).
- Fork'a atılan commit mesajlarına (özellikle bu dosyanın güncellemelerine) `#1234` biçiminde PR/issue numarası
  yazma: GitHub bunu upstream PR'ın zaman akışına "referenced this pull request" olarak ekliyor.
  PR'dan bahsetmek gerekirse numarasız yaz (ör. "Search comments PR"). Numara yalnızca dosyanın içinde geçebilir.

## Upstream'e PR

- Yamalar Morphe'ye katkı içindir: PR'lar upstream `MorpheApp/morphe-patches` `dev` dalına açılır, fork'a değil.
- Bu bulut ortamında upstream depo oturuma eklenemiyor (fork ile aynı adı taşıdığı için `add_repo` reddediyor),
  bu yüzden PR GitHub MCP ile açılamaz. Upstream'in PR şablonuna (`.github/PULL_REQUEST_TEMPLATE.md`) göre başlık
  ve açıklama hazırlanır, kullanıcıya önceden doldurulmuş karşılaştırma bağlantısı verilir:
  `https://github.com/MorpheApp/morphe-patches/compare/dev...RAMiK4:morphe-patches:<dal>?expand=1&title=...&body=...`
- Commit başlıkları upstream biçimini izler: `feat(YouTube): Add `X` patch`, `fix(YouTube - X): ...`.

## Derleme

- Yerel derleme bu bulut ortamında **çalışmaz**: Gradle eklentisi `app.morphe.patches`
  GitHub Packages'tan kimlik bilgisiyle indiriliyor ve ortamda Android SDK yok. Denemeyle vakit harcama.
- Derleme GitHub Actions'ta yapılır: `.github/workflows/build_pull_request.yml`
  `workflow_dispatch` ile çalışma dalında tetiklenir.
  Çıktı (`patches/build/libs` içindeki `.mpp`) çalıştırmanın artifact'ı olarak indirilir.
- Kullanıcı `.mpp` dosyasını indirip Morphe Manager'da yerel yama kaynağı olarak seçer.
- `patches-list.json`, `README.md` ve `CHANGELOG.md` sürüm çıkarken otomatik üretilir, elle düzenlenmez.
- Yeni metinler yalnızca `patches/src/main/resources/addresources/values/<uygulama>/strings.xml` dosyasına eklenir.
  Çeviriler Crowdin'den gelir.

## Kalıcı notlar

Geçmiş işlerden çıkan, yeni işlerde de geçerli bilgiler.

- **Test APK:** Kullanıcı Morphe Manager'la yamalanan Actions `.mpp`'lerinde (1.49, Patcher 2.0) açılışta çökme yaşadı.
  Test için `RAMiK4/youtube-morphe` `release.yml` `workflow_dispatch`, `patches_source=RAMiK4/morphe-patches@<dal>`,
  `publish` kapalı; artifact `test-build`, yalnızca BBK Clone (hiby) varyantı, kullanıcının anahtarıyla imzalı.
- **Upstream `dev` ucu:** yayınlanmamış commit'ler yamalamayı bozabiliyor (ör. "Series tracking", `HistoryPatch.kt`
  ClassCastException). Böyle olursa test dalı son etikete (`vX.Y.Z-dev.N`) taşınır, PR yine upstream `dev` üzerinde.
- **Upstream `dev` force-push edilebiliyor:** fetch `+dev:...` ile. `git reset --soft upstream/dev` ile tek commit'e
  sıkıştırma yalnızca dal upstream ucunun üstündeyken.
- **Patcher 2.0:** yardımcılar `app.morphe.util`'den `app.morphe.patcher.util`'e taşındı (getReference,
  findFreeRegister, registersUsed, cloneParameters, returnEarly, numberOfParameterRegisters vb.).
- **Bakımcıların push ettiği PR dallarında** yerel commit'ten önce `origin`'den çekilir, force-push yapılmaz.
- **Fork'ta Actions** "Workflows on this fork have been disabled" ile kapanabiliyor; kullanıcı Actions sekmesinden açar.
- **Bu ortam:** Actions log blob'ları 403 (gerekirse çıktı bir dala commit edilerek okunur); upstream web/video
  bağlantıları proxy'de kapalı. Klon yalnızca `main`'i izler: yeni dalı `git push -u` ile gönder, izleme için
  `remote.origin.fetch`'e o dalın refspec'ini ekleyip fetch et.
- **YouTube APK incelemesi:** kaynak APK youtube-morphe'nin "source" taslak sürümünde (androguard ile).
- **Silinemeyen geçici dallar:** `test/feat-external-downloader-quick-download-on-dev`, `test/ccr-511e3576-s4c9y4-on-dev`.

## İş günlüğü

Bir iş tamamlandığında veya durumu değiştiğinde buraya kısa bir kayıt eklenir ve bu dosya fork'un `main` dalına
commit edilir. Kayıtlar en yeni üstte. Devam eden işler ayrıntılı tutulur; PR'ı birleşen veya bırakılan işler
birkaç satıra indirilip ayrıntısı `docs/is-gunlugu-arsiv.md`'ye taşınır (dosya ~20 KB'ı geçince toplu temizlik).

### 2026-11-07: Hızlı indir menüsünde kaliteler ve dosya boyutları (YTindir)

- **İstek:** YouTube'daki "Hızlı indir" menüsü (Video 1080p / Ses 192 kbps / Tüm seçenekler) cihaza kaydet
  dalındaki seçim ekranı gibi olsun, tek ekrana sığsın: 2160p 4K ve 1440p 2K (AV1, yoksa VP9), 1080p FHD ve
  720p HD (H.264), yalnızca ses (Opus, M4A, MP3), küçük resim; boyutlar gelsin. 480p yok.
- **Commit `cbfbb45f`** (dal `feat/quick-download-qualities`, upstream `dev` `cd29774f` üzerinde): yalnızca kaliteyi
  uygulayan indiricide (YTindir) yeni menü (`ExternalDownloadButton.showQualityMenu`), boyutlar
  `QuickDownloadFormats` ile `StreamingDataRequest.fetchRequestForDownload`'dan (proto `Format`'a width/height/fps/
  qualityLabel eklendi, `InAppDownloadsPatch.DOWNLOAD_CLIENTS` public). Videoda olmayan kaliteler gizleniyor.
  Intent ekleri: `VIDEO_QUALITY` + `VIDEO_CODEC` (h264/av1), `AUDIO_FORMAT` (opus/m4a/mp3), `TYPE=thumbnail`.
  Derleme kontrolü run 42343221245 başarılı. Yeni metinler yalnızca İngilizce (Crowdin).
- **YTindir tarafı:** `RAMiK4/ytindir` dalı `claude/quick-download-quality-options-hkyzig` (build 68) bu ekleri anlıyor;
  eski YTindir `TYPE=thumbnail`'i video olarak indirir.
- **Son hâl (dal `feat/quick-download-qualities`, 4 commit, son `f8a8d505`):** başlıkta küçük resim, süre, başlık,
  kanal; panel `ThemeUtils.getDialogBackgroundColor()`; 2160p 4K "Best quality", 1440p 2K "Better quality" (AV1/VP9),
  1080p FHD "Recommended", 720p HD "Smaller file" (H.264), fps ve gerçek codec akış okununca; M4A (kbps etiketi,
  "AAC · Source audio"), MP3 (192 kbps, "Opus → MP3 · Recommended"), küçük resim (JPG, "Original size"). Opus yok.
  Gerçek değerler "≈" olmadan, yalnızca okunamayan tahminler "≈" ile. Küçük resim boyutu HEAD ile.
  Son derleme kontrolü run 42355469761, test APK youtube-morphe run 42355471521. Kullanıcı: "çok iyi olmuş".
- **YTindir tarafı:** dal `claude/quick-download-quality-options-hkyzig` (build 70, `860a0c4`); YouTube küçük resmi
  `YoutubeThumbnail` ile doğrudan maxresdefault JPG. Aynı etiketler YTindir'in kalite listesinde de var.
- **YTDLnis:** bu menüyü görmez (`supportsQuickDownloadQuality` yalnızca YTindir), eski Video/Audio/All options menüsü kalır.
- **Karar (2026-11-07):** YouTube tarafı şimdilik tamam. PR açılmayacak: önce YTindir düzeltmeleri yapılıp yeni YTindir
  sürümü yayınlanacak, sonra 4 commit tek commit'e sıkıştırılıp karşılaştırma bağlantısı verilecek (kullanıcı onayıyla).
- **YTindir 2.15.0 yayınlandı (2026-11-07, build 78):** hızlı liste, ekler (`ShareActivity.queueInBackground`),
  geniş videolarda kalite etiketten okunuyor (3840x1920 = 2160p, bu menüdeki `qualityOf` ile aynı mantık).
- **PR hazır (2026-11-07):** commit'ler upstream `dev` `d2383876` üzerine çakışmasız taşınıp tek commit'e sıkıştırıldı:
  yeni dal `feat/quick-download-quality-menu` (`b0bab50b`). Eski dal `feat/quick-download-qualities`'e force-push
  izni verilmediği için o dal olduğu gibi duruyor (artık gereksiz, silinebilir). Derleme kontrolü run 42405289646.
  Upstream PR şablonuna göre başlık ve açıklamalı karşılaştırma bağlantısı kullanıcıya verildi; PR'ı kullanıcı açacak.
- **Tekrar kontrol (2026-11-07):** dal hâlâ upstream `dev` ucunda (`d2383876`), PR henüz açılmamıştı; bağlantı yeniden verildi.
- **Son test (2026-11-07):** youtube-morphe run 42412970551 (PR dalı, `v2.2.0-dev.1` tabanı) cihazda sorunsuz. Son bağlantı verildi:
  YTindir bağlantısız metin, deneysel sürüm kutusu işaretli, "Tested on YouTube 21.44.528 (patches based on v2.2.0-dev.1)".

### 2026-11-07: YouTube Music cam navigasyon çubuğu (PR 4542 açık)

- Hızlı indir menüsü PR'ı açıldı: upstream PR 4539.
- Eski dal `feat/music-glass-navigation-bar` (`d1dc820b`, ~50 commit geride) force-push yapılmadan yeni dala taşındı:
  **`feat/music-glass-nav-bar`** (`46528ef2`, upstream `dev` `0b7ea38d` üzerinde).
- Çakışma: commit YouTube cam kodunu `shared-youtube`'a (`GlassCapsuleDrawable`, `GlassNavigationBarLayout`,
  `GlassNavigationBarSizePreference`) taşıyor; upstream'deki 0xrxL düzeltmeleri ortak koda işlendi:
  tablette en küçük tuş genişliği (`Buttons.minWidth`, `lastMinButtonWidth`, `getButtonsLayout(appMinWidth)`) ve
  YouTube `CapsuleHost.getCapsule`'da slim status bar. Upstream "blur yalnızca Android 14+" (YouTube) korunuyor.
- Açık karar: Music kendi bulanıklığını Android 12+ RenderEffect (öncesi yazılım) ile çiziyor; YouTube'daki 12/13
  kısıtlaması Music'e uygulanacak mı kullanıcıya soruldu.
- Testler: derleme run 42417396280, YT Music test APK `yt-music-morphe` run 42417411050, YouTube test APK
  `youtube-morphe` run 42417417732 (hepsi başarılı); cihazda deneme bekleniyor, sonra PR bağlantısı.
- Kullanıcı ikisini de denedi, ciddi sorun yok. Bulanıklık kararı: Music'te Android 12+ açık kalıyor, sorun olursa
  karar bakımcılara (PR açıklamasında belirtildi). YT Music 9.45.51 kaynağıyla yeni test APK run 42419605285.
  YouTube debug logunda hata yok (tek toast "You have the latest version", ayarlarda güncelleme denetimi).
  PR karşılaştırma bağlantısı verildi.
- **PR açıldı (2026-11-07): upstream PR 4542**, bakımcı incelemesi bekleniyor.

### 2026-11-07: Glass navigation bar durumu ve YouTube Music cam çubuğu

- Upstream `dev`'de YouTube cam çubuğu PR'larımız (4338, 4426, 4447, 4452) ve 0xrxL'nin sonraki düzeltmeleri var
  (tablette uç tuşlar, gizli mod/bağlantı çubuğu, Android 11 siyah bant, boyut önizlemesi).
- YouTube'da varsayılan opaklık upstream'de **50**. %20 varsayılan yalnızca dal `feat/music-glass-navigation-bar`
  (`d1dc820b`, YouTube Music cam çubuğu, upstream'de PR yok) içinde ve yalnızca Music ayarı için; YouTube için %20
  yapan bir commit hiçbir dalda bulunamadı.

### Önceki işler (özet; ayrıntı `docs/is-gunlugu-arsiv.md`)

- **2026-11-05 Glass navigation bar düzeltmeleri:** 3 tuşlu gezinmede uzama (issue 4443) PR 4452 ve açık temada Shorts
  beyaz panel PR 4447 upstream `dev`'de birleşti. Test dalları (`test/glass-room-field`, `test/glass-shorts-light`) gereksiz.
- **2026-11-02 Navigation bar cam stili:** PR 4338 (opaklık kaydırıcısı dahil) ve uzamış kapsül düzeltmesi PR 4426
  birleşti. Açık: bir kullanıcıda "tema yaması hariç tutulunca açılışta çökme"; cam koduyla bağı görünmüyor, logcat gerekli.
- **2026-11-02 Copy text (seçilebilir metin):** PR 4317 açıldı, dal `ccr-88e9ea31-1yc38m`. Bağlantılar YouTube'da
  göründüğü gibi kısaltılmış kalıyor (kullanıcı kararı).
- **2026-11-01 Patches update notice, dev sürümleri ayarı:** PR 4178, dal `feat/patch-update-notification`; yorumculara
  yanıt metni verildi.
- **2026-10-29 RYD akış beğeni rozeti kararlılığı:** dal `fix/feed-engagement-counts` (`dd9240f0`), karşılaştırma
  bağlantısı verildi. Yana kaydırmalı raflarda rozet yok (tasarım gereği, eklenmeyecek).
- **2026-10-29 Feed video actions düzeltmeleri:** dal `fix/feed-video-actions` (`a647f8ae`), PR açıldı. Açık: Abonelikler'de
  uzun kalınca kartlar arası boşluk kapanıyor (kaynağı belli değil, muhtemelen upstream "Feed card layout").
- **2026-10-29 Download post images kararsız eşleşme:** dal `fix/post-image-download` (`01b648cf`). Asıl yama ve
  Translate posts PR'ları bakımcı tarafından birleştirildi.
- **2026-10-29 Translate posts:** dal `feat/post-translate` (`c97cc4ef`), birleşti.
- **2026-10-26 Channel search gönderi araması:** dal `feat/channel-search-posts` (`ea883c862`), PR'ı kullanıcı açacaktı.
- **2026-10-23 Download post images:** dal `feat/post-image-download`, PR açıldı ve birleşti.
- **2026-10-22 Search comments:** PR 3942 (dal `ccr-511e3576-s4c9y4`), bakımcı incelemesi bekleniyordu.
- **Fork `dev` dalında upstream'de olmayan 2 commit:** `28efd7a4` (RYD inset thumbnail ve kanal gönderileri),
  `d8d3286f` (Feed video actions yatay ızgara). Kullanıcının mı, çalışma dallarına katılacak mı belli değil.
