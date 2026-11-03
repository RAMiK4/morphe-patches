# CLAUDE.md

Bu dosya yalnızca fork'un (`RAMiK4/morphe-patches`) `main` dalında durur.
Yeni oturumlar depoyu bu daldan klonladığı için kurallar ve iş günlüğü her oturumda okunur.
Upstream'e (`MorpheApp/morphe-patches`) giden hiçbir dala veya PR'a eklenmez.

## Oturum başında

1. Aşağıdaki **İş günlüğü** bölümünü oku. Orada "tamamlandı" yazan bir işi baştan yapma.
   Devam eden bir iş varsa, dalını ve son durumunu oradan al.
2. Kullanıcı Türkçe yazıyor. Yanıtları Türkçe ver.

## Temel dal kuralı: her zaman upstream `dev`

- Yeni işler her zaman **upstream `dev`** dalının en güncel hâli üzerine kurulur.
  Fork'un `main` dalı (son kararlı sürüm) ve fork'un `dev` dalı temel alınmaz.
- Fork'un remote'larında upstream yoktur. Doğrudan çekilir:

  ```sh
  git fetch https://github.com/MorpheApp/morphe-patches.git dev:refs/remotes/upstream/dev
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

## İş günlüğü

Bir iş tamamlandığında veya durumu değiştiğinde buraya kısa bir kayıt eklenir ve bu dosya
fork'un `main` dalına commit edilir. Kayıtlar en yeni üstte olacak şekilde tutulur.

### 2026-11-02: Navigation bar: cam (glass) stili (PR 4338 açık)

- **PR:** https://github.com/MorpheApp/morphe-patches/pull/4338 (kullanıcı açtı, 4 ekran görüntüsü tabloda).
- **Bakımcı değişiklikleri (dalımıza push):** `b364857f` (Liso): renk tonu yansıma yerine YouTube'un Paint'i oluşturduğu
  yerde kancayla düşürülüyor (`GlassNavigationBar.lightenBlurTint`, Fingerprints.kt'de yeni parmak izi), köprüler hep
  ekleniyor, `PIVOT_BAR` NavigationBarPatch'ten, boyutlar sabit. `e164fc8a` (0xrxL): yalnızca `dev` birleştirmesi.
  Bu dalda artık yerel commit atmadan önce `origin/ccr-039f9d55-3ljch4` çekilmeli (force-push yapılmamalı).
- **Açık istek (0xrxL, ekran kaydıyla):** "Increase the reposition offset, otherwise the animation end will not hide the
  navigation bar." Video bu ortamdan açılamıyor (upstream web/API proxy'de kapalı). YouTube kodu: `Lrxc.p(ZZ)` gizleme
  ya translationY (gösterirken başlangıç `lp.height`=168, gizlerken hedef `getHeight()`=203) ya da LayoutParams yüksekliği
  animasyonu; `Lrxc.X(FZ)` kaydırmaya bağlı `translationY = f * getHeight()`. Kök neden henüz belli değil, kullanıcıdan
  videonun tarifi/indirilmesi istendi.
- **Reposition sonucu (2026-11-03):** Kullanıcı açıkladı: 0xrxL minimal / minimal 2 mini oynatıcı + "Yukarı kaydırarak
  genişlet" kullanıyor; mini oynatıcıyı yukarı sınıra sürükleyip tutarken altta kapsülün üst kısmı (~35px, eklediğimiz oda)
  görünüyor, parmak kalkınca çubuk tamamen gizleniyor. YouTube o sürüklemede çubuğu eski yüksekliğe (168) göre kaydırıyor
  olmalı. Kod değiştirilmedi (kullanıcı kararı: mevcut hâl iyi, az veriyle çalışan yöntemi bozma); 0xrxL'e açıklama yanıtı
  yazıldı, istenirse ofseti ölçekleme sonraki adım. Açık soru (bir kullanıcı): Şeffaf/Yarı saydam yoğunluğu için kaydırıcı.
- **Opaklık kaydırıcısı (test, PR'a eklenmedi):** Dal `test/glass-opacity` (= PR dalı `e164fc8a` + `33cec069`). Ayar
  `morphe_navigation_bar_glass_opacity` (SeekBarPreference 0-100, varsayılan 50, cam ayarına bağlı). Ölçek = değer/50:
  Yarı saydam bulanıklık tonu koyu %30, açık %40 (50'de), Şeffaf dolgu her iki temada %50, Shorts %45; 0 = hiç ton/dolgu
  (yalnızca kenar, parlaklık, gölge), 100 = iki katı (en çok %100). Test: youtube-morphe run 41579198443, derleme
  kontrolü run 41579195193. PR'a mı ayrı PR'a mı eklenecek: kullanıcı karar verecek.
- **Kaydırıcı test 1 (run 41579198443):** Ayarlar çalışıyor, logda hata yok (ton 206 -> 77). Kullanıcı: Yarı saydam
  varsayılanı koyu %40, açık %50 olsun. Düzeltme `62360920` (test dalı), test APK youtube-morphe run 41582123102.
  Öneri: PR 4338'e ayrı commit olarak eklemek (varsayılan ton değişikliği zaten PR'a girmeli); karar kullanıcıda.
- **Kaydırıcı PR'a eklendi (2026-11-03):** Kullanıcı onayladı. PR dalına (bakımcı commit'lerinin üstüne, force-push yok)
  tek commit `df80d60a` "feat(YouTube - Navigation bar): Add `Glass opacity` slider" (kod `test/glass-opacity` ile aynı).
  Derleme kontrolü run 41583382711 başarılı. Soruyu soran kullanıcıya PR yorumu metni verildi.
- **0xrxL düzeltmesi `88ba5382` (2026-11-03):** Reposition sorununu kendisi çözdü: `PivotBar.setTranslationY(F)` override'ı
  (addBridge) → `GlassNavigationBar.getTranslationY`: YouTube çubuğu verdiği yükseklik kadar kaydırınca (sürükleme
  translation'ı, kaydırmada negatif alt margin) eklenen oda da aynı oranda ekleniyor; ortak `getAddedRoom`. Mantıklı,
  bizim yöntemle uyumlu. Test APK: youtube-morphe run 41596409164. Üstteki Home/Subs sekme çubuğundaki hap biçimli
  seçim göstergesi bizden değil (yalnızca `pivot_bar`'a dokunuyoruz), YouTube'un tasarımı/denemesi.

- **Durum:** İlk sürüm yazıldı, cihazda henüz test edilmedi.
- **Dal:** `ccr-039f9d55-3ljch4`, upstream `dev` `67b7f769` üzerinde, commit `3a9f1494`.
  Derleme kontrolü run 41453276056, test APK youtube-morphe run 41453301121 (`patches_source`, publish kapalı).
- **Ne yapıyor:** Yeni ayar `morphe_navigation_bar_glass` (varsayılan kapalı, "Navigation bar" ekranında, stil listesinin
  altında). `GlassNavigationBar.java`: `pivot_bar`'ın arka planı tek bir özel Drawable (gölge, yarı saydam dolgu,
  üstten alta parlaklık, geçişli 1dp kenar, seçili sekme kapsülü 200ms kayarak). Kapsül yatay 12dp, üst 6dp, alt 8dp +
  `pivot_bar` alt padding'i (sistem butonları/jest alanı). Düzen ve yükseklik değişmiyor, yatay padding verilmedi
  (gösterge kapsül içine kırpılıyor, en fazla 96dp). Arka plan PreDraw ile korunuyor; Shorts'ta her zaman koyu cam;
  çubuk yatay alt bar değilse (genişlik ≤ 2×yükseklik) YouTube'un arka planı geri veriliyor. Cam açıkken YouTube'un
  bulanıklığı (`useNavigationBarBlur`) kapalı.
- **Upstream değişikliği:** `DISABLE_TRANSLUCENT_NAVIGATION` artık yok (deprecated → `NAVIGATION_BAR_STYLE` enum:
  Default/Translucent/Transparent/Opaque). Ayar Opaque stilde kullanılamıyor (`GlassAvailability`), ikisi açıksa opak kazanıyor.
- **Test 1 (run 41453301121):** Kapsül dışı saydam (YouTube'un çubuk arka planı `pivot_bar` arka plan drawable'ıymış),
  ama kapsül dar: ikonlar üstten, etiketler alttan ve kenarlardan taşıyor; bulanıklık yok. Düzeltme `1e985afe`: kapsül
  butonların tüm yüksekliği (üst 0, alt buton alanı + en çok 4dp), yatay 10dp; `pivot_bar`'a yatay padding 20dp (PreDraw
  ile korunuyor, ray düzeninde geri veriliyor); YouTube'un arka planı (renk/opak değilse) kapsüle kırpılıp altta çiziliyor,
  `useNavigationBarBlur` artık camı kapatmıyor → Yarı saydam stilde (Android 12+) gerçek bulanıklık. Dolgu bunun üstünde 1/3.
  Hata olursa ("app background failure") YouTube arka planı bırakılıyor. Test: youtube-morphe run 41458468427,
  derleme kontrolü run 41458461269.
- **Test 2 (run 41458468427):** Butonlar kapsülün içinde; Yarı saydam ve Varsayılan stilde gerçek bulanıklık var, Şeffafta
  bulanıklıksız. Yatay ekran ve sekme göstergesi çalışıyor. Logda hata yok ("Glass navigation bar applied", sekme
  değişimleri). Kapsül karanlık temada oldukça koyu (YouTube'un kendi tonu + bizim 1/3 dolgu); daha saydam olsun mu
  kullanıcıya soruldu. Aydınlık tema, mini oynatıcı, kaydırınca gizlenme, 3 tuşlu gezinme henüz test edilmedi.
- **Test 3 (aydınlık, 3 tuş, Shorts):** Hata yok, yalnızca tasarım. Kullanıcı: daha saydam, dolgu kaldırılsın (liquid
  glass'a en yakın). Düzeltme `b5849e58`: bulanık YouTube arka planı üstünde dolgu yok (Şeffaf stilde dolgu kalıyor),
  karanlıkta parlaklık/kenar biraz güçlü. Soru: kapsül alta yapışık görünüyor, havada durabilir mi? Düzen değişmeden
  yukarı taşınamaz (butonlar çubuğun tüm yüksekliğini dolduruyor; daha yukarısı çubuğu uzatmak = akış alt boşluğu/mini
  oynatıcı riski). Teşhis için "Glass navigation bar applied" satırına yükseklik/alt padding/alt boşluk eklendi.
  Test: youtube-morphe run 41464087817, derleme kontrolü run 41464082281.
- **Test 4 (run 41464087817):** Log: `pivot_bar` yükseklik 168, alt padding 0, altında boşluk 0 (kullanıcının cihazında
  sistem alanı yok, kapsül ekranın en altına değiyor); YouTube arka planı sınıfı `auyq` (frosted). Kullanıcı: bulanıklık
  fazla koyu, ~%30 olsun; havada durması sorun çıkarmazsa iyi olur. Düzeltme `ed2f3854`: (1) `getNavigationBarBottomPadding`
  ve slim status bar yolunda alt oda en az 10dp (`GlassNavigationBar.getFloatingRoom`; YouTube'un sistem butonları
  odası gibi yüksekliğe ekleniyor, akış bunu izliyor); kapsül bu 10dp'nin üstünde biter. (2) `auyq` alanları yansıma ile
  taranıyor: yarı saydam Paint/ColorDrawable rengi karanlıkta %35, aydınlıkta %55'e düşürülüyor; aynı değerli int alanlar
  da. Debug: "Glass navigation bar floating room:", "Glass app background paint/color/int:". Test: youtube-morphe run
  41469305013, derleme kontrolü run 41469298297. Riskler: mini oynatıcı/akış alt boşluğu 10dp kayabilir.
- **Test 5 (run 41469305013):** Bulanıklık tonu çalıştı (log: `auyq.h` Paint `ce0f0f0f` → `590f0f0f`), kullanıcı beğendi.
  Kapsül havada değil: sistem alanı yokken YouTube alt padding'i hiç ayarlamıyor, `getNavigationBarBottomPadding` çağrılmıyor.
  Düzeltme `c7c58c27`: PreDraw'da alt padding < 10dp ise `PivotBarAccess.patch_setSystemButtonsRoom(10dp)` + setPadding
  (post ile, slim status bar yöntemi gibi). Debug: "Glass navigation bar floating room set:". Test: youtube-morphe
  run 41484339276, derleme kontrolü run 41484333884.
- **Test 6 (run 41484339276):** Kapsül havada, görünüm iyi. Sorunlar: kaydırınca gizlenip gelirken titreme, hızlı
  kaydırmada çubuk parçalanıyor/üst üste biniyor, sekme değişince 1-2px inip çıkıyor. Log: "floating room set: 0 -> 35"
  94 kez (bir şey alt padding'i sık sık 0'a çekiyor, biz post ile geri koyuyorduk → arada hatalı kare). YouTube APK
  (youtube-morphe draft release "source", 21.44.528) androguard ile incelendi: `PivotBar` bir `HorizontalScrollView`
  (`avni`); oda alanı `s`, yükseklik `i()` = `t + s`; padding'i yalnızca `Lrxc.L` (insets) ve `Lrwx.accept` yazıyor
  (ikisi de bizim kancalı yolumuz, ama "floating room:" logu yok → çağrılmıyor; bayrak 45649805 açıkken `L` atlanıyor).
  Sıfırlayan kaynak bulunamadı. Düzeltme `6603c08b`: padding/oda PreDraw içinde aynı karede geri konuyor ve o kare
  atlanıyor (en çok 2 ardışık kare); kapsül yan boşluğu 12dp. Test: youtube-morphe run 41501442214, kontrol run 41501437245.
- **Test 7 (run 41501442214):** Titreme, sekme geçişinde inip çıkma düzeldi. Kalan: otomatik gizleme açıkken çubuk
  gelip giderken düşük fps hissi (sıfırlamada kare atlıyorduk). Düzeltme `6f9529d3`: Kotlin'de `PivotBar`'a
  `setPadding`/`setPaddingRelative(IIII)` override'ı (addBridge, invoke-super üst sınıf `avni`), alt değer
  `NavigationBarPatch.getNavigationBarPadding` → `GlassNavigationBar.getFloatingRoom`; padding artık sıfırlanmıyor,
  PreDraw yolu yalnızca yedek. Test: youtube-morphe run 41506122855, kontrol run 41506118141.
- **Test 8 (run 41506122855):** Akıcı oldu, ama butonlar üstten/yanlardan taşıyor. Log: "height: 168 bottom padding: 35":
  override padding'i en baştan 35 yaptığı için yüksekliği artıran `patch_setSystemButtonsRoom` hiç çağrılmadı. Düzeltme
  `bfc10b0a`: PreDraw'da padding ≤ 10dp iken LayoutParams yüksekliği son ayarladığımızdan farklıysa oda yeniden veriliyor
  (döngü yok: sonuç aynıysa tekrar çağrılmaz). Debug: "Glass navigation bar height: a -> b". Test: youtube-morphe run
  41509136605, kontrol run 41509130682.
- **Test 9 (run 41509136605):** Butonlar kapsülün içinde. Ama log: "bar height: X -> 203" 699 kez, X 168'den 1'e kadar →
  YouTube otomatik gizlemede çubuğun LayoutParams yüksekliğini 168→0 canlandırıyor, biz her karede 203'e çekiyorduk.
  Düzeltme `f091cef6`: yükseklik PreDraw'da değil, `PivotBar.onMeasure(II)` override'ında (addBridge, PROTECTED) ekleniyor:
  EXACTLY yükseklik h için ek = alt padding − YouTube'un kendi odası (`appRoom`, `getNavigationBarBottomPadding` yolundan);
  h < taban ise orantılı (animasyon bozulmuyor). Taban = apply anında LayoutParams yüksekliği − appRoom. Debug:
  "Glass navigation bar base height:". Test: youtube-morphe run 41512912017, kontrol run 41512905810. Kullanıcı PR istedi,
  bu testten sonra tek commit'e sıkıştırılıp karşılaştırma bağlantısı verilecek.
- **Test 10 + PR (2026-11-03, run 41512912017):** Kullanıcı onayladı ("her şey tamam, daha stabil"). Log temiz: taban 168,
  çubuk 203, "height: X -> 203" yok. Upstream `dev` `f3a64b6c` üzerinde tek commit `b1eb28ec`, `ccr-039f9d55-3ljch4`
  force-push, derleme kontrolü run 41516593339 başarılı. Karşılaştırma bağlantısı verildi, PR'ı kullanıcı açacak.
  Eski ara commit'ler yerel `backup/glass-before-squash` dalında.
- **Belirsizlikler:** YouTube'un bulanık arka planı `pivot_bar`'ın arka planı mı yoksa ayrı bir view mı (kapsül dışı
  saydam görünmezse buna bakılacak); Default stilde hesap saydam düzende değilse kapsül dışında içerik değil uygulama
  arka planı görünür (Translucent/Transparent önerilir).

### 2026-11-02: Copy text: açıklama ve gönderilerde metin seçme (PR 4317 açık)

- **PR:** https://github.com/MorpheApp/morphe-patches/pull/4317 (kullanıcı açtı, 4 ekran görüntüsü Additional context'te).

- **İstek:** Gönderilerde ve video açıklamalarında istenen kelime/cümleyi seçip kopyalama. Upstream "Copy text"
  yalnızca başlığı ve yorumları bütün olarak kopyalıyordu (uzun basma).
- **Değişiklik `11153c5c`** (dal `ccr-88e9ea31-1yc38m`, upstream `dev` `86cd3501` üzerinde): yeni ayar
  `morphe_copy_select_text` (varsayılan açık, General). Açıklama (`description_rich_text_list`, Shorts
  `cell_description_body`) ve gönderi (identifier'da "post", yorumlar hariç, ≥8 karakter) metinleri
  `hookLithoSpannableString` ile kaydediliyor; uzun basınca seçilebilir metinli pencere (CustomDialog,
  `setTextIsSelectable`) + "Copy all". Karşılaştırma boşluklar normalize edilerek, kesik metin başlangıçla.
  Tıklanabilir üst yoksa (açıklama) uzun basma metni çizen host'a konuyor.
- **Test 1 (run 41396885932):** Kullanıcı onayladı (akış, kanal Gönderiler, açıklama; seçim ve kopyalama çalışıyor).
  Sorun: bağlantılar gönderide göründüğü gibi kısaltılmış ("site.com/ab...") geliyordu. Düzeltme `a0ee3f1d`:
  `hookElement` ile post/açıklama elementlerindeki bağlantılar (YouTube `/redirect?q=` ise hedef) saklanıyor; pencerede
  "..."/"…" ile biten kelime, şemasız başlangıcı eşleşen tam bağlantıyla değiştiriliyor (Copy all dahil).
  Test: youtube-morphe run 41399215805, derleme kontrolü run 41399212631.
- **Test 2 (run 41399215805):** Bağlantılar hâlâ kısaltılmış (patreon "pos...", youtu.be "?si=xEpzH..."); element
  verisinden bulunamadı. Düzeltme `b20caf88`: metnin link span'larında (ClickableSpan/özel span, URLSpan) yansıma ile
  (BFS, derinlik 6, 400 nesne) URL/`/redirect?q=` aranıyor, gösterilen metin → bağlantı saklanıyor. Debug log:
  "Select text element links:", "Select text link:", "Select text link not found:", "Select text shortened link not found:".
  Test: youtube-morphe run 41404217677, derleme kontrolü run 41404214358.
- **Test 3 + PR (2026-11-02):** Span yöntemi de çalışmadı; kullanıcı kararı: bağlantılar YouTube'da göründüğü gibi
  kalsın, bağlantı kodları çıkarılsın. İlk sürüm upstream `dev` `b031d640` (v2.1.0-dev.6) üzerinde tek commit `2cbdb39e`,
  `ccr-88e9ea31-1yc38m` force-push, derleme kontrolü run 41413681481 başarılı. Karşılaştırma bağlantısı verildi,
  PR'ı kullanıcı açacak.

### 2026-11-01: Patches update notice: dev sürümleri ayarı (PR 4178 açık)

- **PR:** https://github.com/MorpheApp/morphe-patches/pull/4178 (dal `feat/patch-update-notification`, ilk commit `88e9ce6b`).
- **Yorumlar (Stark-0505, prat89, MarcaDian):** dev sürümleri neredeyse her gün çıktığı için bildirim yalnızca kararlı
  sürümlerde olmalı; dev isteyen ayardan seçebilmeli; hiç istemeyen kapatabilmeli (ana ayar zaten varsayılan kapalı).
- **Değişiklik `a2f4b76f`:** "Include dev releases" alt ayarı (`morphe_patches_update_notice_dev_releases`, varsayılan
  kapalı, ana ayara bağlı). Eskiden dev yamalarında dev sürümleri otomatik dahildi (`Utils.isPreReleasePatches`).
  Önbellek ayarı da saklıyor; ayar değişince veya yamalar değişince saatlik bekleme olmadan yeniden kontrol.
- **Test:** `test/patch-update-notice-preview` (1.49.0-dev.10 bildiriyor; v2.0.0 kararlı olduğu için varsayılanla
  bildirim çıkmalı, ayar açılınca 2.1.0-dev.x), youtube-morphe run 41303615067, derleme kontrolü run 41303617047.
- **Test 1 (run 41303615067):** Kullanıcı onayladı: varsayılanla 2.0.0, dev ayarı açıkken 2.1.0-dev.5 gösteriliyor.
  İstekler: iki ayar ayarlar ekranında alt alta olsun; değişiklik listesi youtube-morphe'deki güncelleme penceresi gibi
  görünsün. Düzeltme `000590df`: ayarlar `noTitleUnsortedPreferenceCategory` içinde; bölüm başlıkları kalın, 1.15x ve altında
  ince çizgi (`RuleSpan`), maddeler tam boyutta, alt satırlar "• " genişliğine hizalı. Test: youtube-morphe run 41307447901,
  derleme kontrolü run 41307450058. Onaydan sonra PR'a yorumculara yanıt notu yazılacak.
- **Test 2 (2026-11-01):** Kullanıcı onayladı. PR yorumu (3 ekran görüntülü tablo) metni verildi, kullanıcı ekleyecek.

### 2026-10-29: Return YouTube Dislike akış beğeni rozeti kararlılığı

- **PR durumu:** Çeviri ve Download post images PR'ları bakımcı tarafından birleştirildi; Feed video actions PR'ı açıldı.
- **Kod:** `FeedEngagementCountsPatch.java` (upstream'de; bizim `de420c27`/fork `28efd7a4` yerleşim eklememiz dahil).
- **Gözlem:** Logda ~600 "Duration badge not found ... thumbnail: Rect" (FeedViewCountPatch.findThumbnailFrame); süre
  çoğu kez tüm küçük resmin host'unda çiziliyor. Rozet süre varsa onun üstüne, yoksa küçük resimden tahminle
  konuyor; iki yol arasında gidip gelince zıplıyor. Ayrıca kart yeniden yerleşirken video bir an bulunamazsa
  rozet kaldırılıp yeniden ekleniyor (yanıp sönme).
- **Düzeltme `009d6f8d`** (dal `fix/feed-engagement-counts`, upstream `dev` `82103d36`): video bir an bulunamazsa,
  kart ayrılmadıysa ve yeniden kontrol bekliyorsa (`FeedVideoCardsPatch.isRetryPending`) rozet korunuyor; aynı video
  ve aynı genişlikte süreye göre bulunan yer saklanıp süre bulunamadığında kullanılıyor.
  Test: youtube-morphe run 40943783037, derleme kontrolü run 40943777993.
- **Test 1 (run 40943783037):** Taze açılışta aramada ve gönderi videolarında (KanalD) rozet hızlıca yanıp sönüyordu,
  yeniden açılışta yok. Kare müzik videolarında artık çıkıyor. Başka sayfalarda gezinip ana sayfaya dönünce rozet
  kayboluyor. Sebep (tahmin): `FeedVideoCardsPatch.textVideoIds` (4000 metin, LRU) başka sayfaların metinleriyle
  doluyor, akış öğeleri yeniden ayrıştırılmadan gösterilince kartlar eşleşmiyor. Düzeltme `d7b1da32`: bulunan kartların
  ≥20 karakterlik metinleri ayrı bir haritada (1000, çakışmada AMBIGUOUS) tutuluyor; ana haritada hiç eşleşme yoksa
  bu haritadan en az iki metin aynı videoyu göstermeli. Test: youtube-morphe run 40950420057, kontrol run 40950414491.
- **Test 2 + PR (2026-10-29):** Kullanıcı: yanıp sönme yok, genel olarak daha stabil; çok gezinip dönünce bazı
  rozetler hâlâ kayboluyor, kaydırınca geri geliyor (kabul edildi; "ana sayfa yeniden yüklenmesin" ayarı açık).
  Raflara eklenmeyecek (kullanıcı kararı). Upstream `dev` `ba1ac737` üzerinde tek commit `dd9240f0`,
  `fix/feed-engagement-counts` force-push; karşılaştırma bağlantısı verildi.
- **Tasarım gereği:** Yana kaydırmalı raflarda (Yeniden dinleyin, En alakalı vb.) rozet ve Daha sonra izle yok
  (`hasHorizontalList`). Eklenmesi yeni özellik; kullanıcıya soruldu.
- **Abonelikler yapışma:** Video kartından sonra gelen gönderide boşluk kayboluyor (gezinip dönünce). "Feed card
  layout" (upstream, 0xrxL) kart şablonlarını değiştiriyor; bizim yamalar yalnızca overlay çiziyor. Upstream'e bildirilebilir.

### 2026-10-29: Feed video actions düzeltmeleri

- **Download post images PR'ı:** Kullanıcı upstream'e açtı (2026-10-29). Çeviri PR'ı da açılmıştı.
- **Sorun 1 (düzeltme yazıldı):** Gömülü videolu gönderilerde (Honor, Yandex) Daha sonra izle butonu gönderinin ⋮
  menüsünün üstünde. Sebep: buton hep kök host'un üst sağına konuyordu; gönderide video başlık+metnin altında.
  Düzeltme `cba5fb5d` (dal `fix/feed-video-actions`, upstream `dev` `a3b34d75` üzerinde): küçük resmin üstü süre
  rozetinin altından hesaplanıyor (alt boşluk 8dp, yükseklik genişlik×9/16; üst yarıdaysa 0 = eski davranış); süre
  yoksa (canlı) menü butonu küçük resmin ≤64dp altındaysa 0, değilse buton gösterilmiyor. Süre rozeti mount edilmemişse
  aynı menü butonu için önceki değer kullanılıyor. Test: `test/post-fixes-dev11` + bu commit, youtube-morphe run 40922117770.
  Derleme kontrolü run 40922112366.
- **Test 1 (run 40922117770):** Gönderi menüsüne binme düzeldi. Yeni sorun: kare küçük resimlerde (müzik) buton sağ
  ortadaydı (16:9 varsayımı). Log (İngilizce): `Learned feed menu button pattern: Action menu` ilk menü açılışında;
  öncesinde Türkçe öğrenilmiş kalıp eşleşmediği için hiçbir kartta buton yoktu (Sorun 2'nin kök nedeni).
  Düzeltme `3366aa72`: kalıp dille saklanıyor (`dil|kalıp`, dil = view kaynaklarının ilk locale'i; eski değer/başka dil
  = öğrenilmemiş, şekil + süre rozeti ile çalışır); küçük resim yalnızca üstünde harfli metin varsa (gönderi) aşağıda
  sayılıyor, yoksa üstte (kare de olsa). Test: youtube-morphe run 40931504323, derleme kontrolü run 40931498980.
- **Test 2 + PR (2026-10-29):** Kullanıcı onayladı (her dilde buton hemen görünüyor, kare ve gömülü doğru). Upstream
  `dev` `82103d36` üzerinde tek commit `a647f8ae`, `fix/feed-video-actions` force-push; karşılaştırma bağlantısı verildi.
- **Filtre çipleri:** Kullanıcı buldu: YouTube hesabı yeni üst sekme düzenine (Ana Sayfa/Abonelikler/Müzik/Canlı)
  almış, "Disable toolbar navigation tabs" ayarı açıkken filtreler gizleniyordu; kapatınca düzeldi (upstream ayarı).
- **Upstream kaynaklı olabilecekler:** Ana sayfa filtre çipleri (Tümü/Müzik) kayboldu; Abonelikler'de kartlar
  arası boşluk kapanıyor. Logda `FeedCardLayoutPatch` ("Feed card layout") ve `ClassicSubscriptionsLayoutPatch`
  var; bizim yamalar yalnızca overlay çiziyor. Kullanıcıdan ilgili ayarları kapatıp denemesi istendi.
- **Sorun 2 (araştırılacak, log gerekli):** İngilizce/Rusça uygulama dilinde açılışta Daha sonra izle butonu ve RYD
  beğeni rozeti bazen hiç görünmüyor, birkaç kez çıkıp girince düzeliyor; Türkçede yok. Şüphe: öğrenilmiş menü butonu
  açıklama kalıbı (`morphe_feed_menu_button_description`) dile bağlı; dil değişince eşleşmiyor olabilir.
- **Sorun 3 (araştırılacak):** Abonelikler akışında uygulamada uzun kalınca iki içerik arası boşluk kapanıyor,
  yeniden başlatınca düzeliyor. Kaynağı belli değil.
- **Sorun 4:** RYD beğeni rozetinde nadiren titreme (bizim RYD eklememiz, fork `dev` `28efd7a4`).

### 2026-10-29: Download post images: kararsız eşleşme düzeltmesi

- **Durum:** Tamamlandı, PR hazır (kullanıcı açacak).
- **Belirti:** Birçok resimli gönderide buton/menü öğesi çıkmıyor (BBC Mundo: uzun metin + birkaç 4:5 resim, onlarca
  gönderiden birkaçında; Netflix Korea; akışta çıkan buton gönderi açılınca (1:1 resimler) yok).
- **Kök neden (tahmin):** `ProtoNode` satır sonu içeren metinleri metin saymıyor; çok paragraflı gönderilerin asıl metni
  kaydedilmiyordu. Eşleşme kısa metinlere (kanal adı vb.) kalıyor, kanal sayfasında/açık gönderide kanal adı birden çok
  gönderide geçtiği için AMBIGUOUS oluyor.
- **Düzeltme:** `ProtoNode.getMultiLineText()` (yerinde bayt taraması, mesaj etiketiyle başlayanlar hariç) ve
  `ProtoNode.texts()` (metin alanları + satır sonlu metinler, sırayla); `PostImageDownloadPatch` artık bu listeyi kullanıyor.
- **Dal:** `fix/post-image-download` (upstream `dev` `de2d7e94` üzerinde, commit `93029b31`). Derleme kontrolü run 40906549850.
- **Test:** dal `test/post-fixes-dev11` (= `test/post-translate-dev11` + bu düzeltme), youtube-morphe run 40906555399.
- **Test 1 (2026-10-29):** Kullanıcı onayladı: BBC, Netflix, TRT1 vb. yüzlerce gönderide akışta ve kanal sayfasında
  buton ve menü var, indirilen resimler doğru. Tek istisna: yalnızca tek emoji metinli resimli gönderi (emoji 2-3
  karakter, MIN_TEXT_LENGTH=4 altında; eşleşme için skor ≥2 gerekiyor). Düzeltme `7f7db1ea`: yalnızca emoji/sembol
  (harf/rakam yok, en az bir So karakteri) içeren kısa metinler de sayılıyor. Test APK: youtube-morphe run 40911435183.
  Kullanıcı: mevcut hâli bozmadan çözülemiyorsa olduğu gibi kalabilir. PR'da iki commit tek commit'e sıkıştırılacak.
- **Test 2 + PR (2026-10-29):** Kullanıcı onayladı (emojili gönderi dahil hepsi çalışıyor). Upstream `dev` `de2d7e94`
  üzerinde tek commit `01b648cf`, `fix/post-image-download` force-push. Kullanıcıya karşılaştırma bağlantısı verildi,
  PR'ı kendisi açacak. Sıradaki: Feed video actions (gömülü videoda Daha sonra izle butonunun yukarı kayması), sonra RYD.

### 2026-10-29: YouTube "Translate posts" yaması (gönderi çevirisi)

- **Durum:** İlk sürüm yazıldı, cihazda henüz test edilmedi. PR metni kullanıcı "her şey tamam" diyene kadar verilmeyecek.
- **Dal:** `feat/post-translate`, upstream `dev` (`2b92ceb2`) üzerinde, commit `d3d41312`.
- **Test derlemesi:** youtube-morphe run 40867787486 (`patches_source=RAMiK4/morphe-patches@feat/post-translate`,
  publish kapalı, artifact `test-build`). Derleme kontrolü: morphe-patches run 40867761359.
- **Ne yapıyor:** Metinli gönderilerin ⋮ menüsüne "Translate" ekler; çeviri (paylaşılan `TextTranslator`, gtx)
  metnin Litho text drawable'ına yeni bir StaticLayout verilerek yerinde gösterilir (aynı satır sayısı, sığmazsa "…").
  Tekrar menüde "Show original". Litho drawable'ı yeniden bağlarsa `FeedVideoCardsPatch.onRootLaidOut` üzerinden
  çeviri yeniden uygulanır; Litho metni yeniden oluşturursa `hookLithoSpannableString` ile çeviri döner.
  Çeviri alınamazsa metin Google Translate'e (ACTION_PROCESS_TEXT), yoksa paylaşım menüsüne gönderilir.
  Drawable bulunamazsa çeviri diyalogda gösterilir. Ayarlar: Feed ekranı, aç/kapat + hedef dil (varsayılan uygulama dili).
- **Nasıl buluyor:** Gönderi elemanının (identifier'da "post") tüm metinleri (satır sonlu olanlar dahil, ProtoNode
  bunları metin saymıyor) saklanır; kesik metin başlangıcıyla eşleştirilir. Gönderi metni = başlık satırının
  (en üstteki metin: kanal adı/tarih) altındaki ilk eşleşen metin. Menü butonundan yukarı doğru host'lar taranır (raflar için).
  Text drawable'lar ComponentHost alanlarında yansıma ile (BFS, derinlik 5, en çok 2000 nesne) aranır.
- **Belirsizlikler:** Text drawable'ın alanları (Layout/CharSequence) bulunabiliyor mu; başlık sezgisi doğru mu
  (yalnızca görselli gönderide öğe çıkmamalı); yerinde çevirinin kaydırınca korunması.
- **Test dalı:** upstream `dev` ucundaki yayınlanmamış "Series tracking" değişikliği youtube-morphe'de yamalamayı
  bozuyor (run 40867787486, `HistoryPatch.kt:112` ClassCastException; bizim yamayla ilgisi yok). Test için commit'ler
  `v1.49.0-dev.11` üzerine taşındı: dal `test/post-translate-dev11`.
- **Test 1 (run 40869591813):** Menüde "Translate" çıkıyor, ama İngilizce/Türkçe/Rusça her gönderide "The post is
  already in the translation language". Muhtemel sebep: başlığın ikinci satırındaki tarih ("1 gün önce", uygulama dilinde)
  gönderi metni sanılıyordu. Düzeltme `b039ec9f` (feat dalında; test dalında da): rakam içeren ≤30 karakterlik metinler
  (tarih, sayaç) aday değil. Test APK: youtube-morphe run 40871795768.
- **Test 2 (run 40871795768):** Çeviri çalışıyor, metin yerinde değişiyor. Sorun: çeviri koyu/siyah renkte çiziliyordu
  (YouTube rengi paint'e değil ForegroundColorSpan'e veriyor). Düzeltme `7a1eb089` (feat dalı; test dalında da):
  orijinal metnin tümünü kaplayan stil span'ları (link/görsel hariç) çeviriye kopyalanıyor, yoksa ilk harfin rengi.
  Test APK: youtube-morphe run 40874188309.
- **Test 3 (run 40874188309, 2026-10-29):** Kullanıcı onayladı: renk doğru, kaydırınca çeviri kalıyor, metinsiz/yalnız
  emojili gönderilerde öğe çıkmıyor. PR dalı `feat/post-translate` (`d3d41312`, `b039ec9f`, `7a1eb089`). PR metni için
  kullanıcının "her şey tamam" demesi bekleniyor.
- **PR hazır (2026-10-29):** Kullanıcı "her şey tamam" dedi. Upstream `dev` force-push ile yeniden yazılmıştı; değişiklikler
  yeni `dev` (`f049c91c`) üzerine tek commit `c97cc4ef` olarak taşındı (teşhis için eklenen debug yol kaydı çıkarıldı),
  `feat/post-translate` force-push. Kullanıcıya karşılaştırma bağlantısı verildi, PR'ı kendisi açacak.
- **Sıradaki işler (kullanıcının kararı):** aşağıdaki üç sorun bizim yamalarımızda düzeltilecek: Download post images,
  Feed video actions ve Return YouTube Dislike'a bizim eklediğimiz özellik (fork `dev`'deki `28efd7a4`).
- **Kullanıcının bildirdiği başka yamalardaki sorunlar (bu PR'ın konusu değil):** (1) Download post images: birçok resimli
  gönderide buton/menü öğesi bazen çıkmıyor, sonraki kontrolde çıkıyor (eşleşme kararsız). (2) Gömülü videolu gönderilerde
  video kartındaki Daha sonra izle butonu (Feed video actions) yukarı kayıp gönderinin ⋮ menüsünün üstüne biniyor
  (Yandex, Honor kanalları). (3) Aynı kartlarda beğeni rozeti nadiren titriyor (Feed engagement counts / RYD).
- **Debug log satırları:** "Post text:", "Other post text:" (yol bilgisiyle), "Translation to .. is the same text", "Text drawable ... layout: ... text: ...",
  "The translation can not be shown in place", "Replaced post text with its translation".
- **Dosyalar:** `PostTranslatePatch.java` (yeni), `FlyoutUtils.java`, `FeedVideoCardsPatch.java`, `Settings.java`,
  `patches/.../interaction/posttranslate/PostTranslatePatch.kt` (yeni), `strings.xml`, `arrays.xml`.

### 2026-10-26: YouTube "Channel search" yamasına gönderi araması

- **Durum:** Upstream `dev` (`f4c1f84`, 1.49.0-dev.6) üzerinde ilk test derlemesi hazır, cihazda henüz test edilmedi.
- **Dal:** `feat/channel-search-posts`. **Derleme:** run 40233263047 (artifact `patches-1.49.0-dev.6.mpp`):
  https://github.com/RAMiK4/morphe-patches/actions/runs/40233263047
- **Ne yapıyor:** Kanal araması panelinde sıralama butonunun soluna "Videos | Posts" seçicisi. Gönderiler
  modunda iki kaynak birleşir: (1) kanal arama sekmesi yanıtındaki gönderiler (`backstagePostRenderer` /
  `postRenderer`, video aramasıyla aynı istekten toplanıyor, aranan kelimeleri içerenler gösteriliyor);
  (2) Gönderiler sekmesi (WEB istemcisi, oturumsuz) en fazla 60 sayfa taranıp metinde arama yapılıyor.
  Kart: görsel (çoklu görselde sayı rozeti), eşleşmeden kısa önceden başlayan metin, aranan kelimeler kalın,
  tarih ve beğeni. Dokununca `youtube.com/post/<id>` uygulama içinde açılır. Seçilen mod sonraki aramada korunur.
  Türkçe I/ı/İ/i farkı yok sayılıyor.
- **Test 1:** Uygulama açılışta çöküyordu. Sebep: `ChannelSearchPatch` açılışta yükleniyor ve yeni `ResultMode`
  enum'u sınıf yüklenirken `sf(..).toString()` ile string okuyordu. Düzeltme: mod adı gösterilirken okunuyor.
  Yeni derleme run 40235264010.
- **Test 2:** Düzeltmeye rağmen açılışta çökme sürüyor. Kullanıcı olmazsa geri alınmasını istedi (2026-10-26).
  Kontrol için değişikliksiz upstream `dev` derlendi: dal `test/upstream-dev-control`, run 40239204971.
  O da çökerse sorun upstream 1.49.0-dev.6'da; çökmezse bizim değişiklikte (crash log gerekli).
- **Kontrol sonucu:** Değişikliksiz upstream `dev` (1.49.0-dev.6) de kullanıcının cihazında açılışta çöküyor; sorun
  bizim kodda değil. Muhtemel sebep: 1.49 dev'leri Patcher 2.0'a geçti (`0d5663d38`), kullanıcının Manager'ı uyumsuz olabilir.
  Test için özellik `v1.48.0` üzerine taşındı: dal `test/channel-search-posts-1.48`, run 40260173537
  (artifact `patches-1.48.0.mpp`). PR için yine upstream `dev` dalı (`feat/channel-search-posts`) kullanılacak.
- **Düzeltme (teşhis):** Kullanıcı resmi v1.49.0-dev.6 ile sorunsuz; yalnızca bu oturumdaki derlemeler çöküyor. Sebep:
  derlemeler upstream `dev` ucundan (`f4c1f84`) alınmıştı, bu uç dev.6'dan sonra gelen 11 yayınlanmamış commit içeriyor;
  çökme muhtemelen onlardan. Özellik `v1.49.0-dev.6` etiketine taşındı: dal `test/channel-search-posts-dev6`,
  run 40264042853. (1.48 tabanlı `test/channel-search-posts-1.48` da duruyor.)
- **dev.6 tabanlı derleme de (Manager ile yamalanınca) çöktü.** Kullanıcının sorunsuz release'leri `RAMiK4/youtube-morphe`
  iş akışında Morphe CLI ile derleniyor; Manager değil. Muhtemel sebep Manager + 1.49 (Patcher 2.0) uyumsuzluğu.
  `youtube-morphe` release.yml'e isteğe bağlı `patches_source` (owner/repo@dal, yalnız test, yayınlamaz) eklendi;
  `test/channel-search-posts-dev6` ile test APK'sı: run 40266898459 (artifact `test-build`, iki varyant, kullanıcının anahtarıyla imzalı).
- **Test 3 (youtube-morphe CLI derlemesi):** Açıldı, çalışıyor; karta dokununca gönderi açılıyor. Sorun: en fazla 3-4
  gönderi. Actions'ta prob (çıktı dala commit edilerek okundu; Actions log blob'u bu ortamdan 403): MWEB arama sekmesi
  hiç gönderi döndürmüyor; Gönderiler sekmesinde son `continuationItemRenderer` başlıktaki "Hakkında" paneline ait,
  bu yüzden tarama 1. sayfada bitiyordu. Doğru token ile beIN SPORTS'ta 40 sayfa/398 gönderi, ~7 sn.
  Düzeltme: sonraki sayfa yalnızca `contents`/`onResponseReceived*` içinden (header, engagementPanel, gönderi
  thread'leri hariç); arama sekmesi kaynağı kaldırıldı (ChannelSearchRequest upstream hâline döndü);
  gönderiler için En yeni/En eski sıralaması. Test APK: youtube-morphe run 40270404802.
- **Not:** Kullanıcının sorunsuz derlemeleri `youtube-morphe` iş akışında (Morphe CLI). Test için bundan sonra
  `patches_source=RAMiK4/morphe-patches@<dal>`, publish kapalı. Manager ile yamalanan Actions `.mpp`'leri çöküyordu.
- **Test 4 (2026-10-26):** Kullanıcı onayladı: çok sayıda sonuç, En yeni/En eski çalışıyor, son ~3-4 ayın gönderileri
  (sekme ~400 gönderide bitiyor). PR için hazır: `feat/channel-search-posts` upstream `dev` (`e32b05c66`) üzerinde tek
  commit `ea883c862`, derleme run 40272086656 başarılı. Kullanıcı PR'ı kendisi açacak.
- **youtube-morphe:** `patches_source` ile yapılan test derlemeleri artık yalnızca BBK Clone (hiby) varyantını derliyor.
- **Belirsizlikler:** MWEB arama sekmesinin gönderi döndürüp döndürmediği; Gönderiler sekmesi parametresi
  (yanıttaki sekmelerden yedek okuma var); `/post/` bağlantısının uygulama içinde açılması.
- **Debug log satırları:** "Channel search renderers", "Fetched channel search ... posts=", "Posts tab renderers",
  "Searched posts tab", "Post results: search tab=.. posts tab=..".
- **Dosyalar:** `ChannelSearchPatch.java`, `utils/requests/ChannelPostsSearchRequest.java` (yeni),
  `ChannelSearchRequest.java`, `ChannelSearchRoutes.java`, `strings.xml`.

### 2026-10-23: Quick download menu ve Search comments PR'larının çakışmaları

- Upstream `dev` (`71275419c`) Patcher 2.0'a geçti ve bazı yardımcı fonksiyonlar `app.morphe.util`'den
  `app.morphe.patcher.util`'e taşındı (getReference, findFreeRegister, registersUsed, cloneParameters,
  returnEarly, numberOfParameterRegisters vb.). Yeni dallarda bu importlar `app.morphe.patcher.util.*` olmalı.
- Quick download (`feat/external-downloader-quick-download`): `dev` birleştirildi (`588c947bc`),
  DownloadsPatch.kt import çakışması çözüldü.
- Search comments (`ccr-511e3576-s4c9y4`, Liso'nun 2 commit'i dahil): `dev` birleştirildi (`71796e585`),
  AskFlyoutMenuPatch.kt import çakışması (Liso `addBridge`'i Bridge.kt'ye taşımıştı) çözüldü,
  SearchCommentsPatch.kt'deki `getReference` importu güncellendi.
- Fork'ta Actions "Workflows on this fork have been disabled" ile kapanmıştı; kullanıcı Actions sekmesinden
  yeniden açtı (2026-10-23). İki dal Patcher 2.0 ile derlendi: run 39901166108, 39901168271 (başarılı).
- Download post images: upstream'in tablet menü düzeltmesi (`71275419c`) FlyoutUtils'e `contentView`
  değişkeni ekledi, bizimkiyle çakışıp derlemeyi bozuyordu. Dala `dev` birleştirildi (`9a281af55`) ve
  düzeltildi (`8cd67c019`); run 39901831610 başarılı.
- Ortak test dalı `test/all-prs` (upstream dev + üç PR, yalnızca test): run 39901829079 başarılı.
- Silinemeyen geçici dallar: `test/feat-external-downloader-quick-download-on-dev`, `test/ccr-511e3576-s4c9y4-on-dev`.

### 2026-10-23: YouTube "Download post images" yaması

- **Durum:** Upstream `dev` (`e8c520c`) üzerinde. Test 1: buton ve menü hiç görünmedi. Debug log gösterdi:
  görseller bulunuyor, ama kesik ("…Devamını okuyun") ve bağlantılı post metinleri eşleşmiyordu; Paylaş
  açıklaması "Paylaş" iken yalnızca "Share" aranıyordu; Paylaş'ın solunda Beğenme butonu var.
  Düzeltme `d246fa3`: kesik metin başlangıç eşleşmesi, görünüm açıklamaları da metin sayılıyor,
  Paylaş etiketi framework `share` string'inden; yer yoksa buton satırın son butonundan sonra.
  Test 2: görseller artık menüde bulunuyor ama menü öğesi görünmüyor (hata yok, FlyoutUtils muhtemelen -1);
  framework `share` string'i de bulunamadı. Paylaş butonu Litho'da `post_share_button.eml` (a11y id yok).
  Düzeltme `9ef840b`: Paylaş metni elementte `share_button` tanımlayıcısından sonraki metinlerden öğreniliyor;
  menü öğesi eklenemezse menü görünüm ağacı loga yazılıyor.
  Test 3: elementte Paylaş metni yok (öğrenilen aday çıkmadı). Gönderi menüsü PopupWindow: içerik görünümü →
  dikey LinearLayout → RecyclerView (video menüsünden bir katman az), FlyoutUtils kabı bulamıyordu.
  Düzeltme `babbbe8`: FlyoutUtils bu düzeni de tanıyor; Paylaş metni, adında "share" geçen uygulama
  string kaynakları taranarak bulunuyor (bir kez, arka planda).
  Test 4: çalışıyor; kalite iyi, GIF gerçek GIF iniyor. İstekler: alttaki buton tek (görünen) görseli
  indirsin; bazı gönderilerde seçenek yok (post_shelf_slim rafları atlanıyordu, bazı metinler birebir
  eşleşmiyordu). Gömülü video kapağı/anket görselleri konusunda kullanıcıya a/b/c seçeneği soruldu.
  Düzeltme `290bf9e`: buton karuseldeki en görünür öğeyi indiriyor (kaydırma ofseti / öğe aralığı);
  raflar avatar URL'lerinden gönderilere bölünüyor; metinler boşlukları normalize edilerek ve
  başlangıçlarıyla eşleşiyor.
  Karar (kullanıcı): şimdilik yalnızca resim gönderileri; anketler ve gömülü videolu gönderiler hariç.
  Düzeltme `23ec182`: yalnızca `images_post*` elementleri; options/poll/videos/shared post ve `/vi/`
  video kapağı içerenler eleniyor (raf parçalarında da).
  Test 5: kullanıcıya göre istediği gibi çalışıyor, yalnızca arada bazı gönderilerde çıkmıyor; hazır kabul etti.
  Son hâl: tanılama logları kaldırıldı, eşleşmeyen Paylaş butonlu kökler yeniden deneniyor; tek commit'e
  sıkıştırıldı (`472b09e`, upstream `dev` `e8c520c` üzerinde, force-push). Derleme run 39869167504 başarılı.
- **Dal:** `feat/post-image-download` (commit `472b09e`).
- **Derleme:** run 39869167504 (artifact `patches-1.48.0.mpp`):
  https://github.com/RAMiK4/morphe-patches/actions/runs/39869167504
- **PR:** Kullanıcı upstream'e açtı (2026-10-23, "Generated with Claude Code" satırı silinerek).
  PR numarası henüz kaydedilmedi; gelince dosya başlıklarındaki bağlantıya eklenebilir.
- **Ne yapıyor:** Resimli/GIF'li gönderilerde Paylaş butonunun hemen soluna bir indirme butonu çizer
  (kök host overlay'i, dokunma `dispatchTouchEvent` kancasıyla) ve gönderinin menüsüne "Download images"
  ekler. İkisi de gönderideki tüm görselleri `=s0` (orijinal boyut) ile Pictures/YouTube'a kaydeder;
  dosya türü baytlardan bulunur (gif/webp/png/jpg). Ayar varsayılan açık (Feed ekranı).
- **Nasıl buluyor:** Element ayrıştırılırken tanımlayıcısında "post" geçen (shelf olmayan) elementlerdeki
  ggpht/googleusercontent görsel adresleri alınır; avatar/emoji (`-k-`, `no-rj`, <200 px) elenir.
  Gönderinin metinleri görsellere eşlenir (FeedVideoCardsPatch ile aynı yöntem). Paylaş butonu içerik
  açıklamasıyla bulunur (YouTube `share` string kaynağı + "Share").
- **Dosyalar:** `extensions/.../patches/PostImageDownloadPatch.java`, `FlyoutUtils.java` (menü),
  `FeedVideoCardsPatch.java` (kök host çağrısı), `Settings.java` (`POST_IMAGE_DOWNLOAD`), `strings.xml`,
  `patches/.../interaction/postimages/PostImageDownloadPatch.kt`,
  `patches/src/main/resources/postimagedownload/drawable/morphe_post_image_download.xml`.
- **Belirsizlikler (test edilecek):** Gönderi elementinin görsel adreslerini içerip içermediği, Paylaş
  butonunun açıklaması, butonun solunda boşluk olup olmadığı, `=s0`'ın GIF'i animasyonlu verip vermediği.
  Sorunda debug log: "Post element", "Not a post element", "Share button not found" satırlarına bakılır.

### 2026-10-22: YouTube "Search comments" yaması

- **Durum:** Upstream `dev` (1.48.0-dev.18, `a6fdc2f1`) üzerinde. İki cihaz testi yapıldı, bulunan sorunlar düzeltildi.
  Son sürüm (`0b2bccb7`) henüz cihazda test edilmedi.
- **Dal:** `ccr-511e3576-s4c9y4` (son commit `6c98d5bb`; yazar RAMiK4, mesaj sonları `Co-Authored-By: Claude` olarak yeniden yazıldı, kod `0b2bccb7` ile aynı).
- **Derleme:** Son başarılı derleme run 39733975757 (artifact `patches-1.48.0-dev.18.mpp`):
  https://github.com/RAMiK4/morphe-patches/actions/runs/39733975757
  Kod değişmedikçe yeniden derlemeye gerek yok.
- **Test 1 → düzeltme `36158645`:** Eşleşen yorumlarda yalnızca metin kalıyordu, eşleşmeyenlerde "N yanıt" kalıyordu.
  Sebep: Litho her iç bileşeni kendi buffer'ıyla değerlendiriyor. Karar artık yorumun en dıştaki bileşeninde
  bir kez veriliyor (ThreadLocal yol öneki takibi), iç bileşenler aynı kararı izliyor.
- **Test 2 → düzeltme `0b2bccb7`:** Yorumlar artık tam görünüyor ve eşleşmeyenler kayboluyor, ama bazen
  "N yanıt" göstergesi kalıyordu (dokununca yalnızca eşleşen yanıtlar açılıyor, bu davranış kabul edildi).
  Litho tek iş parçacığında çalışıyor; yanıt satırı yorumundan ayrı yerleşebiliyor. Yanıt satırı artık
  yorumuna ortak uzun kimliklerle (yorum kimliği, yanıt belirteci, 24+ karakter) bağlanıyor; bağlı yorum
  görünürse yanıt hep görünür; kimlik yoksa sıraya dayalı yedek yöntem. Ayrıca Türkçe I/ı/İ/i farkı yok
  sayılıyor (Türkçe cihazda "IPHONE" bulunamıyordu) ve tırnaklı ifade araması eklendi. Saf mantık 17
  senaryoluk yerel testle doğrulandı.
- **Bilinen sınır:** Arama ham buffer'da yapıldığı için arayüz metinleriyle (ör. "yanıt") eşleşen kelimeler
  her yorumla eşleşebilir. Debug log için: Ayarlar → Debugging → Enable debug logging → Export debug logs.
- **Ne yapıyor:** Yorum panelinin başlığına, kapatma butonunun soluna bir arama butonu ekler.
  Aranan kelimelerin hepsini içermeyen yorumlar (`comment_thread.eml`) yüklenirken Litho filtresiyle gizlenir.
  Arama yalnızca yapıldığı videoda geçerlidir.
- **Dosyalar:**
  - `patches/.../youtube/interaction/searchcomments/SearchCommentsPatch.kt`, `Fingerprints.kt`
  - `extensions/youtube/.../patches/components/CommentsSearchFilter.java`
  - `extensions/youtube/.../settings/Settings.java` (`SEARCH_COMMENTS`)
  - `extensions/youtube/.../shared/EngagementPanel.java` (`getLastOpenedPanelId()` eklendi;
    upstream `getCurrentOpenedPanels()` metodunu kaldırmıştı)
  - `strings.xml`, `patches/src/main/resources/searchcomments/drawable/morphe_search_comments_button.xml`
- **Bilinen sınırlar:** Arama sonradan yüklenen yorumlara uygulanır (sıralama değiştirilmeli ya da yorumlar yeniden açılmalı).
  Başlık düzeni yalnızca LinearLayout ve RelativeLayout için destekleniyor.
  Yorum paneli, panel kimliğinde "comment" geçtiği varsayılarak tanınıyor.
- **Test 3 (`0b2bccb7`):** Sorun yok. Nadir aramalar (tırnaklı ifade, 3+ kelime) yavaş: YouTube daha çok sayfa
  indirmek zorunda kalıyor; filtre maliyeti değil, normal kabul edildi.
- **PR:** https://github.com/MorpheApp/morphe-patches/pull/3942 (upstream `dev`, kullanıcı açtı, 2026-10-22).
  Experimental v21.42.241 üzerinde test edildi. Commit yazarları Claude'dan RAMiK4'e çevrildi, mesaj sonlarındaki model adı ve oturum bağlantısı kaldırıldı (force-push).
  Upstream CI (Build pull request) üç push'ta da başarılı (run #5942-#5944). İnceleme yorumu henüz yok.
- **Sıradaki adım:** Bizim taraftan tamam (CI geçti, çakışma yok). Bakımcı incelemesi bekleniyor; yorum gelirse düzeltme.

### Not: fork `dev` dalındaki fazladan commit'ler

Fork'un `dev` dalında upstream'de olmayan 2 commit var (2026-10-22 itibarıyla):
`28efd7a4` (Return YouTube Dislike: inset thumbnail ve kanal gönderilerinde beğeni sayısı) ve
`d8d3286f` (Feed video actions: yatay ızgarada göster). Kullanıcının kendi değişiklikleri olup olmadığı ve
çalışma dallarına katılıp katılmayacağı henüz netleşmedi.
