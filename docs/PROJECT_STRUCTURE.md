# 📂 Estructura del Proyecto Leelo

## 🗂️ **Arquitectura General**

El proyecto sigue una arquitectura limpia con separación clara de responsabilidades:

```
Leelo/
├── 📁 app/                    # Módulo principal de la app
│   ├── 📁 src/
│   │   ├── 📁 main/
│   │   │   ├── 📁 java/com/app/leelo/
│   │   │   │   ├── 📁 data/           # 🗄️ Capa de datos
│   │   │   │   │   ├── 📁 entity/     # Entidades Room
│   │   │   │   │   ├── 📁 dao/        # Data Access Objects
│   │   │   │   │   ├── 📁 database/   # Configuración Room
│   │   │   │   │   └── 📁 repository/ # Repositorios
│   │   │   │   ├── 📁 model/          # 📝 Modelos de UI
│   │   │   │   ├── 📁 ui/             # 🎨 Interfaz de usuario
│   │   │   │   │   ├── 📄 MainActivity.java
│   │   │   │   │   ├── 📄 TextFragment.java
│   │   │   │   │   ├── 📄 AddTextFragment.java
│   │   │   │   │   ├── 📄 ReadingActivity.java
│   │   │   │   │   └── 📄 ... (otros fragments)
│   │   │   │   ├── 📁 database/       # 🗄️ Base de datos (legacy)
│   │   │   │   └── 📁 utils/          # 🔧 Utilidades
│   │   │   └── 📁 res/              # 🎨 Recursos Android
│   │   │       ├── 📁 layout/        # XML layouts
│   │   │       ├── 📁 values/        # Strings, colors, etc.
│   │   │       └── 📁 drawable/      # Imágenes e íconos
│   │   └── 📁 test/                # 🧪 Tests unitarios
│   ├── 📄 build.gradle.kts           # ⚙️ Configuración Gradle
│   └── 📄 proguard-rules.pro        # 🔐 Reglas de ofuscación
├── 📁 docs/                        # 📚 Documentación
├── 📄 build.gradle.kts              # ⚙️ Configuración raíz
├── 📄 gradlew                       # 🚀 Wrapper Gradle
└── 📄 settings.gradle.kts           # 📋 Configuración de módulos
```

---

## 🏗️ **Explicación Detallada de Carpetas**

### 📁 **`data/` - Capa de Datos**

Responsable del almacenamiento y persistencia de datos usando **Room Database**.

```
data/
├── 📁 entity/
│   └── 📄 TextEntity.java          # 🏷️ @Entity - Tabla "texts"
├── 📁 dao/
│   └── 📄 TextDao.java            # 🔍 @Dao - Operaciones CRUD
├── 📁 database/
│   └── 📄 AppDatabase.java        # 🗄️ @Database - BD principal
└── 📁 repository/
    └── 📄 TextRepository.java     # 🧠 Repository - Lógica de negocio
```

**Responsabilidades:**
- ✅ **Entity**: Define estructura de tablas SQLite
- ✅ **DAO**: Interfaz para operaciones SQL (@Insert, @Query, @Update, @Delete)
- ✅ **Database**: Configuración Room y conexión a SQLite
- ✅ **Repository**: Convierte Entity ↔ Model y maneja async operations

---

### 📁 **`model/` - Modelos de UI**

Clases POJO que representan datos en la interfaz de usuario.

```
model/
└── 📄 Text.java                   # 📝 Modelo de texto para UI
    ├── idText: Long                # ID único
    ├── tittle: String             # Título (con error de ortografía 😅)
    ├── text: String               # Contenido del texto
    └── creationWordDate: LocalDate # Fecha de creación
```

**Características:**
- ✅ **POJOs puros**: Sin dependencias de Android
- ✅ **Compatibles con Serializable**: Para pasar entre Activities
- ✅ **Getters/Setters**: Acceso controlado a datos
- ✅ **Independientes**: No saben nada de la base de datos

---

### 📁 **`ui/` - Interfaz de Usuario**

Componentes visuales de la aplicación.

```
ui/
├── 📄 MainActivity.java           # 🏠 Pantalla principal con bottom nav
├── 📄 TextFragment.java          # 📋 Lista de textos
├── 📄 AddTextFragment.java        # ➕ Agregar/editar textos
├── 📄 ReadingActivity.java        # 📖 Lectura paginada
├── 📄 PracticeFragment.java      # 🎯 Modo de práctica
├── 📄 SettingFragment.java       # ⚙️ Configuración
├── 📄 ImportUrlTextFragment.java # 🌐 Importar desde URL (vacío)
└── 📄 ImportPdfTextFragment.java # 📄 Importar desde PDF (vacío)
```

**Flujo de UI:**
```
MainActivity (Bottom Nav)
├── TextFragment (Lista)
│   ├── ➕ AddTextFragment → Guardar → Volver a lista
│   └── 📖 ReadingActivity → Navegación paginada
├── PracticeFragment (Ejercicios)
└── SettingFragment (Configuración)
```

---

### 📁 **`res/` - Recursos Android`

