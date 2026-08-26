# BuscadorAndroid

**BuscadorAndroid** es una aplicación gratuita para dispositivos Android que te ayuda a
**encontrar cualquier archivo guardado en tu teléfono** (memoria interna, tarjeta SD,
descargas, documentos y archivos multimedia) de forma rápida y sencilla.

Pensada para cualquier persona, aunque no sepa mucho de tecnología: escribe lo que buscas
y la app hace el resto.

---

## ¿Qué puede hacer?

- **Buscar por nombre o por contenido.** Escribe el nombre del archivo (por ejemplo
  *"fotos de playa"*) o una palabra que sabes que tiene dentro de un documento.
- **Filtrar por tipo.** Muestra solo Imágenes, Vídeos, Audio, Documentos u Otros.
- **Buscar dentro del texto.** Activa la opción *En contenido* para que la búsqueda también
  mire dentro de los documentos (PDF, textos, etc.), no solo el nombre.
- **Ordenar los resultados** por lo más parecido, nombre, fecha, tamaño, tipo o carpeta.
- **Agrupar por carpeta** para ubicar rápido dónde está cada archivo.
- **Seleccionar varios a la vez** para copiarlos, moverlos, compartirlos o eliminarlos.
- **Tema claro / oscuro / sistema**, a tu gusto.
- **Botón de ayuda (?).** Toca el signo de interrogación en la barra superior y se abre
  una ventana con explicaciones sencillas de cada botón.
- **Conexión MiNube (SMB).** Toca el icono de carpeta compartida para conectarte a tu
  **carpeta de red privada en la LAN** (un recurso SMB/CIFS como MiNube, un NAS o un
  equipo de la red local). Desde allí puedes **explorar, subir, descargar y abrir
  (previsualizar imágenes, vídeos y música)** archivos; también **copiar al teléfono**,
  **mover al teléfono** (descarga y borra de la nube) y **eliminar** archivos o carpetas
  de la nube. Toda la transferencia ocurre dentro de tu red y sin salir a internet.

---

## Privacidad

- **Conexión solo a tu red local.** La aplicación puede conectarse a la carpeta de red
  que **tú configures** (protocolo SMB/CIFS en tu LAN). No se comunica con ningún servidor
  en internet ni envía nada a la nube externa.
- **Tus credenciales quedan en el teléfono.** El usuario, dominio y contraseña de MiNube
  se guardan **cifrados** en el propio dispositivo (Android EncryptedSharedPreferences) y
  nunca se comparten con nadie.
- **La búsqueda es local.** Encontrar archivos en tu teléfono se hace enteramente en el
  dispositivo, sin red.

---

## Descarga e instalación

1. Entra en la página de versiones (releases) del proyecto:
   https://github.com/dmg-cmd/BuscadorAndroid/releases
2. Descarga el archivo **`app-release.apk`** de la última versión.
3. Ábrelo en tu teléfono. Android te avisará que el archivo es de "origen desconocido":
   - Toca **Configuración** y activa **Permitir de esta fuente** (orígenes desconocidos).
   - Vuelve atrás y pulsa **Instalar**.
4. Listo: busca el icono redondo de **BuscadorAndroid** en tu pantalla de inicio.

> **Nota:** El instalador está **firmado con una clave propia (autofirmado)**, por lo que
> Android puede mostrar una advertencia de seguridad. Es normal en versiones de prueba.
> Si lo prefieres, puedes pedir una versión firmada con una clave de tienda oficial.

---

## Permisos que pide la aplicación

La app necesita permisos para leer tus archivos. Aquí te explicamos para qué sirve cada uno:

| Permiso | ¿Para qué sirve? |
| --- | --- |
| **Leer archivos multimedia** (imágenes, vídeos, audio) | Para poder encontrar y mostrar tus fotos, vídeos y música. |
| **Leer almacenamiento** (en versiones antiguas de Android) | Lo mismo en teléfonos con Android 8 a 12. |
| **Acceso a todos los archivos** (opcional) | Permite buscar **cualquier** archivo del teléfono, no solo los multimedia. Es útil para una búsqueda completa, pero es opcional: si lo deniegas, la app sigue funcionando y busca tus archivos multimedia y documentos. |
| **Internet / estado de red** | Solo para conectarse a tu **carpeta de red MiNube (SMB)** dentro de tu LAN. La app no navega por internet ni sube nada a servidores externos. |

