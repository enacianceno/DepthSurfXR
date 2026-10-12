# DepthSurf XR — ARCHITECTURE_RECOMMENDATION

## Recomendación (menor riesgo): mantener WebView + Moonlight, experimentar en rama
1. PRESERVAR intacta la base aprobada `C:\DepthSurfXR_MoonlightBase`:
   OpenXR, rayos, teclado XR, barra, ZipDepth y la ruta
   WebView→VirtualDisplay→OES→ZipDepth quedan como referencia funcional.
2. Ejecutar primero el experimento decisivo de TikTok (logcat + síntoma +
   proveedor + control Horizon Browser + A/B UA desktop/popup visible/
   m.tiktok.com) con el prototipo aislado en `prototype/`
   (WebEngineDiagnostics + test JUnit, paquete com.limelight.browser,
   pensado para copiarse SOLO en una copia experimental, nunca en la base).
3. Solo si la evidencia señala motor/fingerprint: spike GeckoView
   time-boxed cuya PRIMERA puerta es entregar frames continuos a la
   Surface/SurfaceTexture OES sin readback CPU. TikTok se prueba después
   de la puerta de frames, no antes. ZipDepth después de ambas.
4. Chromium completo: descartado salvo que fallen 2 y 3 con evidencia.

## Qué NO hacer
No reconstruir UI/rayos/teclado, no tocar upstream Moonlight/DuelXR/
XWAQuest/Stremio3D_XR, no instalar APK en Quest sin autorización, no
generar APK "para demostrar que compila" sin integración útil.

## Siguiente artefacto
APK experimental (solo tras autorización de instalación) = copia de la base
+ overlay del prototipo, identificada como PENDIENTE DE VALIDACIÓN EN QUEST,
con SHA-256 y registro de compilación. Hoy no se genera: sin adb ni
autorización, una APK nueva no aportaría evidencia.

## Créditos
Moonlight XR / Gilleece y colaboradores conservados; nombre visible
"DepthsurfXR" sin cambios de identidad, applicationId, firma ni iconos.
