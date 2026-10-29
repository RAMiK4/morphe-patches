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
- **Açık:** Kare müzik videosunda (INNA) rozet hiç yoktu; sebep bilinmiyor (log gerekli).
- **Abonelikler yapışma:** "Disable toolbar navigation tabs" kapalıyken de oluyor; kaynak hâlâ belli değil.

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
