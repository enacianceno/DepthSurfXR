# DepthSurf XR — CHROMIUM_VS_GECKOVIEW

Criterio: compatibilidad demostrable SIN perder el pipeline Moonlight/ZipDepth.
[PENDIENTE] = no probado en Quest. Nada de esto afirma compatibilidad no probada.

## Opción A — WebView/Chromium actual (RECOMENDADA por ahora)
- Ya integrada: entrada XR, textura GPU-GPU, ZipDepth, YouTube y Facebook
  Reels reportados OK por el usuario. Coste de motor en APK: ~0 MB.
- Límites: proveedor fijado por Horizon OS; sin control de fingerprint;
  knobs restantes solo UA desktop / popup visible / m.tiktok.com.

## Opción B — GeckoView
- [COMPROBADO] Artefacto publicado: maven.mozilla.org org.mozilla.geckoview,
  latest citado 158.0.20261007201254 (metadata consultada 2026-10-12). ~30 MB.
- Ventaja real: motor propio actualizable por la app, distinto de Horizon.
- [HIPÓTESIS] Podría esquivar un bloqueo por token/versión WebView.
- [PENDIENTE] TikTok/YouTube en GeckoView-sobre-Quest, IME con teclado XR, y
  sobre todo ENTREGA DE FRAMES (ver GPU_TEXTURE_INTEGRATION.md). Sin eso, no
  hay ventaja demostrable: es solo teoría.
- Wolvic (upstream/wolvic, CHROMIUM.md presente) prueba que un navegador XR
  alternativo en Quest es viable como producto, no que su salida alimente
  nuestro ZipDepth.

## Opción C — Chromium completo
- CEF no soporta Android; la vía es //android_webview (AwContents) crudo.
- Coste: build Linux x86-64, ≥100 GB disco, >16 GB RAM, horas; APK +80–180 MB;
  PSS/tab inferido 300–600 MB contra XR+ZipDepth en 8 GB totales [HIPÓTESIS].
- Sin Widevine/H.264/Google services; rebases de seguridad cada ~4 semanas
  a cargo nuestro. Licencia BSD-3 + cientos de licencias de terceros.
- Chrome Custom Tabs NO sirve: Activity del navegador, sin Surface/frame
  accesible → no puede alimentar ZipDepth. Descartado como integración.

## Veredicto
Chromium completo: NO salvo último recurso. GeckoView: solo spike con
puerta de frames (GPU_TEXTURE_INTEGRATION.md §B). WebView: seguir mientras
el experimento TikTok no demuestre causa de motor/fingerprint.
