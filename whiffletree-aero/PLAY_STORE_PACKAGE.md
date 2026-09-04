# Whiffletree Aero — Play Store Package

Version: 7.20.0 (versionCode 85)
Package: com.mg.whiffletreeaero
Target SDK: 36
Min SDK: 26

## Distribution flavors

- `play`: Google Play build. Sideload updater is disabled and `REQUEST_INSTALL_PACKAGES` is not requested.
- `sideload`: Direct-install engineering/test build. GitHub updater and `REQUEST_INSTALL_PACKAGES` are enabled.

## Build commands

- Play test APK: `gradle assemblePlayDebug`
- Sideload test APK: `gradle assembleSideloadDebug`
- Play Store App Bundle: `gradle bundlePlayRelease`

The Play release AAB must be signed with your private Google Play upload key before upload. Do not commit or share the private keystore.

## Store listing draft (TR)

App name: Whiffletree Aero
Short description: EFT yapısal statik testleri için otomatik whiffletree tasarım ve doğrulama aracı.

Full description:
Whiffletree Aero, hava aracı harici yakıt tankı ve benzeri yapısal test parçaları için yük uygulama sistemlerinin ön tasarımını destekleyen bir mühendislik aracıdır. Kullanıcı test parçası geometrisini, yük uygulama bölgelerini ve signed Fx/Fy/Fz yüklerini tanımlar; uygulama whiffletree topolojisini, actuator gruplamasını, beam/pivot lever oranlarını, actuator ve load-cell taleplerini hesaplar. 2D rig görünümü, kuvvet/moment doğrulaması, bileşen seçimi ve limit/ultimate/unload test simülasyonu sağlar.

Bu yazılım mühendislik ön tasarım ve test planlama aracıdır. Nihai imalat, sertifikasyon veya emniyet kararı için yetkili mühendislik analizi, üretici verileri ve kurum prosedürleriyle doğrulama gerekir.

## Data safety / privacy draft

The app does not require account creation. Engineering inputs are processed locally on the device. The Play flavor does not download and install APK updates. Internet permission remains available for optional catalog/reference functions; if no network feature is used, no engineering input needs to leave the device.

Before publishing, verify every network endpoint used by the current code and complete the Google Play Data safety form according to actual behavior.

## Privacy policy draft

Whiffletree Aero processes user-entered engineering values primarily on the device. The application does not require a user account. The application is not intended to collect sensitive personal information. Network access may be used for optional reference/catalog functions. The Google Play distribution does not request permission to install packages from unknown sources and does not self-install application updates. Users should not enter confidential or export-controlled engineering data into any online feature unless their organization authorizes it.

For a public Play listing, host the final privacy policy on a publicly accessible HTTPS page and enter that URL in Play Console.

## Pre-publication checklist

1. Create a private Play upload keystore and enroll in Play App Signing.
2. Sign `playRelease` AAB with the upload key.
3. Run internal testing on Android 8 through Android 16, including S24 Ultra / Android 16.
4. Verify all AUTO WHIFFLETREE calculations against independent hand calculations and benchmark load cases.
5. Verify 2D/3D labels, negative-force cases, layer insufficiency, actuator overload, and zero-load cases.
6. Complete App content, Data safety, privacy policy URL, content rating, target audience, and store contact fields in Play Console.
7. Capture phone screenshots from the Play build; recommended screens: guided input, AUTO result, 2D rig, proof, component selection, test simulation.
8. Upload the signed AAB to Internal testing before Production.
