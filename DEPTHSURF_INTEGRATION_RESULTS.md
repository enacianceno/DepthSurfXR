# DepthSurf XR — Resultados de integración (A + B)

## Etapa A — Moonlight funcional: COMPLETADA

- Build `assembleNonRootDebug`: SUCCESS (7m13s).
- APK `12.1-xr0.4.3` (102 MB) SHA-256
  `2DDE4A7E22DB4B51C7E9053ED7C6EAE238F334D7A6673086027A5D0E42EFE7A1`
  (no instalada; usuario ya validó upstream en Quest).
- JVM 335/335 · C 16/16 en Quest (NDK clang + adb).
- Cambios vs upstream: `local.properties`, submódulo poblado
  (pin exacto), Gradle home local. Cero código.

## Etapa B — Navegador dentro de Moonlight: CÓDIGO COMPLETO

Nuevos: `browser/BrowserActivity.java`,
`browser/WebViewInputMapper.java`, `browser/WebViewInputMapperTest`,
manifest (actividad + launcher). Tocados de ML: ninguno (.java/.c).

- Build: SUCCESS (6m22s). APK (102.7 MB) SHA-256
  `1E00A65277082425DA8C8A0E0174EF8C85F806FD646C87B1CE059D14B118BDE9`
  Launcher: `DepthSurf Browser`. Doc: `BROWSER_SOURCE_ADAPTER.md`.
- JVM 341/341 (6 nuevas del mapper, 0 fallos).
- Entrada B: puntero/botón/scroll/exit → WebView; teclas = stub
  con log (Etapa C); gamepad/mirada-relativa ignorados.
- Riesgos abiertos (ver adapter R1–R4): orden superficie/VD,
  clamp UV (testeado), hilos (handler), foco de campos (C).

## Intacto vs adaptado

- Intacto: `XrRenderer`, `xr-renderer`, menús, rayos, sliders,
  teclado 3D, profundidad, `Game/GameXR`, red, audio.
- Adaptado: manifest (launcher), + paquete `browser` nuevo.

## Etapa C — Teclado 3D a campos: CÓDIGO COMPLETO

Reutiliza `VrKeyboard` intacto + `WebSink` propio: Unicode →
`ACTION_MULTIPLE`, VK → `KeyEvent` DOWN/UP (`WebViewKeyMapper`,
9 tests), modificadores como META (ctrl+A etc.), foco antes de
inyectar. Build SUCCESS (2m28s), JVM **350/350**. APK
`DepthSurfXR-EtapaC.apk` SHA-256
`B0B6A3A46DD8FE5DBFAA21BF9457AC42DB641CBAF9A9BC8745910F6156448CEE`.

## Etapa D — Profundidad sobre web: SIN CÓDIGO (verificado)

- Prefs frescas → `zipdepth` → `DEPTH_MODE_MODEL` por defecto;
  `startDepthThread` corre sobre el frame del loop (textura de
  pantalla = WebView, no decodificador). `nativeDepthDue` decide
  captura; fallo = stereo plano, app usable.
- Toggle 2D/3D: pestaña Display/3D nativa de Moonlight (runtime).
- Prueba en Quest: log `depth model zipdepth…`, stats de mapas,
  P10. Binario idéntico a Etapa C.

## Pendiente de Quest (todas las funcionales)

P1 (tap), P2/P3 (rayo al mando), P8 (curva+selección), P10 (3D),
P4–P7 (barra/engrane/mover/ocultar — ya son ML nativo), P9 (C),
video (E). Instalar Etapa B solo con autorización.

## Siguiente

Etapa C (teclas→campos), D (profundidad sobre web + botón 2D/3D),
E (video/métricas). Checkpoint: `moonlightbase-etapaB-*.zip`.
## RayDot (2026-10-11)

- Cursor dot always drawn at the ray landing point, on a surface or at
  the free beam end in the room (xr_layers.c, addPointerLayers):
  removed the !beamFree gate. Size/art unchanged (2.2 cm at 3 m, dark
  rim for bright pages); C test test_controller.c untouched.
- Stick scroll: no code needed. Native updateScroll (xr_input.c) winds
  either thumbstick (deadzone 0.30, up to 7.5 clicks/s) into IN_SCROLL
  while the pointer is on the page; Java forwards onVrScroll and
  BrowserActivity.scroll() moves the WebView 240 px/click, wheel-style
  (push up = page up). Quest validation pending.
- APK dist/DepthSurfXR-RayDot.apk, tests 373/373 green.
## APROBADO EN QUEST (2026-10-11) - "Chayane"

