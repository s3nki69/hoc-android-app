# HOC Android – APK készítése GitHubon, Android Studio nélkül

Ez a projekt már tartalmazza az automatikus GitHub Actions buildet. A GitHub szervere elkészíti az APK-t, ezért a saját számítógépre nem kell Android Studio, Java vagy Android SDK.

## Első alkalom – kb. 5 perc beállítás

### 1. Készíts egy GitHub repositoryt

1. Lépj be a GitHubra.
2. Válaszd a **New repository** lehetőséget.
3. Névnek például: `hoc-android`.
4. A fejlesztés idejére célszerű **Private** repositoryt választani.
5. Ne kérj automatikus README-t vagy más kezdőfájlt.
6. Hozd létre a repositoryt.

### 2. Töltsd fel ezt a projektet

1. Csomagold ki a `hoc-android-app-2.0.2-github.zip` fájlt.
2. Nyisd meg benne a `hoc-app` mappát.
3. A GitHub repositoryban válaszd: **Add file → Upload files**.
4. Húzd be a `hoc-app` mappa **teljes tartalmát**. Fontos, hogy a `.github` mappa is bekerüljön.
5. Alul válaszd a **Commit changes** gombot.

A feltöltés után a build automatikusan elindul.

## APK letöltése

1. A repository tetején nyisd meg az **Actions** fület.
2. Kattints a legutóbbi **HOC Android APK** futásra.
3. Várd meg, amíg zöld pipa jelenik meg. Az első build általában néhány perc.
4. Az oldal alján az **Artifacts** résznél kattints erre:
   `HOC-Android-2.0.2-debug`
5. A GitHub ZIP-ben tölti le az artifactot.
6. Csomagold ki. Ebben lesz:
   `HOC-Android-2.0.2-debug.apk`
7. Másold a telefonra és telepítsd.

## Későbbi verziók

Ha új app-verziót kapsz, a repository tartalmát frissíted az új fájlokkal. Minden `main` vagy `master` ágra kerülő módosítás után a GitHub automatikusan új APK-t buildel.

Kézzel is indítható:

**Actions → HOC Android APK → Run workflow**

## Mit jelent a „debug APK”?

A debug APK teljesen telepíthető és alkalmas a fejlesztés, a kezelőfelület, a cache, az értesítések és a cikkkezelés tesztelésére.

A végleges publikálás előtt külön, saját tartós aláírókulccsal készítünk **release APK/AAB** verziót. Ez azért fontos, mert az Android-frissítések és az ellenőrzött HOC.hu App Links a végleges aláírókulcshoz kötődnek.

A fejlesztési fázisban ezért nem teszünk privát aláírókulcsot a GitHub repositoryba.

## App Links a debug buildben

A `hoc.hu` linkek kezelésének kódja már benne van az alkalmazásban, de az automatikus Android App Links hitelesítést a végleges aláírással állítjuk be. A debug buildnél a fő cél az alkalmazás funkcióinak tesztelése.

## Ha a build piros hibával leáll

Nyisd meg az **Actions → HOC Android APK** futást, kattints a piros lépésre, és másold be nekem a hibaüzenetet. Abból célzottan javítható a projekt.
