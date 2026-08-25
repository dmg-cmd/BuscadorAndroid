# Reglas Globales del Proyecto

## Idioma Obligatorio
> **REGLA INQUEBRANTABLE:** Toda comunicación y todo razonamiento interno deben ser **EXCLUSIVAMENTE en español**.

- Responde siempre en español, sin importar en qué idioma escriba el usuario.
- Piensa y razona en español (chain-of-thought en español).
- Comentarios de código, mensajes de commit, documentación, nombres de variables explicativas y respuestas deben estar en español (excepto keywords técnicas inevitables del lenguaje/framework).
- Si necesitas citar documentación en inglés, tradúcela y explica en español.
- Esta regla aplica a todos los agentes, subagentes y comandos del proyecto.

## Regla de Versionado de la Aplicación
> **REGLA DE VERSIONADO AUTOMÁTICO:** En **cada modificación, corrección o agregado** a la aplicación, se debe actualizar la versión en `app/build.gradle.kts` (`versionName` y `versionCode`) y reflejarla en la barra superior.

- **Formato:** `MAJOR.MINOR.PATCH` (iniciando en `0.0.1`).
- **Incremento de PATCH (último número):** Cada cambio incrementa el último número en 1 (`0.0.1` -> `0.0.2` ... -> `0.0.9`).
- **Incremento de MINOR (segundo número):** Cuando el último número supera el 9 (llega a 10), el último número se reinicia a `0` y el segundo número se incrementa en 1 (`0.0.9` -> `0.1.0`).
- **Incremento de MAJOR (primer número):** El segundo número cuenta de 0 a 4 (5 ciclos de 10: `x.0.z` a `x.4.z`). Al pasar de `x.4.9`, en la siguiente versión se incrementa el primer número en 1 y los demás se reinician a 0 (`0.4.9` -> `1.0.0`, `1.4.9` -> `2.0.0`).

## Proyecto: BuscadorAndroid - Buscador de Archivos para Android
App nativa Android que busca archivos en todo el dispositivo (almacenamiento interno, SD, descargas, documentos, media).

Ver plan detallado en `PLAN.md`.
