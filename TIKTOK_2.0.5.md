# HOC Android 2.0.5 – TikTok fül visszaállítása

A `hoc-app-beta2.apk` elemzése alapján a korábbi alkalmazás külön `TikTokFragment` képernyőt tartalmazott, amely a `https://www.tiktok.com/@hoc.hu` profilt WebView-ban nyitotta meg.

A 2.0.5-ben visszakerült:

- a **TikTok** fül a **Videók** és az **Értesítések** közé;
- a HOC TikTok profil beágyazott megjelenítése;
- JavaScript, DOM storage és sütik támogatása a TikTok működéséhez;
- TikTokon belüli navigáció az appban;
- külső domainek megnyitása az Android alapértelmezett böngészőjében/appjában;
- WebView visszalépés kezelése;
- a korábbi APK-ban is meglévő mobil böngésző user-agent és cookie-banner elrejtés.

A PWA/Push és a 2.0.4 kuponos módosítások változatlanul megmaradtak.
