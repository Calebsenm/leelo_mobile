# 🚀 Guía Rápida de Desarrollo - Leelo

## 🎯 **Cómo Empezar en 5 Minutos**

### **1️⃣ Entender el Flujo Principal**

```
🏠 MainActivity
    ↓ (Bottom Navigation)
📋 TextFragment → Lista de textos guardados
    ↓ (FAB +)
➕ AddTextFragment → Crear/editar texto
    ↓ (Guardar)
💾 Room Database → Persistencia
    ↓ (Click texto)
📖 ReadingActivity → Lectura paginada
```

### **2️⃣ Puntos de Entrada Clave**

#### **🧠 TextRepository.java** - El Cerebro
```java
// Obtener instancia (singleton)
TextRepository repo = TextRepository.getInstance(context);

// Operaciones principales
repo.insertText(text, callback);     // Guardar
repo.getAllTexts(callback);          // Leer todos
repo.updateText(text, callback);     // Actualizar
repo.deleteText(id, callback);      // Eliminar
```

#### **📝 Model vs Entity**
```java
// Model para UI (usar siempre este)
Text uiText = new Text();
uiText.setTittle("Mi texto");
uiText.setText("Contenido...");

// Entity para BD (manejado por Repository)
TextEntity dbText = repository.modelToEntity(uiText); // Automático
```

---

## ⚡ **Operaciones Comunes**

### **💾 Guardar un Nuevo Texto**
```java
// En AddTextFragment.java
Text newText = new Text();
newText.setTittle("Título");
newText.setText("Contenido largo...");

textRepository.insertText(newText, new TextRepository.OnInsertCallback() {
    @Override
    public void onInsertComplete(boolean success, long id) {
        if (success) {
            Toast.makeText(getContext(), "¡Guardado!", Toast.LENGTH_SHORT).show();
            // Volver a la lista
        } else {
            Toast.makeText(getContext(), "Error al guardar", Toast.LENGTH_SHORT).show();
        }
    }
});
```

### **📖 Leer y Mostrar Textos**
```java
// En TextFragment.java
textRepository.getAllTexts(new TextRepository.OnGetAllCallback() {
    @Override
    public void onGetAllComplete(List<Text> texts) {
        // Actualizar UI en main thread
        requireActivity().runOnUiThread(() -> {
            adapter.updateData(texts);
            renderTexts();
        });
    }
});
```

### **✏️ Editar un Texto Existente**
```java
// En AddTextFragment.java (modo edición)
Text editText = new Text();
editText.setIdText(editId);           // ID existente
editText.setTittle("Nuevo título");
editText.setText("Nuevo contenido...");

textRepository.updateText(editText, new TextRepository.OnUpdateCallback() {
    @Override
    public void onUpdateComplete(boolean success) {
        if (success) {
            Toast.makeText(getContext(), "Actualizado", Toast.LENGTH_SHORT).show();
            navigateBack();
        }
    }
});
```

### **🗑️ Eliminar un Texto**
```java
// En TextFragment.java
textRepository.deleteText(text.getIdText(), new TextRepository.OnDeleteCallback() {
    @Override
    public void onDeleteComplete(boolean success) {
        if (success) {
            Toast.makeText(getContext(), "Eliminado", Toast.LENGTH_SHORT).show();
            refreshTexts(); // Recargar lista
        }
    }
});
```

---

## 🗂️ **Navegación Entre Pantallas**

### **Reemplazar Fragmentos**
```java
// Patrón usado en toda la app
MainActivity activity = (MainActivity) getActivity();
if (activity != null) {
    activity.replaceFragment(new TextFragment());
}
```

### **Pasar Datos entre Activities**
```java
// De TextFragment a ReadingActivity
Intent intent = new Intent(requireContext(), ReadingActivity.class);
intent.putExtra("title", text.getTittle());
intent.putExtra("text", text.getText());
startActivity(intent);
```

### **Recibir datos en Activity**
```java
// En ReadingActivity.java
String title = getIntent().getStringExtra("title");
String content = getIntent().getStringExtra("text");
```

---

## 🔧 **Patrones y Convenciones**

### **Callbacks Asíncronos (OBLIGATORIO)**
```java
// ✅ CORRECTO - Usar callbacks
repository.getAllTexts(new TextRepository.OnGetAllCallback() {
    @Override
    public void onGetAllComplete(List<Text> texts) {
        // Procesar resultado
    }
});

// ❌ INCORRECTO - Operación síncrona
List<Text> texts = repository.getAllTextsSync(); // No existe!
```

### **Actualización de UI**
```java
// ✅ CORRECTO - Siempre en main thread
if (getActivity() != null) {
    getActivity().runOnUiThread(() -> {
        // Actualizar UI aquí
        adapter.notifyDataSetChanged();
        Toast.makeText(getContext(), "Listo", Toast.LENGTH_SHORT).show();
    });
}

// ❌ INCORRECTO - Modificar UI desde background thread
adapter.notifyDataSetChanged(); // Puede crash!
```

### **Manejo de Errores**
```java
// ✅ CORRECTO - Siempre validar
if (text.getTittle() == null || text.getTittle().isEmpty()) {
    Toast.makeText(getContext(), "El título es requerido", Toast.LENGTH_SHORT).show();
    return;
}

// ✅ CORRECTO - Validar respuesta
if (!success) {
    Toast.makeText(getContext(), "Error en la operación", Toast.LENGTH_SHORT).show();
    return;
}
```

