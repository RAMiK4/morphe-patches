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

### 2026-10-22: YouTube "Search comments" yaması

- **Durum:** Kod yazıldı, upstream `dev` (1.48.0-dev.18, `a6fdc2f1`) üzerine taşındı. Cihazda henüz test edilmedi.
- **Dal:** `ccr-511e3576-s4c9y4` (commit `b65c76ec`). PR açılmadı.
- **Derleme:** Upstream `dev` tabanında Actions derlemesi başarılı (run 39728123459,
  artifact `patches-1.48.0-dev.18.mpp`, 90 gün saklanır):
  https://github.com/RAMiK4/morphe-patches/actions/runs/39728123459
  Yeniden derlemeye gerek yok; kod değişmedikçe bu dosya kullanılır.
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
- **Sıradaki adım:** Kullanıcı cihazda test edecek. Geri bildirime göre düzeltme yapılacak, sonra PR kararı verilecek.

### Not: fork `dev` dalındaki fazladan commit'ler

Fork'un `dev` dalında upstream'de olmayan 2 commit var (2026-10-22 itibarıyla):
`28efd7a4` (Return YouTube Dislike: inset thumbnail ve kanal gönderilerinde beğeni sayısı) ve
`d8d3286f` (Feed video actions: yatay ızgarada göster). Kullanıcının kendi değişiklikleri olup olmadığı ve
çalışma dallarına katılıp katılmayacağı henüz netleşmedi.
