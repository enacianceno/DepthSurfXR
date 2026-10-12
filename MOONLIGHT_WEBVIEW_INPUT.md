# Moonlight → WebView: adaptador de entrada (Etapas B/C)

`XrRenderer.InputListener` intacto; el adaptador vive en
`com.limelight.browser` (`BrowserActivity` + `WebViewInputMapper`).
Callbacks llegan en el hilo del frame loop → salto a UI por handler.

## B: puntero, botones, scroll (implementado)

- `onVrPointerMove(u,v)` [0,1]: guarda última posición; si botón
  abajo → `ACTION_MOVE` (drag). Sin botón: solo tracking (hover
  futuro, sin evento sintético para no prefocoquear enlaces).
- `onVrButton(0, down)`: flancos → `ACTION_DOWN/UP` en último (u,v)
  vía `WebView.dispatchTouchEvent` (`downTime` del DOWN, reciclado).
  Botones 1/2: ignorados en B (menú contextual en C si acaso).
- `onVrScroll(clicks)`: `scrollBy(0, clicks*160)`.
- `onVrMouseMove`: ignorado (modo mirada relativo; sin target táctil).
- `onVrGamepad*`: ignorado (navegador).
- `onVrExit()`: `finish()`.
- Mapeo UV→px: `clamp01` + escala al tamaño real del WebView
  (`WebViewInputMapper`, JUnit: clamp, escala, scroll).

## C: teclado (IMPLEMENTADO)

Reutiliza `VrKeyboard` de Moonlight con un `Sink` propio (`WebSink`
en `BrowserActivity`): `onVrKey` → `type(code, mods)` en UI,
`onVrModifiers` → `hold(mods)`, `releaseAll()` al salir.

- `onVrKey(code, mods)`: Unicode con shift aplicado, control codes
  para backspace/tab/enter/space, o VK sobre `KB_CODE_VK`.
  - Texto Unicode → `ACTION_MULTIPLE` al WebView con foco
    (`requestFocus` primero; mismo mecanismo probado DepthSurf v17).
  - Backspace(8)/Enter(13) → DOWN/UP `KEYCODE_DEL/ENTER`.
  - `KB_CODE_VK + x` → mapear códigos virtuales usuales
    (flechas, escape, tab) a `KeyEvent`.
- `onVrModifiers(m)`: shift/ctrl/alt/win para combinaciones
  (ctrl+A/C/V/X en campos; resto se ignora).
- Foco: el campo se enfoca con tap (B) antes de abrir el teclado 3D
  (`kbOpen`); el adaptador NO mueve el foco solo.

## Hilos y ciclo de vida

- Todo callback → `Handler(Looper.getMainLooper())`.
- Salida: `prepareForStop()` → liberar VD → `dismiss()` →
  `webView.destroy()` → `cleanup()` (orden del path codec).
- Sin segundo raycast, sin poses de cabeza, sin duplicar eventos:
  una sola fuente (Moonlight) y un solo destino (WebView).
