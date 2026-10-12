# DepthSurf XR — Arquitectura sobre base Moonlight

Rama: `C:\DepthSurfXR_MoonlightBase` (Moonlight `75d92f2` intacto +
módulo navegador). GPLv3 en ambos lados.

## Mapa de módulos

- `com.limelight.*` (Moonlight, INTACTO salvo manifest):
  `XrRenderer` (sesión XR, capas, rayos, paneles, profundidad),
  `xr-renderer` nativo, prefs, audio, red (sin usar en browser).
- `com.limelight.browser` (NUEVO, nuestro):
  - `BrowserActivity`: anfitriona; arranca XR, crea WebView +
    VirtualDisplay sobre `getInputSurface()`, implementa
    `InputListener`, ciclo de vida.
  - `WebViewInputMapper`: UV→px puro + scroll (JUnit).
- Manifest: `BrowserActivity` = LAUNCHER + VR; `PcView` degradado
  (restaurable); `Game/GameXR` intactos.

## Flujos

Video (Etapa B): WebView → VirtualDisplay(1280×720) →
`inputSurface`/`SurfaceTexture` OES → loop XR (`updateTexImage`) →
warp/DEX → capas. Sin MediaCodec, sin red. Estático = re-presenta.

Entrada (B/C): rayos ML → `InputListener` → handler UI →
`dispatchTouchEvent`/`scrollBy`/teclas al WebView. Sin raycast
propio. Teclas del teclado 3D en Etapa C.

Profundidad (D): `startDepthThread` + `nativeInit(stereoMode…)`
ya existen; el navegador es un frame más. Botón 2D/3D del menú ML.

## Identidad (pendiente estabilizar)

Hoy: `com.gilleece.moonlightxr.debug`, icono ML, label actividad
"DepthSurf Browser". Al estabilizar: applicationId, icono DS y
nombre propios (misma firma debug; sin migración de datos ML).

## Reglas

- Nada de `xr-renderer`/UI ML se reescribe; solo se alimenta.
- Todo lo nuevo vive en `com.limelight.browser` + tests.
- Streaming queda disponible restaurando el manifest.
