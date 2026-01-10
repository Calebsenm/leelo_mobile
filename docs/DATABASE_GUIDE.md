# 🗄️ Guía de Base de Datos - Leelo

## 📋 **Resumen de la Base de Datos**

Leelo utiliza **Room Database** como abstracción sobre **SQLite** para garantizar persistencia robusta de textos.

### **🏗️ Arquitectura de Datos**
```
UI Layer (Fragments)
    ↓ Repository Pattern
        ↓ Room Database
            ↓ SQLite Engine
                💾 Archivo .db persistente
```

---

## 📊 **Esquema de la Base de Datos**

### **Tabla: `texts`**
```sql
CREATE TABLE texts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,    -- ID único auto-generado
    title TEXT NOT NULL,                    -- Título del texto
    content TEXT NOT NULL,                  -- Contenido completo
    creation_date INTEGER NOT NULL,         -- Timestamp creación
    modification_date INTEGER NOT NULL      -- Timestamp última modificación
);

-- Índices para rendimiento
CREATE INDEX idx_texts_title ON texts(title);
CREATE INDEX idx_texts_creation_date ON texts(creation_date);
CREATE INDEX idx_texts_modification_date ON texts(modification_date);
```

### **🔄 Flujo de Conversión de Datos**

```
Text.java (Model UI)          TextEntity.java (Entity BD)
├── idText: Long           ↔     ├── id: Long
├── tittle: String          ↔     ├── title: String  
├── text: String           ↔     ├── content: String
└── creationWordDate: Date  ↔     ├── creationDate: Long
                               └── modificationDate: Long
```

---

## 🏷️ **Entities Room**

### **TextEntity.java**
```java
@Entity(tableName = "texts")
public class TextEntity {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public Long id;

    @ColumnInfo(name = "title")
    public String title;

    @ColumnInfo(name = "content") 
    public String content;

    @ColumnInfo(name = "creation_date")
    public Long creationDate;

    @ColumnInfo(name = "modification_date")
    public Long modificationDate;
}
```

**📝 Notas Importantes:**
- `@Entity`: Marca esta clase como tabla Room
- `@PrimaryKey`: Campo identificador único
- `@ColumnInfo`: Nombres explícitos de columnas
- `Long` timestamps: Facilita conversión de fechas

---

## 🔍 **Data Access Objects (DAOs)**

### **TextDao.java - Operaciones CRUD**

```java
@Dao
public interface TextDao {
    
    // 🆕 INSERTAR
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(TextEntity text);
    
    // ✏️ ACTUALIZAR  
    @Update
    int update(TextEntity text);
    
    // 🗑️ ELIMINAR
    @Delete
    int delete(TextEntity text);
    
    @Query("DELETE FROM texts WHERE id = :id")
    int deleteById(long id);
    
    // 📖 LEER
    @Query("SELECT * FROM texts ORDER BY creation_date DESC")
    List<TextEntity> getAll();
    
    @Query("SELECT * FROM texts WHERE id = :id")
    TextEntity getById(long id);
    
    // 🔍 BÚSQUEDA
    @Query("SELECT * FROM texts WHERE title LIKE '%' || :query || '%' ORDER BY creation_date DESC")
    List<TextEntity> searchByTitle(String query);
    
    // 📊 CONSULTAS ÚTILES
    @Query("SELECT COUNT(*) FROM texts")
    int getCount();
    
    @Query("SELECT COUNT(*) FROM texts WHERE title = :title")
    int existsByTitle(String title);
}
```

**🎯 Patrones de Consultas Room:**
- `@Insert/@Update/@Delete`: Operaciones de modificación
- `@Query`: Consultas SQL personalizadas
- `:parameter`: Parámetros nombrados (previene SQL injection)
- `LIKE '%' || :query || '%'`: Búsqueda parcial en strings

---

## 🗄️ **AppDatabase.java - Configuración Room**

```java
@Database(
    entities = {TextEntity.class},  // Tablas incluidas
    version = 1,                   // Versión del esquema
    exportSchema = false           // No generar JSON schema
)
public abstract class AppDatabase extends RoomDatabase {
    
    // Room genera implementación automáticamente
    public abstract TextDao textDao();
    
    // Singleton pattern
    private static AppDatabase INSTANCE;
    
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                INSTANCE = Room.databaseBuilder(
                    context.getApplicationContext(),
                    AppDatabase.class,
                    "leelo_database"  // Nombre del archivo .db
                )
                .fallbackToDestructiveMigration()  // Para desarrollo
                .build();
            }
        }
        return INSTANCE;
    }
}
```

**⚙️ Configuración Importante:**
- **Singleton**: Una sola instancia por aplicación
- **ApplicationContext**: Evita memory leaks
- **fallbackToDestructiveMigration**: Para desarrollo (pierde datos)
- **Nombre BD**: `leelo_database` (archivo físico)

