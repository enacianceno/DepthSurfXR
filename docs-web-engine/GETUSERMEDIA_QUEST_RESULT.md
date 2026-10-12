# getUserMedia en Quest 3S — RESULTADO (2026-10-12, en vivo)

Prueba automatica interna (dsurl=gumtest) en la APK experimental:
- enumerateDevices: audioinput, videoinput, audiooutput (existen).
- Permisos WebView: VIDEO_CAPTURE granted=true, AUDIO_CAPTURE granted=true.
- getUserMedia({video,audio}) -> FAIL NotReadableError: Could not start video source
- getUserMedia({audio}) -> FAIL NotReadableError: Could not start audio source

Conclusion COMPROBADA: el WebView de Horizon enumera dispositivos y
concede permisos, pero NO puede iniciar ninguna fuente de captura.
Transmitir por la web (TikTok LIVE Studio web / WebRTC) NO es viable
en este motor. La via para transmitir es NATIVA: capturar el frame de
la app (o Passthrough Camera API de Meta), codificar MediaCodec y
enviar RTMP al ingest de TikTok con clave del usuario (nunca en logs).
Requiere ademas acceso LIVE en la cuenta de TikTok.
