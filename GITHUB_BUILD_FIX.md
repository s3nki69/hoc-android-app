# GitHub build javítás – 2.0.2

A 2.0.1-ben a `gradlew` fájl hibás volt: nem a `gradle-wrapper.properties` által megadott Gradle-verziót használta, hanem a GitHub runneren éppen elérhető globális Gradle-t. 2026 szeptemberében ez Gradle 9.7.1 lett, ami nem kompatibilis a projekt Kotlin 1.9.20 / Android Gradle Plugin 8.2.0 párosával.

A 2.0.2 `gradlew` fájlja ezért közvetlenül Gradle 8.2.1-et használ. Ez akkor is működik, ha a repositoryban még a korábbi GitHub Actions workflow maradt, amely egyszerűen ezt futtatja:

    ./gradlew assembleDebug

## Frissítés a GitHub repositoryban

A legegyszerűbb: töltsd fel a 2.0.2 csomag teljes `hoc-app` mappájának tartalmát a repository gyökerébe, felülírva a meglévő fájlokat.

A legfontosabb cserélendő fájl a repository gyökerében lévő:

    gradlew

Ha a `.github/workflows/android-build.yml` is felülíródik, az új workflow már külön ellenőrzi is, hogy Gradle 8.2.1 fut-e.

A sikeres build végén az Artifacts között ezt keresd:

    HOC-Android-2.0.2-debug