- Usuario valido en visor: navegador + YouTube (concierto completo),
  teclado 3D con especiales, rayos Moonlight con punto, esquinas
  redondeadas + glow, zoom, nav H/X/*/F/P, 2D/3D. Sin regresiones.
- Estado aprobado = codigo RayDot + dist/DepthSurfXR-RayDot.apk
  (SHA-256 A098C3F7D1075755BEEDF403F3CBC08DFA916815C098EE61483731D1396ACAFE).
- Checkpoint: moonlightbase-aprobado-20261011.zip (no instalar mas
  versiones hasta definir v-siguiente).

## Hoja de ruta v-siguiente (pendiente de diseno)

1. Boton de aislamiento (lock): con aislamiento ON, la barra/engrane/
   mover/rayos/teclado siguen activos pero NADA dentro del contenido
   responde (sin taps, sin scroll, sin hover al WebView). Diseno:
   nuevo BAR_SLOT_LOCK (8) en la barra + bandera Java en
   BrowserActivity que ignora callbacks de contenido
   (onVrPointerMove/onVrButton/onVrScroll; onVrKey se conserva).
   Tamano estimado: S/M.
2. Identidad visual propia: paleta DepthSurf (navy/cian/violeta) en
   barra/paneles/cursor, glifo DS, strings values/values-es
   (el resto de idiomas conserva fallback), About mantiene atribucion
   GPL (LICENSE.txt intacto). Tamano: M.
3. Efecto pop-out (objetos fuera del marco): hoy el warp es gather
   dentro del quad (u_disparity, edge fade pegado al borde); el pop
   real exige overscan + marco oclusor a profundidad de pantalla en
   xr_shaders.c/xr_layers.c. Propuesta: spike de investigacion antes
   de comprometer la v-siguiente. Tamano: M/L, riesgo medio.
## Lock + Rename (2026-10-11)

- Visible name DepthSurfXR: app_label resValue in app/build.gradle
  (debug + release) + BrowserActivity label; verified in the binary
  with aapt2 (application-label:'DepthSurfXR'). ManifestLabelTest (3)
  guards the chain: activity label, nonRoot wiring, gradle resValues.
- Content lock button: BAR_SLOT_LOCK (8, past the 3D switch) with
  padlock faces (XrPanels.buildLockButtons), session-only ctx->lockOn,
  IN_ISOLATION channel to Java, onVrIsolation callback. While locked,
  BrowserActivity ignores pointer/button/scroll to the page (Isolation
  class + IsolationTest); bar, panels, move, rays and typing keep
  working; engaging mid-drag releases the press. Game.java ignores it.
- Tests 379/379 green (XrPadTest tail extended for IN_ISOLATION).
- APK dist/DepthSurfXR-Lock.apk, checkpoint moonlightbase-lock-*.
## TikTok access (2026-10-11)

- Root causes in the stock WebView setup: the "; wv" UA token flags an
  embedded view (large sites restrict it), no third-party cookies, no
  multiple windows (login/share popups died), and app links
  (intent:/tiktok:/market:) died as error pages.
- Fix: BrowserUa strips "; wv" (plain Chrome UA), third-party cookies
  on, window.open routed into the main view, external schemes handed
  to Android (quiet when no app), page/HTTP errors logged via LimeLog
  for remote diagnosis. BrowserNav.isExternalScheme is the pure seam.
- Tests 384/384 green (BrowserUaTest 3, BrowserNavTest +2).
- APK dist/DepthSurfXR-TikTok.apk, checkpoint moonlightbase-tiktok-*.
  Quest proof pending: tiktok.com loads, feed scrolls, video plays.
## IdentityFix (2026-10-11)

- Quest showed "Moonlight XR Debug" for two stacked reasons, both
  verified: (1) the installed com.gilleece.moonlightxr.debug is an OLD
  binary (lastUpdate 2026-10-11 17:17, minSdk 23, label Moonlight XR
  (Debug) per aapt of RayDot); the renamed Lock/TikTok builds were
  never installed. (2) My rename used "DepthSurfXR" (capital S);
  the exact requested spelling is "DepthsurfXR".
- Fix: app_label = DepthsurfXR (debug+release+root), launcher
  activity label = DepthsurfXR, applicationId nonRoot =
  com.depthsurf.xr (+ .debug suffix => com.depthsurf.xr.debug),
  root = com.depthsurf.xr.root. ManifestLabelTest tracks the exact
  spelling (7 occurrences).
- Safety: Java stays com.limelight (JNI untouched), namespace kept,
  single provider uses poster.${applicationId} (verified in binary:
  poster.com.depthsurf.xr.debug), no custom permissions/aliases.
  New id cannot overwrite com.gilleece.moonlightxr (upstream, kept)
  nor the old debug build (kept until user authorizes uninstall).
  LICENSE.txt + ko-fi/github attribution untouched.
- Binary proof (aapt2): package com.depthsurf.xr.debug, v31407 /
  12.1-xr0.4.3, application-label + launchable-activity label =
  DepthsurfXR, no Moonlight in badging, icons present.
- Device proof (adb read-only): packages com.gilleece.moonlightxr +
  com.gilleece.moonlightxr.debug present, no depthsurf yet.
- Tests 384/384. APK dist/DepthsurfXR-IdentityFix.apk (102509105 B),
  SHA-256 22341413EA47AC172080A0310DA4D8EF0584745F64AC9F75CE309C257059C483.
  Build log: dist/DepthsurfXR-IdentityFix.build.log.
## Aspect + login hardening (2026-10-11)

- Name VALIDATED in visor by user (DepthsurfXR shows in Unknown Sources).
- TikTok login: root cause still needs the on-device error text (Quest
  fell off adb, 10061, before logcat could be pulled). Hardened anyway:
  WebChromeClient now answers JS alert/confirm/prompt (pages that
  alert() with no handler hang forever, blocking login/captcha) and
  logs console messages + page/HTTP errors via LimeLog for remote
  diagnosis. Next login attempt will leave the exact failure in logcat.
- Portrait: corner drag is uniform-scale by design (GRAB_RESIZE along
  the half diagonal) and video dims are session-fixed (swapchain/eye
  buffers sized at start), so free-reshaping mid-session would need
  native realloc surgery. Instead: aspect flip button on the bar
  (BAR_SLOT_ASPECT, past head aim, momentary, IN_ASPECT pulse) that
  restarts the XR session 1280x720 <-> 720x1280. Page survives
  (WebView untouched); VirtualDisplay detaches, resizes and rebinds
  to the new surface; aspect persists in prefs; one flip-back retry
  before fatal. Aspect pure class + AspectTest.
- Tests 387/387 (XrPadTest tail = IN_ASPECT). APK
  dist/DepthsurfXR-Aspect.apk (102862362 B), SHA-256
  E22A8C9A52FFED97349887C6D5C1770F27FD347584FB6CD290C547AAD1A4642E.
  Identity re-verified in binary (com.depthsurf.xr.debug / DepthsurfXR).
- Quest unreachable (adb 10061): install + login-diagnosis + portrait
  proof all pending. Quest needs USB tcpip reactivation.
## Resize libre + OAuth popups (2026-10-11)

- Resize estilo PC: agarrar cualquier esquina y arrastrar cambia las
  proporciones (no solo escala). Esquina opuesta anclada, centro
  recalculado, ejes independientes, clamps 0.8..8 m y aspecto 2:1..1:2.
  Implementado con ctx->contentAspect (h/w) desacoplado del video:
  quad, barra, esquinas, hit-test, glow y radio siguen la misma
  geometria, asi 3D/separacion/convergencia, glow y esquinas
  redondeadas se conservan al estrechar. Gaze y rooms conservan el
  comportamiento uniforme anterior. Sin realloc GL: buffers fijos.
- Contenido en vivo: canal IN_ASPECT_VALUE cada frame ->
  onVrAspectSize -> VirtualDisplay.resize + SurfaceTexture
  setDefaultBufferSize + relayout (throttle 120 ms + pasada final).
  Aspect.pixelsFor: area constante ~720p, pares, presets exactos.
  La forma libre persiste (aspect_live) y sobrevive reinicios.
- Depth en formas no-16:9: la captura estira el frame a 512x288 para
  el modelo; los mapas siguen validos con leve anisotropia. Sin
  cambios de codigo. Documentado, pendiente ojo en visor.
- Login Google/OAuth: causa raiz del blanco muerto = mi onCreateWindow
  anterior mandaba TODO el popup a la vista principal, rompiendo
  window.opener/redirect de retorno. Ahora el popup vive invisible
  con su flujo propio (misma UA/cookies/DOM) y solo navegaciones de
  regreso al sitio opener (BrowserNav.isReturnToOpener, base domain)
  pasan a la vista principal; onCloseWindow destruye popups.
- Tests 392/392 (AspectTest 6, BrowserNavTest +2 popup, XrPadTest).
- APK dist/DepthsurfXR-Resize.apk (102994573 B), SHA-256
  745342CFFECD417DFAD075E14334013096C246AFEFDAFB1C02188AE5CEAD09C5.
## Esquinas sin boton (2026-10-12)

- A peticion: fuera el boton flip de formato de la barra; la forma
  solo cambia agarrando esquinas (resize libre intacto). Retirado:
  BAR_SLOT_ASPECT/HOVER/celda, pulso IN_ASPECT, onVrAspect,
  restartForAspect/swapXrSession, arte del boton, presets con
  estado (Aspect ahora solo pixelsFor/clamps). Canal vivo
  IN_ASPECT_VALUE -> onVrAspectSize -> resize intacto; atlas 14
  celdas (lock cierra), cola XrPadTest actualizada.
- Tests JVM 390/390 (AspectTest 4: presets, cuadrado, clamps,
  extremos drag). Tests C pendientes en Quest (sin toolchain host).
- APK dist/DepthsurfXR-Corners.apk (102512343 B), SHA-256
  A6F5437470A3F6AB2A937F910527A8A2B7B12BE5D02D97AA9CBCBDB029E45097.
- Sin instalar: adb caido (ni Wi-Fi ni USB visibles); faltan fotos
  del usuario (video vs forma) y prueba de login TikTok con popups.
- APROBADO en visor (2026-10-11): resize por esquinas excelente,
  Facebook Reels OK. TikTok-web no funciona (se descarta web).
