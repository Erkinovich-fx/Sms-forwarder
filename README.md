# SMS → Telegram Forwarder

Telefoningizga kelgan SMS'larni avtomatik ravishda Telegram bot orqali
belgilangan chatga yuboradi. Doimiy fon xizmati (foreground service) bilan
ishlaydi, shuning uchun Android uni batareya tejash sababli o'chirib
qo'ymaydi.

## Termux'da joylashtirish

```bash
cd ~
# Bu ZIP'ni telefoningizga (masalan Downloads papkasiga) yuklab oling,
# so'ng Termux'ga ko'chiring yoki to'g'ridan-to'g'ri o'sha joyda oching:

cd ~/storage/downloads   # yoki ZIP qayerda bo'lsa shu joy
unzip sms-forwarder.zip -d ~/sms-forwarder-project
cd ~/sms-forwarder-project
```

Agar avvalgi GitHub repo'ingiz bor bo'lsa:

```bash
git init
git add -A
git commit -m "SMS forwarder - foreground service bilan"
git branch -M main
git remote add origin https://github.com/<FOYDALANUVCHI_NOMI>/sms-forwarder.git
git push --force origin main
```

(`--force` ishlatilmoqda, chunki GitHub'dagi eski holat bilan farq bo'lishi
mumkin — bu shaxsiy loyiha bo'lgani uchun xavfsiz.)

## Push qilgach

1. GitHub saytida repo'ga kiring → **Actions** bo'limi
2. "Build APK" ishga tushganini kuting (3-5 daqiqa)
3. Tugagach, **Artifacts** bo'limidan `sms-forwarder-apk` ZIP'ni yuklab oling
4. Ichidan `app-debug.apk`ni chiqarib, telefoningizga o'rnating

## O'rnatgandan keyin

1. Ilovani oching — SMS ruxsati so'raladi, "Allow" qiling
2. Status panelida (notification) **"SMS Forwarder faol"** degan doimiy
   yozuv paydo bo'ladi — bu normal, Android'ga ilovani o'chirmaslikni
   bildiradi
3. Ilova sahifasida agar **"Batareya cheklovini o'chirish"** tugmasi
   chiqsa — albatta bosing va ruxsat bering. Bu eng muhim qadam, aynan
   shu 30 daqiqadan keyin o'chib qolish muammosini hal qiladi.

## Fayl tuzilishi

```
sms-forwarder/
├── .github/workflows/build-apk.yml   # GitHub Actions - avtomatik APK build
├── .gitignore
├── capacitor.config.json
├── package.json
├── www/
│   └── index.html                    # Ilova interfeysi (holat ko'rsatish)
└── native-android-plugin/
    └── com/yourname/smsforwarder/
        ├── SmsReceiver.java          # SMS kelganda ishga tushadi
        ├── SmsForwarderPlugin.java   # Capacitor plugin (sozlamalar)
        ├── KeepAliveService.java     # Doimiy fon xizmati (notification)
        ├── QueueWorker.java          # Offline SMS'larni qayta yuborish
        └── BootReceiver.java         # Telefon qayta yonganda ishga tushadi
```

Bot Token va Chat ID kod ichida (`build-apk.yml`dagi `MainActivity`
qismida) allaqachon o'rnatilgan — ilova ochilganda avtomatik ishlaydi,
qo'lda kiritish shart emas.
