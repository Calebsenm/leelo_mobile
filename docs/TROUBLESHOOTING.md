# 🐛 Guía de Troubleshooting - Leelo

## 🚨 **Problemas Comunes y Soluciones**

Esta guía contiene los problemas más frecuentes que encontrarás al desarrollar en Leelo y sus soluciones inmediatas.

---

## 🔥 **PROBLEMAS CRÍTICOS (Crash Inmediato)**

### **1️⃣ "Cannot access database on the main thread"**

#### **📄 Stack Trace Típico**
```
java.lang.IllegalStateException: Cannot access database on the main thread 
since it may potentially lock the UI for a long period of time.
at androidx.room.RoomDatabase.assertNotMainThread(RoomDatabase.java:274)
```

#### **🔍 Causa Principal**
Llamada síncrona a Room desde el thread principal de UI.

#### **❌ Código Problemático**
```java
// EN TextFragment.java o AddTextFragment.java
TextEntity entity = textDao.getById(id);  // ❌ PROHIBIDO
List<TextEntity> all = textDao.getAll();  // ❌ PROHIBIDO
```

#### **✅ Solución Correcta**
```java
// Siempre usar callbacks del Repository
textRepository.getByIdAsync(id, new TextRepository.OnGetByIdCallback() {
    @Override
    public void onGetByIdComplete(Text text) {
        // Este código se ejecuta en background
        // Actualizar UI con runOnUiThread
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                // Actualizar UI aquí
            });
        }
    }
});
```

---

### **2️⃣ "getActivity() returns null"**

#### **📄 Stack Trace Típico**
```
java.lang.NullPointerException: Attempt to invoke virtual method 'void android.app.Activity.runOnUiThread(java.lang.Runnable)' on a null object reference
```

#### **🔍 Causa Principal**
Fragment está detachado de la Activity cuando se ejecuta el callback.

#### **❌ Código Problemático**
```java
// En cualquier callback asíncrono
getActivity().runOnUiThread(() -> {  // ❌ getActivity() puede ser null
    Toast.makeText(getContext(), "Listo", Toast.LENGTH_SHORT).show();
});
```

#### **✅ Solución Correcta**
```java
// Siempre validar que getActivity() no sea null
MainActivity activity = (MainActivity) getActivity();
if (activity != null) {
    activity.runOnUiThread(() -> {
        Toast.makeText(getContext(), "Listo", Toast.LENGTH_SHORT).show();
    });
}

// Alternativa más segura
if (getActivity() != null) {
    getActivity().runOnUiThread(() -> {
        Toast.makeText(getContext(), "Listo", Toast.LENGTH_SHORT).show();
    });
}
```

---

### **3️⃣ "No such table: texts"**

#### **📄 Stack Trace Típico**
```
android.database.sqlite.SQLiteException: no such table: texts (code 1 SQLITE_ERROR): 
, while compiling: SELECT * FROM texts ORDER BY creation_date DESC
```

#### **🔍 Causa Principal**
Base de datos no se inicializó correctamente o no creó las tablas.

#### **🧪 Debug Steps**
```java
// En TextRepository.java
private TextRepository(Context context) {
    AppDatabase database = AppDatabase.getInstance(context);
    this.textDao = database.textDao();
    
    // Debug: Verificar estado de la BD
    Log.d("DB_DEBUG", "Database path: " + database.getDatabasePath(context));
    Log.d("DB_DEBUG", "Database exists: " + database.databaseExists(context));
    Log.d("DB_DEBUG", "Database size: " + database.getDatabaseSize() + " bytes");
}
```

#### **✅ Solución Correcta**
```java
// Asegurar que AppDatabase esté inicializado correctamente
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase INSTANCE;
    
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),  // Importante: applicationContext
                        AppDatabase.class,
                        "leelo_database"
                    )
                    .fallbackToDestructiveMigration()  // Para desarrollo
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
```

---

## ⚠️ **PROBLEMAS LÓGICOS (Funcionamiento Incorrecto)**

### **4️⃣ "Los textos no se actualizan en la lista"**

#### **🔍 Síntomas**
- Nuevo texto se guarda pero no aparece en la lista
- Texto editado pero cambios no se reflejan
- Texto eliminado pero sigue apareciendo

#### **❌ Código Problemático**
```java
// En TextFragment.java después de guardar
texts.add(newText);  // ❌ Solo modifica lista local
renderTexts();       // ❌ No recarga desde BD

// Después de eliminar
texts.remove(textToDelete);  // ❌ Solo modifica lista local
renderTexts();
```

#### **✅ Solución Correcta**
```java
// Siempre recargar desde base de datos
private void refreshTexts() {
    textRepository.getAllTexts(new TextRepository.OnGetAllCallback() {
        @Override
        public void onGetAllComplete(List<Text> loadedTexts) {
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    texts.clear();
                    texts.addAll(loadedTexts);  // ✅ Datos frescos de BD
                    renderTexts();
                });
            }
        }
    });
}

// Después de cualquier operación CRUD
refreshTexts();  // ✅ Recargar siempre
```

