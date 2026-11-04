# Premium Pleyer — Android

Premium qora-oltin interfeysli, Media3/ExoPlayer asosidagi Android audio pleyer.

## Build

GitHub Actions → **Build APK** → eng so‘nggi muvaffaqiyatli run → **Artifacts** → `pleyer-apk`.

Workflow `assembleDebug` yaratadi. APK — hozircha debug build; Play Store uchun release signing key bilan alohida signed release build kerak.

## Funksiyalar

- MediaStore orqali telefonning barcha ko‘rinadigan tashqi xotira volume’laridagi audio kutubxonani skanerlash.
- MediaStore’da ko‘rinmagan alohida audio faylni Android system file picker orqali import qilish.
- Albomlar, ijrochilar, qidiruv, saralash, sevimlilar va foydalanuvchi pleylistlari.
- Media3 fon ijrosi, media session, lock-screen/system media controls, audio focus.
- Shuffle, Repeat All/One, navbatga qo‘shish, ijro tezligi.
- Audio session’ga ulangan ekvalayzer va Bass Boost (qurilma qo‘llab-quvvatlasa).
- Uyqu taymeri.
- Qo‘shiq matnini qo‘lda kiritish va ijro davomida ko‘rsatish.
- Infinix HOT 60 Pro+ uchun `Music/PhoneRecord` boshlang‘ich istisnosi.
- Istalgan papkani tizim papka tanlagichi orqali kutubxonadan chiqarish; istisnolar qurilmada saqlanadi.
- Qo‘ng‘iroq yozuvlari formati bo‘yicha emas, papka/aniq fayl nomi bo‘yicha ajratiladi. AMR/3GP/AWB MIME turi o‘zi sababli fayl yashirilmaydi.

## Telefon yozuvlarini ajratish

Boshlang‘ich istisnolar:
- `Music/PhoneRecord`
- `Music/Phone Record`
- `Music/Phone Records`
- `Recordings/Call`
- `Recordings/Call Recordings`

`Recordings` yoki `Recorder` kabi umumiy papkalar avtomatik bloklanmaydi, chunki u yerda shaxsiy ovozli yozuvlar ham bo‘lishi mumkin. Call recording boshqa joyga saqlansa, Sozlamalar → Qo‘ng‘iroq yozuvlari va istisno papkalar → Istisno papka tanlash orqali aniq papkani qo‘shing.

Android MediaStore’da Android 12+ uchun `IS_RECORDING` belgisi bor, lekin u “voice recorder” orqali olingan yozuvlarni anglatadi, faqat telefon qo‘ng‘iroqlarini emas. Shu sabab uni barcha fayllarni yashirish mezoni sifatida ishlatmaymiz.

## Audio formatlar

Media3/ExoPlayer standart Android platform dekoderlaridan foydalanadi. Qurilma va konteyner mosligiga qarab MP3, AAC/M4A, WAV/PCM, FLAC, Ogg Vorbis/Opus, AMR kabi keng tarqalgan formatlar ijro etiladi. Noyob kodeklar (masalan, APE, ALAC, AC-3/E-AC-3, DTS) uchun alohida FFmpeg software decoder extension kerak bo‘lishi mumkin; bu modul hozirgi APK’ga hali qo‘shilmagan. Har qanday audio faylni mutlaqo ijro etish kafolati yo‘q.

## Android ruxsatlari

- Android 13+: `READ_MEDIA_AUDIO`
- Android 12 va past: `READ_EXTERNAL_STORAGE`
- Android 13+: bildirishnoma ruxsati
- Tashqi papka istisnosida: foydalanuvchi tanlagan subtree’ga SAF orqali cheklangan ruxsat

**Ilova `MANAGE_EXTERNAL_STORAGE` (barcha fayllarga ruxsat) so‘ramaydi.** Qo‘shimcha fayllar Android document picker orqali tanlanadi.

## Infinix HOT 60 Pro+

Dastlabki call-recording path `Music/PhoneRecord` qilib belgilangan. XOS versiyasi yoki telefon sozlamalariga qarab katalog nomi farq qilsa, ilovada haqiqiy katalogni qo‘lda istisno qiling.

## Muhim eslatma

Bu repository’dagi kod birinchi ishlaydigan debug APK darajasidagi baza. Turli Android qurilmalari va kodeklar bilan amaliy test, FFmpeg extension, crossfade, to‘liq metadata editor va release signing keyingi bosqichlarda qo‘shilishi mumkin.