---

## 🧠 **Repository Pattern - TextRepository.java**

### **Responsabilidades del Repository**

1. **Conversión de Datos**: Entity ↔ Model
2. **Operaciones Asíncronas**: ExecutorService + Callbacks
3. **Lógica de Negocio**: Validaciones, transformaciones
4. **Error Handling**: Manejo centralizado de errores

### **Implementación Clave**

```java
public class TextRepository {
    private final TextDao textDao;
    private final ExecutorService executor;
    
    // 🔄 CONVERSIÓN Model → Entity
    private TextEntity modelToEntity(Text model) {
        TextEntity entity = new TextEntity();
        entity.id = model.getIdText();
        entity.title = model.getTittle();      // ⚠️ "tittle" vs "title"
        entity.content = model.getText();
        entity.creationDate = convertDate(model.getCreationDate());
        entity.modificationDate = System.currentTimeMillis();
        return entity;
    }
    
    // 🔄 CONVERSIÓN Entity → Model  
    private Text entityToModel(TextEntity entity) {
        Text model = new Text();
        model.setIdText(entity.id);
        model.setTittle(entity.title);
        model.setText(entity.content);
        model.setCreationDate(entity.getCreationDateAsLocalDate());
        return model;
    }
    
    // ⚡ OPERACIÓN ASÍNCRONA
    public void insertText(Text text, OnInsertCallback callback) {
        executor.execute(() -> {
            try {
                TextEntity entity = modelToEntity(text);
                long id = textDao.insert(entity);
                
                // Callback en main thread
                mainHandler.post(() -> callback.onInsertComplete(id != -1, id));
            } catch (Exception e) {
                Log.e(TAG, "Error insertando texto", e);
                mainHandler.post(() -> callback.onInsertComplete(false, -1));
            }
        });
    }
}
```

---

## 💾 **Persistencia y Storage**

### **Ubicación del Archivo de Base de Datos**
```
/data/data/com.app.leelo/databases/leelo_database
├── leelo_database      # Base de datos principal
├── leelo_database-shm  # Shared memory (operación)
└── leelo_database-wal  # Write-Ahead Logging (transacciones)
```

### **Características de Persistencia**
- ✅ **Permanente**: Sobrevive a cierres de app
- ✅ **Segura**: Transacciones ACID
- ✅ **Eficiente**: Índices y caching automático
- ✅ **Escalable**: Soporta textos muy largos (hasta 2GB)

### **Rendimiento para Textos Largos**
```java
// 📊 Estadísticas de tamaño
Long textSize = 500_000L;  // 500KB
Integer pageCount = calculatePages(textSize); // ~25 páginas

// 🚀 Optimizaciones automáticas
- Lazy loading de páginas
- Caching en memoria
- Paginación progresiva
- Compresión interna de SQLite
```

---

## 🔄 **Migraciones y Cambios en el Esquema**

### **Versionamiento de Base de Datos**

```java
@Database(entities = {TextEntity.class}, version = 2)  // Incrementar versión
public abstract class AppDatabase extends RoomDatabase {
    
    // Estrategia de migración
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        db.enableWriteAheadLogging();  // Mejor rendimiento
    }
}
```

### **Ejemplo: Agregar Nueva Columna**

```java
// Paso 1: Modificar TextEntity
@Entity(tableName = "texts")
public class TextEntity {
    // ... campos existentes ...
    
    @ColumnInfo(name = "category")
    public String category;  // Nueva columna
}

// Paso 2: Incrementar versión
@Database(entities = {TextEntity.class}, version = 2)

// Paso 3: Migración (en producción)
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2)
    .build();

static final Migration MIGRATION_1_2 = new Migration(1, 2) {
    @Override
    public void migrate(SupportiteDatabase db) {
        db.execSQL("ALTER TABLE texts ADD COLUMN category TEXT");
    }
};
```

### **Para Desarrollo (Destructivo)**
```java
Room.databaseBuilder(...)
    .fallbackToDestructiveMigration()  // Borra y recrea
    .build();
```

---

## 🧪 **Testing y Debugging**

### **Database Inspector (Android Studio)**
1. **Ejecutar la app**
2. **View → Tool Windows → App Inspection**
3. **Database Inspector** → Seleccionar `leelo_database`
4. **Explorar tablas, datos, ejecutar queries**

### **Queries de Debugging**
```sql
-- Ver todos los textos
SELECT * FROM texts ORDER BY creation_date DESC;

-- Buscar por título  
SELECT * FROM texts WHERE title LIKE '%mango%';

-- Estadísticas
SELECT 
    COUNT(*) as total_texts,
    AVG(LENGTH(content)) as avg_length,
    MAX(LENGTH(content)) as max_length
FROM texts;

-- Textos recientes (últimos 10)
SELECT * FROM texts 
ORDER BY creation_date DESC 
LIMIT 10;
```

