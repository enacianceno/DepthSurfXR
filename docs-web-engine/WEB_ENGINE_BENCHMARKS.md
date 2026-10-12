# DepthSurf XR — WEB_ENGINE_BENCHMARKS

Separación estricta: medido / reportado / pendiente. Sin números inventados.

## Matriz de sitios (estado al 2026-10-12)
- Google: [REPORTADO-OK históricamente] búsqueda + entrada de texto; re-validar en la sesión del experimento.
- Wikipedia: [PENDIENTE] navegación/enlaces/scroll en la candidata actual.
- YouTube: [REPORTADO por el usuario] reproduce en la base Moonlight aprobada.
- TikTok: [REPORTADO falla] error de página; causa sin logcat (ver TIKTOK_ERROR_DIAGNOSIS.md).
- Página HTML5 sin DRM: [PENDIENTE] usar como banco de frames/fps del spike.

## Recursos (solo hechos verificables)
- WebView del sistema: ~0 MB extra en APK; RAM del proceso del proveedor no medida [PENDIENTE: dumpsys meminfo].
- GeckoView: ~30 MB de artefacto [maven]; RAM/fps en Quest [PENDIENTE].
- Chromium embebido: +80–180 MB instalados, PSS/tab 300–600 MB [HIPÓTESIS, sin benchmark publicado en XR2 Gen 2].

## Protocolo de medición (cuando haya Quest)
Por sitio y motor: cargada / interactiva / video / audio / textura-para-3D /
profundidad — seis casillas separadas, nunca una sola marca de "funciona".
FPS y latencia solo con conteo en dispositivo; prohibido extrapolar de PC.
La sonda PC de hoy (tiktok.com HEAD=200) NO es un benchmark del visor.