Ningún permiso se usa para otra cosa distinta a buscar y mostrar tus archivos, o a
conectarse (si tú lo decides) a tu propia carpeta de red local.

---

## Cómo usarla (paso a paso)

1. **Escribe** en el cuadro de búsqueda el nombre o la palabra que buscas.
2. Pulsa la **lupa** para empezar.
3. Si quieres afinar, toca:
   - **Tipos** para ver solo imágenes, vídeos, audio o documentos.
   - **En contenido** para buscar también dentro de los documentos.
   - **Orden** para elegir cómo se listan los resultados.
   - **Por carpeta** para agruparlos.
4. **Selecciona todo** (o uno a uno) para copiar, mover, compartir o borrar.
5. ¿Dudas? Toca el **signo de interrogación (?)** arriba, al lado del icono de tema,
   y lee las explicaciones sencillas.

---

## Conexión MiNube (SMB)

MiNube te permite dejar y recuperar archivos en una **carpeta de red privada** que tengas
en tu red de casa o de trabajo (por ejemplo, un servidor MiNube, un NAS o un equipo
compartiendo una carpeta por SMB/CIFS).

**Requisitos**
- Estar en la **misma red local (LAN)** que el equipo que comparte la carpeta.
- Conocer la **dirección IP** (o nombre) del equipo, el **puerto** (por defecto 445), el
  **nombre del recurso/compartido**, tu **usuario** y **contraseña** de acceso.
- Que el recurso use el protocolo **SMB/CIFS** (lo habitual en Windows, NAS y MiNube).

**Cómo conectarse**
1. Toca el **icono de carpeta compartida** (arriba, junto al signo de ayuda).
2. Rellena los datos de tu carpeta de red y pulsa **Conectar y guardar**.
3. Si la conexión es correcta, verás los archivos y carpetas. Desde ahí puedes:
   - **Subir aquí** los archivos que seleccionaste en la app.
   - **Crear carpeta**, entrar en subcarpetas y **descargar** a tu teléfono.
   - **Volver atrás:** toca la flecha hacia arriba (o la flecha de atrás) para subir un nivel
     a la carpeta superior; la equis (X) cierra el explorador.
   - **Buscar:** escribe en el buscador para encontrar archivos dentro de esa carpeta y sus
     subcarpetas. Toca un resultado para ir a su ubicacion o seleccionarlo y descargarlo.
   - **Ver y escuchar:** toca un archivo de imagen, vídeo o música para abrirlo con el visor
     del sistema (galería, reproductor de vídeo o de música). La app descarga una copia
     temporal en el teléfono para poder abrirlo; todo ocurre en tu red local.
4. Tus datos se guardan cifrados; la próxima vez se conectará solo.

> **Sugerencia:** si no tienes un servidor propio, puedes crear una carpeta compartida en
> un equipo Windows o montar MiNube en tu red local. La transferencia es tan rápida como
> lo permita tu Wi-Fi/LAN y no consume datos móviles.

---

## Requisitos

- Teléfono o tablet con **Android 8.0 (Oreo)** o superior.
- Espacio mínimo en disco (la app pesa muy poco).

---

## Estado del proyecto

Esta es una **versión de prueba** (autofirmada). Se actualiza constantemente con mejoras.
La versión más reciente y el historial de cambios están en:
https://github.com/dmg-cmd/BuscadorAndroid/releases

---

## Preguntas frecuentes

**¿Mi teléfono envía mis archivos a internet?**
No, salvo que tú lo decidas. La búsqueda es local. La función MiNube solo se conecta a la
carpeta de red que tú configures dentro de tu LAN (SMB/CIFS); nada se sube a servidores
externos ni a la nube pública, y tus credenciales se guardan cifradas en el teléfono.

**¿Por qué me sale un aviso al instalar?**
Porque el instalador está firmado con una clave propia. Es normal en pruebas.

**¿No encuentra un archivo?**
Asegúrate de haber concedido el permiso de **Acceso a todos los archivos** si quieres
buscar archivos que no sean multimedia o documentos. También prueba a activar
**En contenido** para buscar por el texto interior de los documentos.

---

¡Esperamos que BuscadorAndroid te ahorre tiempo encontrando lo que buscas!
