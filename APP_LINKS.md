# Android App Links beállítás

A kódoldal kész. A hitelesítéshez az alkalmazás aláíró kulcsának SHA-256 fingerprintje szükséges.

Android Studio terminálban:

    ./gradlew signingReport

Windows alatt tipikusan:

    gradlew.bat signingReport

Keresd a használt buildhez tartozó `SHA-256` sort, majd másold be a WordPress adminban:

Értesítések -> Telefonos app -> Android app SHA-256

Ellenőrzés:

    https://www.hoc.hu/.well-known/assetlinks.json

Release APK/AAB esetén a release tanúsítvány fingerprintjét kell megadni. Ha később Google Play App Signingot használsz, a Play Console által használt app signing certificate SHA-256 értéke lesz a megfelelő.