---

### **5️⃣ "Bundle estático pierde datos entre pantallas"**

#### **🔍 Síntomas**
- Datos guardados con `MainActivity.textUpdateData` se pierden
- Funciona a veces pero falla consistentemente

#### **❌ Código Problemático**
```java
// En AddTextFragment.java
MainActivity.textUpdateData.putString("new_title", title);
MainActivity.textUpdateData.putString("new_content", content);

// En TextFragment.java
String title = MainActivity.textUpdateData.getString("new_title");  // Puede ser null
```

#### **✅ Solución Correcta**
```java
// Usar Repository directamente (sin Bundle estático)
public void saveText() {
    String title = titleEditText.getText().toString().trim();
    String content = contentEditText.getText().toString().trim();
    
    Text text = new Text();
    text.setTittle(title);
    text.setText(content);
    
    textRepository.insertText(text, new TextRepository.OnInsertCallback() {
        @Override
        public void onInsertComplete(boolean success, long id) {
            if (success) {
                // Navegar directamente, sin Bundle estático
                navigateBack();
            }
        }
    });
}
```

---

### **6️⃣ "Los callbacks no se ejecutan"**

#### **🔍 Síntomas**
- Operación de base de datos parece funcionar pero callback nunca se llama
- Toast no aparece, no hay errores

#### **🔍 Causas Principales**
1. ExecutorService shutdown
2. Exception silenciosa en background
3. Handler no configurado

#### **✅ Debug y Solución**
```java
// En TextRepository.java
private TextRepository(Context context) {
    this.textDao = AppDatabase.getInstance(context).textDao();
    this.executor = Executors.newSingleThreadExecutor();
    this.mainHandler = new Handler(Looper.getMainLooper());  // ✅ Asegurar Handler
    
    Log.d("REPO_DEBUG", "Repository inicializado");
}

public void insertText(Text text, OnInsertCallback callback) {
    Log.d("REPO_DEBUG", "Iniciando inserción de: " + text.getTittle());
    
    executor.execute(() -> {
        try {
            Log.d("REPO_DEBUG", "Ejecutando inserción en background");
            TextEntity entity = modelToEntity(text);
            long id = textDao.insert(entity);
            
            Log.d("REPO_DEBUG", "Inserción completada con ID: " + id);
            
            // Callback en main thread
            mainHandler.post(() -> {
                Log.d("REPO_DEBUG", "Ejecutando callback en main thread");
                if (callback != null) {
                    callback.onInsertComplete(id != -1, id);
                }
            });
        } catch (Exception e) {
            Log.e("REPO_DEBUG", "Error en inserción", e);
            
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onInsertComplete(false, -1);
                }
            });
        }
    });
}
```

---

## 🧪 **DEBUGGING AVANZADO**

### **7️⃣ "Performance lenta con textos grandes"**

#### **🔍 Síntomas**
- App se congela al cargar textos largos (>10,000 caracteres)
- UI no responde durante varios segundos

#### **🔍 Causa Principal**
Procesamiento síncrono de textos grandes en UI thread.

#### **✅ Solución**
```java
// Paginación progresiva en TextRepository
public void getTextPaginated(long id, int pageSize, OnTextPageCallback callback) {
    executor.execute(() -> {
        try {
            TextEntity entity = textDao.getById(id);
            String fullText = entity.content;
            
            // Dividir en chunks
            List<String> pages = splitIntoPages(fullText, pageSize);
            
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onPagesLoaded(pages);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error loading paginated text", e);
        }
    });
}

// En ReadingActivity.java
textRepository.getTextPaginated(textId, 5000, new OnTextPageCallback() {
    @Override
    public void onPagesLoaded(List<String> pages) {
        // Cargar primera página inmediatamente
        // Resto de páginas on-demand
    }
});
```

---

### **8️⃣ "Memory leaks al rotar pantalla"**

#### **🔍 Síntomas**
- Memory usage aumenta continuamente al rotar dispositivo
- App eventualmente se cierra por OutOfMemoryError

#### **🔍 Causa Principal**
Callbacks mantienen referencia a Fragment después de destroy.

#### **✅ Solución**
```java
// En TextFragment.java
@Override
public void onDestroy() {
    super.onDestroy();
    
    // Cancelar operaciones pendientes
    if (textRepository != null) {
        textRepository.cancelPendingOperations();
    }
}

// En TextRepository.java
public void cancelPendingOperations() {
    if (executor != null && !executor.isShutdown()) {
        executor.shutdownNow();
        // Recrear executor para futuras operaciones
        executor = Executors.newSingleThreadExecutor();
    }
}
```

---

## 🔧 **HERRAMIENTAS DE DEBUGGING**

### **Android Studio Database Inspector**
1. **Ejecutar app en emulator dispositivo real**
2. **View → Tool Windows → App Inspection**
3. **Database Inspector** → Seleccionar `leelo_database`
4. **Ver tablas, datos, ejecutar queries**