---

## 🐛 **Problemas Comunes y Soluciones**

### **1️⃣ "NullPointerException: getActivity()"**
```java
// ❌ PROBLEMA
getActivity().runOnUiThread(...); // getActivity() puede ser null

// ✅ SOLUCIÓN
MainActivity activity = (MainActivity) getActivity();
if (activity != null) {
    activity.runOnUiThread(...);
}
```

### **2️⃣ "Cannot access database on the main thread"**
```java
// ❌ PROBLEMA
TextEntity entity = textDao.getById(id); // UI thread!

// ✅ SOLUCIÓN - Siempre usar callbacks
textRepository.getByIdAsync(id, new TextRepository.OnGetByIdCallback() {
    @Override
    public void onGetByIdComplete(Text text) {
        // Procesar resultado
    }
});
```

### **3️⃣ "Texto no se actualiza en la lista"**
```java
// ❌ PROBLEMA - Modificar lista local directamente
texts.add(newText);
renderTexts();

// ✅ SOLUCIÓN - Recargar desde base de datos
refreshTexts(); // Llama a repository.getAllTexts()
```

### **4️⃣ "Bundle estático pierde datos"**
```java
// ❌ PROBLEMA - Bundle estático es volátil
MainActivity.textUpdateData.putString(...);

// ✅ SOLUCIÓN - Usar repository directamente
textRepository.insertText(text, callback);
```

---

## 🧪 **Testing y Debugging**

### **Debugear Base de Datos**
1. **Android Studio**: View → Tool Windows → App Inspection
2. **Database Inspector**: Ver archivos `.db` en tiempo real
3. **Ubicación**: `/data/data/com.app.leelo/databases/leelo_database`

### **Logs Útiles**
```java
// En TextRepository.java
Log.d(TAG, "Guardando texto: " + text.getTittle());
Log.e(TAG, "Error en base de datos", exception);

// En Fragments
Log.d(TAG, "Cargando " + texts.size() + " textos");
```

### **Probar Callbacks**
```java
// Simular éxito/fallo
@Override
public void onInsertComplete(boolean success, long id) {
    Log.d(TAG, "Insert: success=" + success + ", id=" + id);
    // Testar ambos casos
}
```

---

## 🎯 **Tareas Comunes con Ejemplos**

### **Agregar Nueva Funcionalidad**

#### **1. Nueva característica: Búsqueda avanzada**
```java
// Paso 1: Agregar método en TextDao.java
@Query("SELECT * FROM texts WHERE content LIKE '%' || :query || '%'")
List<TextEntity> searchContent(String query);

// Paso 2: Agregar método en TextRepository.java
public void searchContent(String query, OnSearchCallback callback) {
    executor.execute(() -> {
        List<TextEntity> entities = textDao.searchContent(query);
        List<Text> texts = convertEntities(entities);
        callback.onSearchComplete(texts);
    });
}

// Paso 3: Usar en UI
textRepository.searchContent("palabra clave", new TextRepository.OnSearchCallback() {
    @Override
    public void onSearchComplete(List<Text> results) {
        // Mostrar resultados
    }
});
```

#### **2. Nuevo campo: Categoría de texto**
```java
// Paso 1: Modificar TextEntity.java
@ColumnInfo(name = "category")
public String category;

// Paso 2: Modificar Text.java (modelo UI)
private String category;

// Paso 3: Agregar setter/getter
public String getCategory() { return category; }
public void setCategory(String category) { this.category = category; }

// Paso 4: Actualizar conversiones en Repository
entity.category = model.getCategory();
model.setCategory(entity.category);
```

---

## 📋 **Checklist Antes de Commitear**

### **Code Review**
- [ ] **Callbacks**: Todas las operaciones DB usan callbacks
- [ ] **UI Threading**: Actualizaciones de UI en main thread
- [ ] **Error Handling**: Validaciones y manejo de errores
- [ ] **Memory**: No leaks de activities/fragments
- [ ] **Logging**: Logs útiles para debugging

### **Testing**
- [ ] **Happy Path**: Funciona cuando todo va bien
- [ ] **Error Cases**: Maneja errores correctamente
- [ ] **Edge Cases**: Textos vacíos, null, muy largos
- [ ] **Rotation**: No pierde datos al rotar pantalla
- [ ] **Memory**: No consume memoria excesiva

---

## 🚀 **Siguientes Pasos Recomendados**

### **Para nuevos desarrolladores:**
1. **Leer `PROJECT_STRUCTURE.md`** - Entender arquitectura
2. **Revisar `TextRepository.java`** - Ver patrones de código
3. **Modificar `TextFragment.java`** - Agregar nueva funcionalidad simple
4. **Probar extensivamente** - Asegurar robustez

### **Para nuevas funcionalidades:**
1. **Planear Entity primero** - Estructura de datos
2. **Implementar DAO** - Operaciones básicas
3. **Agregar Repository** - Lógica de negocio
4. **Crear UI** - Fragment/Activity
5. **Integrar todo** - Conectar piezas
6. **Testear** - Manual y automático

---

**Esta guía te permite ser productivo en Leelo desde el primer día.** 🎯