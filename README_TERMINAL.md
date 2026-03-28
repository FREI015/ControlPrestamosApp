# Control de Préstamos - Proyecto Android nativo

Este proyecto fue generado para abrirse en **Android Studio** o compilarse desde **terminal en Windows**.

## Decisiones explícitas del proyecto

1. **Persistencia local segura**  
   Se usa **Room** para la base de datos y **AES/GCM con Android Keystore** para cifrar:
   - teléfono
   - cédula / identificador
   - observaciones
   - notas de pagos
   - motivo de lista negra

2. **Búsqueda y bloqueos por lista negra**  
   Para poder validar sin guardar datos sensibles en texto plano, se almacenan hashes SHA-256 normalizados de:
   - teléfono
   - cédula / identificador

3. **Historial**  
   Para evitar duplicidad y simplificar mantenimiento, el historial se resuelve como la vista de préstamos con estado:
   - COBRADO
   - PERDIDO

4. **Dashboard**  
   El dashboard calcula los indicadores por período usando:
   - préstamos originados en el período
   - pagos registrados en el período
   - avance de cobro = recuperado del período / total a recuperar del período

5. **Wrapper de Gradle**  
   El archivo `gradlew.bat` incluido es un bootstrap ligero:  
   si no encuentra Gradle instalado, descarga **Gradle 8.7** automáticamente en la carpeta `.gradle-bootstrap`.

---

## Requisitos en Windows

- Android Studio instalado
- JDK 17 o superior (Android Studio normalmente ya lo trae)
- Android SDK instalado
- Tener acceso a internet la primera vez para descargar dependencias de Gradle
- En el teléfono:
  - activar Opciones de desarrollador
  - activar Depuración USB

---

## Qué descargar

Descarga y descomprime la carpeta del proyecto:

- `ControlPrestamosApp.zip`

---

## Cómo prepararlo por terminal

Abre **PowerShell** en la carpeta raíz del proyecto.

### 1. Copiar el archivo local.properties
```powershell
Copy-Item .\local.properties.example .\local.properties
```

### 2. Editar la ruta del SDK
Abre `local.properties` y deja algo así:

```properties
sdk.dir=C\:\\Users\\TU_USUARIO\\AppData\\Local\\Android\\Sdk
```

---

## Comandos de terminal

### Ver tareas disponibles
```powershell
.\gradlew.bat tasks
```

### Compilar APK debug
```powershell
.\gradlew.bat assembleDebug
```

### Instalar en el teléfono conectado por USB
```powershell
.\gradlew.bat installDebug
```

### Limpiar compilación
```powershell
.\gradlew.bat clean
```

---

## Ruta de la APK generada

```text
app\build\outputs\apk\debug\app-debug.apk
```

---

## Cómo abrirlo en Android Studio

### Desde terminal
```powershell
start "" "C:\Program Files\Android\Android Studio\bin\studio64.exe" .
```

O abre Android Studio y selecciona la carpeta `ControlPrestamosApp`.

---

## Estructura importante

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `gradlew`
- `gradlew.bat`
- `local.properties.example`

---

## Qué hace la app

- Registro de préstamos
- Cálculo automático de intereses y total a regresar
- Pagos parciales
- Saldo pendiente
- Días de atraso
- Marcado como perdido
- Lista negra
- Bloqueo de nuevos préstamos a clientes bloqueados
- Dashboard quincenal, mensual y trimestral
- Datos semilla locales para demostración

---

## Cómo ampliar luego sin romper lo actual

1. Agregar exportación a Excel/PDF desde una nueva capa `export`.
2. Agregar recordatorios locales con WorkManager.
3. Agregar respaldo cifrado local o manual.
4. Agregar edición de préstamos reutilizando el `LoanFormViewModel`.
5. Agregar autenticación local PIN/biometría encima del `MainActivity`.

---

## Nota importante

El proyecto está listo para abrirse y compilarse, pero la compilación final depende de que en tu PC exista:
- Android SDK correcto
- licencias aceptadas
- internet para dependencias de Gradle la primera vez
