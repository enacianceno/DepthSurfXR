# DepthSurf XR — TIKTOK_ERROR_DIAGNOSIS

## Síntoma registrado
https://www.tiktok.com/ muestra error de página en Quest 3S (reporte del
usuario). NO existe logcat ni descripción literal del síntoma en el repo.
[PENDIENTE-QUEST] — sin esto, la causa raíz NO puede afirmarse.

## Evidencia local obtenida (2026-10-12, PC)
- `HEAD https://www.tiktok.com/` con UA Quest simulado → **200** (sin bloqueo
  de red/DNS desde PC). Google → 200. m.tiktok.com/YouTube: el cliente HTTP de
  PowerShell abortó por estado (no es evidencia de bloqueo del sitio).
- El cliente ya registra `onReceivedError` (código+descripción), HTTP status y
  consola JS (BrowserActivity.java:237-297). El experimento es LEERLOS, no
  añadir instrumentación nueva salvo el clasificador del prototipo.

## Evidencia REAL de Quest (2026-10-12, adb solo lectura, sin instalar nada)
- Proveedor WebView: com.android.webview **152.1.0.34.52** (Chromium 152),
  dumpsys webviewupdate en Quest 3S 3487C10H9T0BKZ, Android 14.
  → La hipótesis 2 (motor viejo) queda muy debilitada: el motor es moderno.
- APK aprobada instalada: com.gilleece.moonlightxr.debug 12.1-xr0.4.3.
- LimeLog llega a logcat como tag com.limelight.LimeLog → el error de la
  APK aprobada ES capturable por ADB sin instalar otra APK (ver
  QUEST_TIKTOK_TEST.md). Huecos de la aprobada: no registra proveedor/UA,
  ni subrecursos, ni código/descripción limpios → copia MoonlightBase-diag.

## Ranking de hipótesis (actualizado)
1. **Anti-bot/fingerprint de TikTok** [HIPÓTESIS principal]: TikTok aplica
   control de riesgo (firma JS, canvas/WebGL, sensores). Un Quest (WebGL de
   XR2, sin sensores de teléfono, UA WebView aunque sin `;wv`) no parece un
   teléfono Chrome. Ningún `WebSettings` controla el fingerprint.
2. ~~Proveedor WebView atrasado~~ [REFUTADA en la práctica: Chromium 152].
3. **Popup invisible / cookies de login** [HIPÓTESIS]: el popup OAuth es
   invisible (BrowserActivity.java:336); flujos que esperan popup visible o
   postMessage pueden romperse. Afectaría LOGIN más que la carga pública.

## Experimento decisivo (único, antes de cambiar motor)
En Quest, sin instalar nada nuevo salvo la APK candidata autorizada:
1. `adb logcat` filtrado `Browser: page error|Browser: HTTP|Browser: console`
   en tiktok.com y m.tiktok.com. 403/429=HTTP_BLOCK, ERR_*=NETWORK,
   200+blanco=JS_OR_BLANK (clasificador en prototype/WebEngineDiagnostics.java).
2. Anotar síntoma literal: pared blanca, captcha en bucle, o solo login roto.
3. Registrar paquete/versión del proveedor WebView + UA completa.
4. Control: Horizon Browser en las mismas URL.
5. A/B: UA desktop (prototipo), popup visible, m.tiktok.com.

## CAUSA CONFIRMADA (captura en vivo 2026-10-12, results/tiktok-live-logcat.txt)
TikTok SI carga (consola RemoteLoader, fmp, ws.opened; cero "page error",
cero HTTP de error). Al hacer scroll, la propia pagina dispara enlaces de
despertar de app ("silence_awaken"): snssdk1233://aweme/detail/...,
snssdk1340://..., intent://aweme/... (gd_label=click_wap_silence_awaken).
Sin app de TikTok instalada, el respaldo web salta a la pagina de
instalacion: eso es lo que el usuario ve. NO es fallo de motor, DNS ni SSL.

## ARREGLO (solo copia experimental MoonlightBase-diag, estable intacta)
BrowserNav.isAppInstallLink(url, currentUrl) + guarda en handleLink:
se tragan snssdk*/musically/tiktok/market, intent de aweme/paquetes
TikTok, y tiendas (play.google.com/apps.apple.com) SOLO cuando la pagina
actual es TikTok. URLs tecleadas en la barra no pasan por ahi.
Tests: :app:testNonRootDebugUnitTest com.limelight.browser.* VERDE,
incl. tiktokAwakenLinksAreBlocked. APK experimental construida
(app-nonRoot-debug.apk, 102706541 B, SHA256
CEECDCB00CF9B5210C5C8676D04320A8F838C97891CAC1402D36AED6E725D0D9),
NO instalada: requiere autorizacion y prueba fisica del usuario.

## Seguridad
Sin desactivar SSL, sin evadir controles del sitio, sin registrar cookies,
tokens ni contraseñas. Si TikTok bloquea el fingerprint, la respuesta honesta
es documentarlo, no hacer spoofing agresivo.