### **Logging en Repository**
```java
private static final String TAG = "TextRepository";

public void insertText(Text text, OnInsertCallback callback) {
    Log.d(TAG, "Insertando texto: " + text.getTittle());
    
    executor.execute(() -> {
        long id = textDao.insert(modelToEntity(text));
        Log.d(TAG, "Resultado inserción: " + id);
        
        mainHandler.post(() -> {
            callback.onInsertComplete(id != -1, id);
        });
    });
}
```

---

## 🚨 **Problemas Comunes de Base de Datos**

### **1️⃣ "Cannot access database on the main thread"**
```java
// ❌ PROBLEMA
TextEntity entity = textDao.getById(id);  // UI thread!

// ✅ SOLUCIÓN
textRepository.getByIdAsync(id, callback);  // Background thread
```

### **2️⃣ "No such table: texts"**
```java
// Causa: Base de datos no inicializada correctamente
// Solución: Verificar AppDatabase.getInstance(context)

// Debug
AppDatabase db = AppDatabase.getInstance(context);
Log.d("DB", "Database path: " + db.getDatabasePath(context));
Log.d("DB", "Database exists: " + db.databaseExists(context));
```

### **3️⃣ "Migration didn't properly handle"**
```java
// Causa: Cambio en esquema sin migración
// Solución: Incrementar versión + migración O destructive migration

@Database(entities = {TextEntity.class}, version = 2)
public abstract class AppDatabase extends RoomDatabase {
    // ...
}

Room.databaseBuilder(...)
    .fallbackToDestructiveMigration()  // Para desarrollo
    .build();
```

### **4️⃣ "Foreign key constraint failed"**
```java
// Leelo no usa foreign keys actualmente
// Si se agregan en futuro, eliminar dependencias primero
textDao.deleteById(childId);  // Primero
textDao.deleteById(parentId);  // Después
```

---

## 📈 **Optimización de Rendimiento**

### **Índices Automáticos**
```java
// Room crea índices automáticamente para primary keys
// Agregar índices personalizados:

@Entity(
    tableName = "texts",
    indices = {
        @Index(value = {"title"}, name = "idx_texts_title"),
        @Index(value = {"creation_date"}, name = "idx_texts_creation_date")
    }
)
```

### **Batch Operations**
```java
// Insertar múltiples textos eficientemente
@Insert(onConflict = OnConflictStrategy.REPLACE)
void insertAll(List<TextEntity> texts);

// En Repository:
public void insertMultiple(List<Text> texts, OnBatchCallback callback) {
    executor.execute(() -> {
        List<TextEntity> entities = texts.stream()
            .map(this::modelToEntity)
            .collect(Collectors.toList());
        
        textDao.insertAll(entities);
        callback.onBatchComplete(true);
    });
}
```

### **Memory Management**
```java
// Para textos muy largos (>1MB)
public void insertLargeText(Text text, OnProgressCallback callback) {
    executor.execute(() -> {
        try {
            // Dividir en chunks
            List<String> chunks = splitIntoChunks(text.getText(), 100_000);
            
            for (int i = 0; i < chunks.size(); i++) {
                // Insertar chunk
                TextEntity chunk = createChunk(text, chunks.get(i), i);
                textDao.insert(chunk);
                
                // Reportar progreso
                int progress = (i * 100) / chunks.size();
                mainHandler.post(() -> callback.onProgress(progress));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inserting large text", e);
        }
    });
}
```

---

## 🔮 **Roadmap de Base de Datos**

### **Próximas Mejoras**
- [ ] **Full-text search**: Para búsqueda por contenido
- [ ] **Text compression**: Para textos muy largos
- [ ] **Backup/Restore**: Exportar a JSON/XML
- [ ] **Encryption**: Para textos sensibles
- [ ] **Sync metadata**: Para sincronización con nube

### **Schema Extensions**
```sql
-- Futuras tablas planeadas
CREATE TABLE categories (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL,
    color TEXT
);

CREATE TABLE reading_sessions (
    id INTEGER PRIMARY KEY,
    text_id INTEGER REFERENCES texts(id),
    start_time INTEGER,
    end_time INTEGER,
    pages_read INTEGER
);

CREATE TABLE bookmarks (
    id INTEGER PRIMARY KEY,
    text_id INTEGER REFERENCES texts(id),
    page_number INTEGER,
    note TEXT,
    created_at INTEGER
);
```

---

**Esta guía es tu referencia completa para trabajar con la base de datos de Leelo.** 🗄️

Para problemas específicos, consulta [TROUBLESHOOTING.md](TROUBLESHOOTING.md).