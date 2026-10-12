# DepthSurf XR — GPU_TEXTURE_INTEGRATION

## Contrato que TODO motor debe cumplir [COMPROBADO en código/docs]
Ruta actual (GPU-GPU, cero copias CPU):
WebView → Presentation → VirtualDisplay → `XrRenderer.getInputSurface()`
(Surface sobre SurfaceTexture GL_TEXTURE_EXTERNAL_OES creado en el hilo XR)
→ onFrameAvailable/updateTexImage + matriz → warp/ZipDepth → swapchain
SRGB8_ALPHA8 doble ancho → OpenXR. Resize vía setDefaultBufferSize.
ZipDepth baja a 512x288 fp16 (PBO/glReadPixels en ruta de profundidad ya
existente). Requisito: producir frames continuos (incl. video) en esa
Surface externa, en el contexto/orden que el loop XR espera.

## Por motor
- **WebView**: CUMPLE hoy. Es el único camino demostrado en Quest.
- **GeckoView** [PENDIENTE — puerta del spike]: GeckoView renderiza en su
  propia superficie interna de la vista. Preguntas que el spike DEBE responder
  antes de hablar de sustitución: (1) ¿puede alojarse en la Presentation del
  VirtualDisplay igual que el WebView y heredar así la Surface OES?
  (2) si no, ¿qué API entrega frames (captura a Bitmap = GPU→CPU, latencia y
  coste inaceptables para video; Surface externa directa = viable)?
  (3) sincronización/fps sostenidos con video. Sin (1) o (2)-directa: NO viable.
- **Chromium/AwContents**: control de compositor teóricamente mayor, al coste
  descrito en CHROMIUM_VS_GECKOVIEW.md. No justificado antes del spike Gecko.

## Regla
No afirmar sustitución sin frames continuos ≥20 fps sin readback CPU
demostrados en el spike. ZipDepth entra DESPUÉS de esa puerta, nunca antes.
