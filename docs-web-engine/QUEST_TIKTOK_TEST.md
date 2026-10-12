# Prueba TikTok en Quest 3S — instrucciones concretas

## Evidencia YA obtenida sin instalar nada (2026-10-12, adb lectura)
- Quest 3S serie 3487C10H9T0BKZ conectada (USB + WiFi 192.168.68.100:5555).
- APK aprobada instalada: com.gilleece.moonlightxr.debug 12.1-xr0.4.3 (31407).
- Proveedor WebView REAL: com.android.webview 152.1.0.34.52.1079628222
  (dumpsys webviewupdate) → Chromium 152: la hipotesis "motor viejo" queda
  muy debilitada. Android 14, 3664x1920.
- LimeLog (java.util.logging) SI llega a logcat con tag com.limelight.LimeLog:
  los errores main-frame y HTTP de la APK aprobada son capturables sin
  instalar otra APK. Lo que NO registra la aprobada: proveedor/UA al
  arranque, errores de subrecursos, codigo/descripcion limpios.

## Captura (PC, antes de ponerte el visor)
  adb -s 3487C10H9T0BKZ logcat -c
  adb -s 3487C10H9T0BKZ logcat -v time -s com.limelight.LimeLog chromium > tiktok-live-logcat.txt

## En el visor
1. Abrir DepthsurfXR (MoonlightBase aprobada). Ir a https://www.tiktok.com/
2. Anotar el sintoma LITERAL: pared en blanco / error de pagina (texto
   exacto) / captcha en bucle / carga pero no reproduce / solo falla login.
3. Probar https://m.tiktok.com/ y un video publico. Probar YouTube de control.
4. Abrir Horizon Browser: mismas dos URL de TikTok. Anotar resultado.

## Lectura del log
- MAIN_FRAME + http=403/429/451 → posible bloqueo HTTP del sitio.
- MAIN_FRAME + err=-x (NETWORK_ERROR) → red/DNS/TLS, no JavaScript.
- http=200 MAIN_FRAME sin errores + pared → render/JS: mirar lineas
  "BrowserDiag console level=ERROR" (solo APK experimental las emite;
  la aprobada emite "Browser: console ..." con todos los niveles).
- SUBRESOURCE solamente → la pagina cargo; falla un CDN/recurso.

## Variante de compatibilidad (SOLO si el log la justifica)
403/429 en MAIN_FRAME → A/B UA desktop / m.tiktok.com en copia experimental.
Nunca tocar SSL, nunca evadir controles del sitio, nunca registrar
cookies/tokens/parametros de URL (sanitizeUrl los elimina por host).

## APK experimental
MoonlightBase-diag (copia, NO estable): registra proveedor+UA, main vs
subresource con codigo+descripcion, consola solo ERROR saneada.
Instalar SOLO con autorizacion explicita del usuario.