### **Logs Útiles**
```java
// Agregar estos logs en puntos clave
Log.d("LIFECYCLE", "onCreate: " + this.getClass().getSimpleName());
Log.d("LIFECYCLE", "onResume: " + this.getClass().getSimpleName());
Log.d("LIFECYCLE", "onDestroy: " + this.getClass().getSimpleName());

// En callbacks
Log.d("REPO_CALLBACK", "onInsertComplete: success=" + success + ", id=" + id);
Log.d("UI_CALLBACK", "updateUI: textCount=" + texts.size());
```

### **Breakpoints Estratégicos**
- **Repository callbacks**: Para verificar ejecución
- **Model conversion**: Para detectar nulls
- **Fragment lifecycle**: Para entender flujo

---

## 📋 **CHECKLIST DE DEBUGGING**

### **Antes de Reportar un Bug:**

#### **✅ Verificar Logs**
```bash
# Filtrar logs relevantes
adb logcat | grep -E "(Leelo|TextRepository|Room)"
```

#### **✅ Verificar Base de Datos**
- Abrir Database Inspector
- Verificar que tabla `texts` exista
- Verificar que los datos se guarden

#### **✅ Verificar Ciclo de Vida**
- Confirmar que `onCreate()` se llame
- Confirmar que `onDestroy()` no sea prematuro
- Verificar que `getActivity()` no sea null

#### **✅ Probar Edge Cases**
- Texto vacío
- Texto muy largo (>100,000 caracteres)
- Caracteres especiales (emojis, unicode)
- Rotación de pantalla
- App a background/foreground

---

## 🆘 **PEDIR AYUDA EFECTIVAMENTE**

### **📋 Información Requerida**

Cuando reportes un problema, incluye:

#### **1️⃣ Descripción Clara**
```
❌ "No funciona"
✅ "Cuando guardo un texto nuevo, no aparece en la lista"
```

#### **2️⃣ Pasos para Reproducir**
```
1. Abrir app
2. Click en botón "+" 
3. Escribir título "Test" y contenido "Hello world"
4. Click en "Guardar"
5. App regresa a la lista pero el nuevo texto no aparece
```

#### **3️⃣ Logs y Stack Traces**
```
adb logcat | grep -E "(Leelo|Room)" > leelo_logs.txt
# Adjuntar leelo_logs.txt
```

#### **4️⃣ Ambiente**
```
- Android Studio versión: 4.2.1
- Dispositivo: Pixel 4 API 30
- Versión app: 1.0.0
- Tiempo del problema: Ocurre 80% de las veces
```

#### **5️⃣ Qué Has Intentado**
```
Ya intenté:
- Limpiar cache de la app
- Reinstalar la app  
- Verificar Database Inspector
- Agregar logs en TextRepository
```

---

## 🎯 **PROBLEMAS FRECUENTES POR CATEGORÍA**

### **🗄️ Base de Datos**
| Problema | Síntoma | Solución |
|----------|----------|----------|
| Tabla no existe | "no such table" | Revisar AppDatabase inicialización |
| Thread principal | "Cannot access database" | Usar callbacks asíncronos |
| Datos inconsistentes | Cambios no se reflejan | Recargar desde BD (refreshTexts) |
| Memory leak | App se cierra por OOM | Liberar callbacks en onDestroy |

### **🎨 UI/UX**
| Problema | Síntoma | Solución |
|----------|----------|----------|
| getActivity() null | NullPointerException | Validar siempre antes de usar |
| UI no actualiza | Cambios no visibles | Usar runOnUiThread |
| Rotación de pantalla | Se pierden datos | Manejar onSaveInstanceState |

### **🔄 Navegación**
| Problema | Síntoma | Solución |
|----------|----------|----------|
| Fragment detach | Callbacks no ejecutan | Validar getActivity() |
| Bundle volátil | Datos se pierden | Usar Repository directamente |
| Navigation stack | No puede volver atrás | Usar replace() vs add() |

---

## 🎉 **RESUMEN RÁPIDO**

### **🔥 Los 5 Problemas Más Comunes:**

1. **❌ Database en main thread** → ✅ Usar callbacks
2. **❌ getActivity() null** → ✅ Validar siempre  
3. **❌ Lista no actualiza** → ✅ refreshTexts()
4. **❌ Bundle estático** → ✅ Repository directo
5. **❌ Callbacks no ejecutan** → ✅ Debug con logs

### **⚡ Fórmula Mágica de Debugging:**
```java
Log.d(TAG, "Starting operation");     // 1. Log inicio
validarInputs();                    // 2. Validar
repository.operacion(callback);       // 3. Operación async
// 4. Log en callback
Log.d(TAG, "Operation completed");  // 5. Log fin
```

### **🎯 Regla de Oro:**
> **"Si algo no funciona, revisa los callbacks y el threading"**

---

**Con esta guía, deberías poder resolver el 90% de los problemas que encontrarás en Leelo.** 🎯

Para problemas específicos no cubiertos aquí, consulta las otras guías o crea un issue con la información completa. 📝