# HOC Android 2.0.6 – natív Push

A 2.0.6-os app már képes Firebase Cloud Messaging (FCM) értesítések fogadására, de **nem tartalmaz Firebase titkos kulcsot és nem igényel `google-services.json` fájlt az APK-ban**. A nyilvános Firebase-konfigurációt induláskor a HOC.hu WordPress bővítménytől kapja meg.

## Mi változott?

- Az **Értesítések** fül teljesen natív: Push BE/KI, témák, legutóbbi értesítések, figyelőlista.
- A beállításokhoz nem nyit külső weboldalt.
- Ha FCM be van állítva, az értesítéshez az appnak nem kell folyamatosan futnia.
- FCM mellett a WorkManager csak 6 óránkénti tartalék szinkronra marad meg.
- FCM nélkül a korábbi 15 perces takarékos háttérellenőrzés működik tovább.
- Az értesítésre koppintva a megfelelő HOC cikk nyílik meg az appban.

## Firebase beállítása

1. Hozz létre egy Firebase projektet.
2. Adj hozzá Android alkalmazást ezzel a csomagnévvel: `hu.hoc.app`.
3. Töltsd le a `google-services.json` fájlt.
4. A Firebase/Google Cloud Service Accounts résznél készíts egy service account privát kulcsot JSON formátumban.
5. WordPressben nyisd meg: **Értesítések → Telefonos app**.
6. A **Natív Android Push (Firebase Cloud Messaging)** blokkban töltsd fel a `google-services.json` fájlt és a service account JSON-t, majd kapcsold be az FCM-et és mentsd a beállításokat.
7. Indítsd újra a HOC Android appot. Az **Értesítések** fülön az állapotnak rövid időn belül „Azonnali natív Push aktív” szövegre kell váltania.
8. WordPressben az **Értesítések → Áttekintés → Egyszeri Push üzenet** blokkal tesztelhető.

## Biztonság

A service account JSON **titkos**. Ne kerüljön GitHubra, APK-ba vagy nyilvános tárhelyre. A 2.0.6 app csak a Firebase nyilvános kliensazonosítóit kapja meg a HOC szervertől; a service account kulcs kizárólag a WordPress oldalon marad.
