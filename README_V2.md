# HOC Android 2.0.1

Ez a verzió a korábbi natív Android app továbbfejlesztett alapja.

## Online APK build Android Studio nélkül

A projekt tartalmaz GitHub Actions buildet. A legegyszerűbb tesztelési módhoz lásd az `ONLINE_BUILD.md` fájlt. A GitHub elkészíti a telepíthető debug APK-t, így helyben nem kell Android Studio.

## Fő változások

- A felső HOC felirat helyett valódi HOC logó jelenik meg.
- A fülek között nincs több oldalirányú swipe, ezért függőleges görgetésnél nem lapoz el véletlenül.
- A cikklista 50 helyett 15 elemet tölt első körben, majd görgetéskor lapozva kér újabbakat.
- Az első cikkoldal helyi cache-be kerül, ezért következő indításkor azonnal megjelenik, miközben a háttérben frissül.
- A WordPress REST válaszból kisebb, közepes képméretet kér az app, ha rendelkezésre áll.
- A cikkek az alkalmazáson belüli olvasón nyílnak meg.
- A HOC.hu linkekhez Android App Link intent-filter került.
- A YouTube lista már nem a teljes YouTube HTML-t kaparja, hanem a hivatalos csatorna RSS feedet használja.
- A videó- és kuponlista is kapott egyszerű helyi cache-t.
- Új Értesítések fül: témák kiválasztása és figyelt cikkek/termékek kezelése.
- Cikkolvasóban szív gombbal közvetlenül figyelhető az adott cikk/termék.
- Android háttér-értesítések: a HOC Értesítések plugin inboxát WorkManager ellenőrzi.

## Fontos az Android értesítésekről

Ez a build nem Firebase Cloud Messaginget használ. A már meglévő HOC Értesítések backend inboxát ellenőrzi Android WorkManagerrel. Emiatt az értesítés nem garantáltan azonnali: az Android energiatakarékosságától függően jellemzően legfeljebb kb. 15 perc késés lehet.

Az azonnali böngészős Web Push továbbra is a HOC PWA erőssége. Ha a natív appban is másodperces, valódi push kell, a következő lépés az FCM integráció.

## Szükséges WordPress plugin

A natív app új értesítési funkcióihoz a `HOC Értesítések 2.3.0` vagy újabb kell.

## Android App Links

A manifest már kezeli a `https://hoc.hu/...` és `https://www.hoc.hu/...` linkeket. A teljes automatikus hitelesítéshez add meg az app aláíró tanúsítványának SHA-256 ujjlenyomatát a WordPressben:

`Értesítések -> Telefonos app -> Android app SHA-256`

A plugin ezután kiszolgálja:

`https://www.hoc.hu/.well-known/assetlinks.json`

A SHA-256 érték Android Studio/Gradle alatt a signing reportból kérhető le.

## Tesztelési sorrend

1. Telepítsd a HOC Értesítések 2.3.0 plugint.
2. Android Studioban nyisd meg ezt a projektet.
3. Build és telepítés telefonra.
4. Indítsd el az appot: a Friss lista cache nélkül első alkalommal hálózatról tölt, később azonnal cache-ből indul.
5. Görgesd a Videók lapot lefelé: oldalirányú lapváltásnak nem szabad történnie.
6. Nyiss meg egy cikket: az appon belüli olvasó jelenik meg.
7. Nyomd meg a szív gombot: a cikk megjelenik az Értesítések -> Figyelt cikkek között.
8. Az Értesítések fülön válassz témákat és mentsd.
9. WordPressből küldj teszt/új értesítést; az Android háttérszinkron a következő ellenőrzésnél rendszerértesítést készít.