```
res/
├── 📁 layout/                    # 📱 Diseños XML
│   ├── 📄 activity_main.xml      # MainActivity con bottom nav
│   ├── 📄 fragment_text.xml      # Lista de textos
│   ├── 📄 fragment_add_text.xml  # Formulario agregar texto
│   ├── 📄 activity_reading.xml   # Lectura paginada
│   └── 📄 item_page.xml          # Plantilla de página individual
├── 📁 values/                    # 🎨 Valores
│   ├── 📄 colors.xml            # Colores de la app
│   ├── 📄 strings.xml           # Textos constantes
│   ├── 📄 themes.xml            # Temas Material Design
│   └── 📄 dimens.xml            # Dimensiones estándar
├── 📁 drawable/                  # 🖼️ Recursos gráficos
│   ├── 📄 ic_launcher.xml       # Ícono de la app
│   ├── 📄 baseline_book_24.xml  # Ícono de libro
│   └── 📄 more_vert.xml          # Ícono de más opciones
└── 📁 menu/                      # 📋 Menús de navegación
    └── 📄 bottom_navigation.xml  # Menú inferior
```

---

## 🔄 **Flujo de Datos**

### **1. Guardar un Nuevo Texto**
```
AddTextFragment (UI)
    ↓ Text.java (Model)
        ↓ TextRepository.convert()
            ↓ TextEntity.java (Entity)
                ↓ TextDao.insert()
                    ↓ Room Database
                        ↓ SQLite
                            💾 Guardado permanente
```

### **2. Leer Todos los Textos**
```
TextFragment (UI)
    ↓ TextRepository.getAllTexts()
        ↓ TextDao.getAll()
            ↓ Room Database (SELECT * FROM texts)
                ↓ SQLite
                    💾 Recuperar datos
                        ↓ TextEntity[]
                            ↓ TextRepository.convert()
                                ↓ Text[]
                                    🎨 Renderizado en UI
```

### **3. Actualizar un Texto**
```
AddTextFragment (UI, modo edición)
    ↓ Text.java (Model con ID)
        ↓ TextRepository.updateText()
            ↓ TextDao.update()
                ↓ Room Database (UPDATE texts SET ...)
                    ↓ SQLite
                        💾 Actualización
                            🔄 Refresh UI
```

---

## 🧩 **Componentes Clave**

### **🗄️ Base de Datos Room**

```java
@Entity(tableName = "texts")
class TextEntity {
    @PrimaryKey Long id;
    String title;        // Título
    String content;      // Contenido
    Long creationDate;   // Timestamp
}

@Dao
interface TextDao {
    @Insert long insert(TextEntity text);
    @Query("SELECT * FROM texts") List<TextEntity> getAll();
    @Update int update(TextEntity text);
    @Delete int delete(TextEntity text);
}

@Database(entities = {TextEntity.class}, version = 1)
abstract class AppDatabase extends RoomDatabase {
    abstract TextDao textDao();
}
```

### **🧠 Repository Pattern**

```java
class TextRepository {
    // Convierte Model ↔ Entity
    private Text modelToEntity(Text model) { ... }
    private Text entityToModel(TextEntity entity) { ... }
    
    // Operaciones asíncronas con callbacks
    public void insertText(Text text, OnInsertCallback callback) { ... }
    public void getAllTexts(OnGetAllCallback callback) { ... }
}
```

### **📖 Sistema de Paginación**

```java
// ReadingActivity.java divide textos automáticamente:
StaticLayout layout = StaticLayout.Builder
    .obtain(fullText, 0, fullText.length(), paint, pageWidth)
    .setLineSpacing(0, 1.2f)
    .build();

// Calcula cuántas líneas caben por página
int pageHeight = screen_height - padding;
```

---

## 🔧 **Configuraciones Importantes**

### **build.gradle.kts (App Level)**
```kotlin
dependencies {
    // Core Android
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    
    // Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
}
```

### **AndroidManifest.xml**
```xml
<application android:name=".LeeloApplication">
    <activity android:name=".MainActivity" android:exported="true">
    <activity android:name=".ReadingActivity" android:exported="false">
</application>
```

---

## 🎯 **Puntos Clave para Entender el Proyecto**

### **✅ Ventajas de esta Arquitectura**
1. **Separación Clara**: UI no sabe de base de datos
2. **Testable**: Cada capa se puede probar por separado
3. **Escalable**: Fácil agregar nuevas funcionalidades
4. **Mantenible**: Código organizado y predecible

### **🔄 Ciclo de Vida de Datos**
```
Crear/Editar → Repository → Room → SQLite → Guardar
Leer → Room → SQLite → Recuperar → Repository → UI
Actualizar → Repository → Room → SQLite → Modificar
Eliminar → Repository → Room → SQLite → Borrar
```

### **🚨 Casos Especiales**
- **Textos Largos**: Paginación automática sin límite de tamaño
- **Error Handling**: Callbacks con manejo de errores
- **Memory Management**: ExecutorService para operaciones en background
- **UI Threading**: RunOnUiThread para actualizar interfaz

---

## 📋 **Checklist para Nuevos Desarrolladores**

### **Para entender el código:**
- [ ] **Leer `TextRepository.java`** primero (cerebro de la app)
- [ ] **Ver `TextFragment.java`** (flujo UI principal)
- [ ] **Entender `ReadingActivity.java`** (paginación inteligente)
- [ ] **Revisar `TextEntity.java`** (estructura de datos)

### **Para hacer cambios:**
- [ ] **Seguir Repository Pattern**: Nunca acceder directamente a Room
- [ ] **Usar callbacks**: Todas las operaciones DB son asíncronas
- [ ] **Mantener compatibilidad**: No romper el modelo `Text.java`
- [ ] **Documentar cambios**: Actualizar esta documentación

### **Para agregar funcionalidades:**
- [ ] **Nueva tabla**: Agregar Entity + DAO + métodos en Repository
- [ ] **Nueva pantalla**: Crear Fragment + navigation
- [ ] **Nueva característica**: Modificar Repository + UI + Tests

---

**Este documento es tu guía maestra para entender y trabajar con el proyecto Leelo.** 🎯