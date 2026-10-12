# RTMP Fase 1 — RESULTADO (2026-10-12, Quest 3S)

StreamRecorder (experimental): vista del navegador -> H.264 854x480@15
(MediaCodec superficie) + microfono -> AAC 44.1k, mux MP4 local.
Trigger: dsurl=rectest (12 s, a los 4 s del arranque).

Verificado en Quest: "BrowserDiag REC done bytes=178843 frames~149".
MP4 extraido y parseado: 2 tracks (video vmhd + audio smhd),
audio 11.96 s. Codificadores y mux FUNCIONAN en el visor.
PENDIENTE: verificar visualmente el contenido del frame (sin ffmpeg
en PC; revisar el MP4 en el visor o con reproductor externo).

Fase 2 (RTMP a TikTok): mismo H.264/AAC, cambiar MediaMuxer por
socket RTMP (handshake + FLV tags) hacia el ingest de TikTok.
Bloqueos externos: la cuenta necesita acceso LIVE y la clave de
transmision la pone el usuario en la app (nunca en logs ni repo).
Sin clave/acceso no hay prueba de emision posible.
