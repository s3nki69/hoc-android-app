# HOC Android 2.0.3 – DNS javítás

A 2.0.2 több API-, cikk- és kép URL-nél fixen a `www.hoc.hu` hostot használta.
A 2.0.3 elsődlegesen a `https://hoc.hu` hostot használja, és a WordPresstől kapott
`www.hoc.hu` URL-eket is automatikusan `hoc.hu` címre normalizálja.

Ez a javítás az `Unable to resolve host "www.hoc.hu"` hibát célozza.

Ha a hiba még ezután is DNS jellegű, kapcsold ki próbaképpen a VPN-t / privát DNS-t,
és próbáld meg Chrome-ban közvetlenül a https://hoc.hu címet.
