# Moonlight XR — Baseline de la rama DepthSurf (Etapa A)

Rama: `C:\DepthSurfXR_MoonlightBase` (copia independiente, sin `.git`).
Fecha: 2026-10-11.

## Origen verificado

- Super-proyecto: `75d92f2` ("Merge branch 'v04-neo3-controllers'
  into v0.4.3-hotfix"), app `12.1-xr0.4.3`.
- Submódulo `moonlight-common-c`: pin `874ac95`, contenido copiado
  de la referencia (117 archivos, SHA coincide con el pin).
- Upstream original (`C:\MoonlightXR_EN`) NO modificado (solo
  lectura). Licencia: GPLv3, igual que DepthSurf.

## Cambios respecto al upstream (solo build local)

1. `local.properties`: `sdk.dir` del equipo (no versionado).
2. Submódulo poblado (vacío en la copia de trabajo).
3. `GRADLE_USER_HOME` local al workspace (descargas en
   `C:\Quest3Dbrowser\tools\gradle-home-ml`, sin tocar el sistema).

Cero cambios de código fuente en esta etapa.

## Build baseline

- Comando: `gradlew :app:assembleNonRootDebug` (Gradle 9.7.1,
  AGP 9.4.0, NDK 29.0.14206865, JDK 21, SDK android-37.0).
- Resultado: `BUILD SUCCESSFUL in 7m 13s`, 48 tareas.
- APK: `app/build/outputs/apk/nonRoot/debug/app-nonRoot-debug.apk`
  (102 MB), `com.gilleece.moonlightxr.debug`, vCode 31407,
  arm64-v8a + otras ABI.
- SHA-256:
  `2DDE4A7E22DB4B51C7E9053ED7C6EAE238F334D7A6673086027A5D0E42EFE7A1`
- Log: `build-baseline.log` (en la rama).

## Pruebas locales

- JVM: `:app:testNonRootDebugUnitTest` → **335/335** (0 fallos).
  Log: `build-baseline-test.log`.
- C host (16 suites `app/src/test/cpp`, Makefile): compiladas con
  NDK clang para ARM64 (0 errores, `-Wall -Werror`) y ejecutadas EN
  EL QUEST: **16/16 PASS** (atlas 79 checks, roommesh 87 checks,
  resto silencioso = pass). Nota: no hay toolchain C host en esta
  PC; se usó el mismo método que el mathtest DepthSurf.

## Límites declarados

- Sin validación funcional en Quest en esta etapa (no instalada;
  el usuario ya validó el upstream original en su visor).
- ADB Wi-Fi actual: `192.168.68.101:5555` (la IP cambia por DHCP).

## Siguiente (Etapa B)

Adaptador WebView: `XrRenderer.start(activity, w, h)` +
`getInputSurface()` → VirtualDisplay; `InputListener` → eventos
WebView. Detalle en `BROWSER_SOURCE_ADAPTER.md` (pendiente).
