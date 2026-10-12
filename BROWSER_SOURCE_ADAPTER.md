# Adaptador WebView → Moonlight XR (Etapa B)

Cómo el navegador local alimenta al renderer Moonlight.
Sin Sunshine, sin PC. `XrRenderer.java` intacto; `xr-renderer`
solo suma máscara de esquinas redondeadas (ver §6).

## 1. Ruta de video (GPU→GPU, cero copias)

Moonlight original:
`MediaCodec` → `XrRenderer.getInputSurface()` (`Surface` sobre
`SurfaceTexture` OES en el contexto EGL del hilo XR) →
`onFrameAvailable` → `pendingFrames++` → loop XR
`updateTexImage()` + warp/depth (`XrRenderer.java:671,1158`).

Adaptador DepthSurf:
`XrRenderer.start(activity, WEB_W, WEB_H, prefs)` (mismos tipos;
`PreferenceConfiguration` esPOJO de prefs, sin conexión) →
`DisplayManager.createVirtualDisplay(..., getInputSurface())` con
un `WebView` en `Presentation` → cada frame web entra al mismo
`SurfaceTexture` que usaba el decodificador.

- Formato: el `Surface` acepta cualquier productor (codec o
  `VirtualDisplay`); el OES + `texMatrix` ya existen.
- Tamaño: `setDefaultBufferSize(w,h)` con `w,h` nuestros
  (p. ej. 1280×720); `videoWidth/Height` nutren aspecto/barra.
- Sin frames (página estática): `pendingFrames==0`, el loop XR
  re-presenta (igual que pausa de stream). Sin drift de rate: la
  inferencia va por tiempo, no por conteo (`XrRenderer.java:1163`).

## 2. Entrada (sin segundo raycast)

`XrRenderer.InputListener` (frame-loop thread) → adaptador:

| Callback ML | Destino WebView |
|---|---|
| `onVrPointerMove(u,v)` | posición táctil (hover + base de tap/drag) |
| `onVrButton(0, down)` | `ACTION_DOWN/UP` en (u,v) (tap, drag) |
| `onVrButton(1/2, …)` | click derecho/medio (menú contextual, futuro) |
| `onVrScroll(clicks)` | scroll vertical |
| `onVrKey(code, mods)` | texto al campo enfocado (Etapa C) |
| `onVrModifiers(m)` | shift/Ctrl/Alt/Win para campos |
| `onVrExit()` | cerrar sesión |
| `onVrGamepad*` | ignorado (navegador sin gamepad) |

Sin pantalla: botones de la barra/panel los consume ML antes; al
adaptador solo llega puntero sobre contenido (verificar en B).

## 3. Actividad anfitriona (nueva)

Nueva `BrowserActivity` (nombre por definir) en la rama:

1. Crea `WebView` + `VirtualDisplay` (patrón probado DepthSurf).
2. `new XrRenderer()` + `start(activity, W, H, prefs)` + surface.
3. Implementa `InputListener` → WebView (hilo UI vía handler).
4. `prepareForStop()/cleanup()` al salir (mismo orden que
   `MediaCodecDecoderRenderer.stopXrRenderer()`).

No se toca `Game`/streaming: la actividad vive en paralelo y la
app arranca directo en ella (provisional; selector después).

La `Presentation` contiene barra de navegación + `WebView`:
botones atrás/adelante/recargar, campo URL (`EditText`,
`setShowSoftInputOnFocus(false)`, Enter = cargar) y botón Ir.
`BrowserNav.normalizeUrl()`: host pelado → https, texto libre →
búsqueda Google. Táctil se despacha al layout raíz (coordenadas
VD completas); teclas van a la vista con foco (URL o página).
`VirtualDisplay` a 224 dpi (70%): contenido ~30% más chico.
`WebView`: overview + wide viewport para ajuste de ancho.

## 6. Esquinas redondeadas (único cambio a `xr-renderer`)

- `xr_shaders.c` (`FRAGMENT_SRC`): `cornerAlpha()` — caja
  redondeada SDF sobre `v_plain`, radio `u_corner` = 0.055
  (fracción de altura), feather ~1px; multiplica `fragColor.a`
  en las 3 salidas (warp, showDepth, barTest).
- `xr_gl.c`: locations `u_corner`/`u_cornerAspect` + valor por
  frame; aspecto = `videoWidth/videoHeight`.
- `xr_layers.c`: capa de video (quad y cilindro) con
  `XR_COMPOSITION_LAYER_BLEND_TEXTURE_SOURCE_ALPHA_BIT`.
  Swapchain `GL_SRGB8_ALPHA8` conserva el alfa.
- Espejo puro `browser/CornerMask.java` + 6 tests JUnit fijan
  la matemática (centro/bordes opacos, esquinas
  transparentes, radio 0 = cuadrada).

## 4. Identidad y firma

- Etapa B: mismo `applicationId` ML debug (rama de trabajo).
- Al estabilizar: renombrar a DepthSurf (`com.quest3d.browser` no
  aplica aquí; decidir id final) + icono/nombre. Firma debug.

## 5. Riesgos y verificación local

- R1: superficie ocupada antes de `start()` → crear VD DESPUÉS de
  `getInputSurface()` no-nulo (igual que el codec).
- R2: UV fuera de `[0,1]` → clamp antes de `MotionEvent`.
- R3: hilo: callbacks en frame-loop → postear a UI.
- R4: teclado 3D abre panel (`kbOpen`) — probar foco en C.
- Local: build + JVM tests + asserts de clamp/mapeo en el
  adaptador (tests nuevos, misma suite JUnit).
- Quest: P1/P2/P3/P8/P10 del plan (sin streaming).
