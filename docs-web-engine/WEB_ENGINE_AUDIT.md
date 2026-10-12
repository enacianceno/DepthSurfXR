# DepthSurf XR — WEB_ENGINE_AUDIT

Fecha: 2026-10-12 · Base aprobada: `C:\DepthSurfXR_MoonlightBase` (Moonlight XR intacto + módulo `com.limelight.browser`). NO modificado.
Estado: [COMPROBADO]=leído en código/probado local · [HIPÓTESIS] · [PENDIENTE-QUEST]

## Proyecto activo
- Activo y aprobado: `C:\DepthSurfXR_MoonlightBase` (APK probada en Quest 3S por el usuario: YouTube OK).
  Evidencia: `DEPTHSURF_ARCHITECTURE.md`, `BROWSER_SOURCE_ADAPTER.md`, checkpoints `moonlightbase-*-aprobado-*.zip`.
- `C:\Quest3Dbrowser\quest3d` = prototipo modular anterior (web-webview/xr-stub), NO es la base aprobada.
- `C:\MoonlightXR_EN\upstream` = upstream limpio, no tocar.

## Auditoría WebView (fuente: BrowserActivity.java / BrowserUa.java)
| # | Aspecto | Estado actual | Evidencia |
|---|---------|---------------|-----------|
|1|Motor|[COMPROBADO] Android WebView del sistema (proveedor Horizon OS, controlado por Meta); la app NO lo elige ni actualiza|BrowserActivity usa `android.webkit.WebView`; sin `getCurrentWebViewPackage` registrado aún|
|2|User-Agent|[COMPROBADO] UA stock con token `; wv` eliminado|BrowserUa.java:13-18, BrowserActivity.java:215-216 (+test BrowserUaTest)|
|3|JavaScript|[COMPROBADO] activado (principal y popups)|:208, :337|
|4|DOM Storage|[COMPROBADO] activado|:209, :338|
|5|Cookies|[COMPROBADO] terceras partes activadas (principal y popup)|:219, :343|
|6|Redirecciones|[COMPROBADO] http(s) internas; esquemas externos vía ACTION_VIEW, tragados si no hay app|:231-234, `handleLink` :410+|
|7|Popups|[COMPROBADO] multi-ventana + auto-apertura; popup INVISIBLE, comparte cliente Chrome; retorno al opener detectado|`:327-375`, `BrowserNav.isReturnToOpener`|
|8|Multimedia|[COMPROBADO] `mediaPlaybackRequiresUserGesture=false`; fullscreen HTML5 vía onShowCustomView|:210, :263-274|
|9|Permisos web|[COMPROBADO por ausencia] sin `onPermissionRequest` → denegación por defecto (cámara/micrófono)|búsqueda sin override|
|10|SSL|[COMPROBADO por ausencia] sin `onReceivedSslError` → cancelación por defecto (seguro)|sin override|
|11|Errores|[COMPROBADO] log `page error`/`HTTP <code>`/`console` ya cableado (LimeLog)|:237-254, :291-297|
|12|Texto/IME|[COMPROBADO] teclado XR Moonlight → WebViewInputMapper/WebViewKeyMapper; EditText URL con `showSoftInputOnFocus=false`|:161-177|
|13|Captura|[COMPROBADO] WebView en Presentation sobre VirtualDisplay → Surface de XrRenderer (OES)|:119-125, :380-384|
|14|ZipDepth|[COMPROBADO] el frame del VirtualDisplay entra al mismo SurfaceTexture que usaba MediaCodec; ZipDepth/shaders intactos|BROWSER_SOURCE_ADAPTER §1|

## Restricción Horizon OS [HIPÓTESIS fuerte, PENDIENTE-QUEST]
Instalar Chrome NO cambia el proveedor WebView de esta app en Android moderno:
el proveedor es un componente del sistema. En Horizon OS lo actualiza Meta con
el SO. Verificación pendiente: registrar `WebView.getCurrentWebViewPackage()`.

## Conclusión de auditoría
La configuración ya es agresiva (sin `;wv`, cookies 3P, popups, autoplay).
Quedan pocos knobs WebView baratos: UA desktop, popup visible, m.tiktok.com.
Ver TIKTOK_ERROR_DIAGNOSIS.md. Identidad conservada: nombre visible
"DepthsurfXR", créditos Moonlight XR/Gilleece intactos, sin cambio de
applicationId/firma/iconos en esta investigación.
