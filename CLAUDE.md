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

### 2026-10-23: Quick download menu ve Search comments PR'larının çakışmaları

- Upstream `dev` (`71275419c`) Patcher 2.0'a geçti ve bazı yardımcı fonksiyonlar `app.morphe.util`'den
  `app.morphe.patcher.util`'e taşındı (getReference, findFreeRegister, registersUsed, cloneParameters,
  returnEarly, numberOfParameterRegisters vb.). Yeni dallarda bu importlar `app.morphe.patcher.util.*` olmalı.
- Quick download (`feat/external-downloader-quick-download`): `dev` birleştirildi (`588c947bc`),
  DownloadsPatch.kt import çakışması çözüldü.
- Search comments (`ccr-511e3576-s4c9y4`, Liso'nun 2 commit'i dahil): `dev` birleştirildi (`71796e585`),
  AskFlyoutMenuPatch.kt import çakışması (Liso `addBridge`'i Bridge.kt'ye taşımıştı) çözüldü,
  SearchCommentsPatch.kt'deki `getReference` importu güncellendi.
- Fork'ta GitHub Actions kapalı (2026-10-23): derleme tetiklenemiyor; upstream PR CI'ına bakılır.
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
