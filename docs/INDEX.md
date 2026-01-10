# 📚 Documentación del Proyecto Leelo

## 🎯 **Navegación Rápida**

| Documento | Para Quién | Qué Encontrarás |
|-----------|------------|-----------------|
| 📄 **README.md** | 🧑‍💻 **Todos** | Visión general, características, tecnologías |
| 📁 **PROJECT_STRUCTURE.md** | 🏗️ **Arquitectos** | Estructura detallada de carpetas y componentes |
| ⚡ **QUICK_START.md** | 🚀 **Desarrolladores** | Guía práctica para empezar a codear en 5 min |
| 🗄️ **DATABASE_GUIDE.md** | 💾 **DB devs** | Todo sobre Room y persistencia |
| 🐛 **TROUBLESHOOTING.md** | 🔧 **Debugging** | Problemas comunes y soluciones |

---

## 🚀 **Para Empezar Rápidamente**

### **Nuevo en el proyecto?**
1. 📖 Lee [**QUICK_START.md**](QUICK_START.md) - Serás productivo en minutos
2. 🏗️ Revisa [**PROJECT_STRUCTURE.md**](PROJECT_STRUCTURE.md) - Entiende la arquitectura
3. 📋 Consulta [**README.md**](README.md) - Contexto general

### **Vas a modificar la base de datos?**
1. 📖 Lee [**DATABASE_GUIDE.md**](DATABASE_GUIDE.md) - Patrones Room
2. 🐛 Revisa [**TROUBLESHOOTING.md**](TROUBLESHOOTING.md) - Errores comunes

### **Buscando algo específico?**
- **Agregar funcionalidad** → QUICK_START.md → "Nueva Funcionalidad"
- **Entender el código** → PROJECT_STRUCTURE.md → "Componentes Clave"
- **Problemas de crash** → TROUBLESHOOTING.md → "Problemas Comunes"
- **Modificar base de datos** → DATABASE_GUIDE.md → "Schema Changes"

---

## 📋 **Información Esencial del Proyecto**

### **🏗️ Arquitectura**
```
UI (Fragments/Activities) 
    ↓ Repository Pattern
        ↓ Room Database 
            ↓ SQLite
                💾 Persistencia
```

### **🔧 Tecnologías Clave**
- **Lenguaje**: Java (sin Kotlin)
- **Base de Datos**: Room + SQLite
- **Arquitectura**: Repository Pattern
- **UI**: Material Design + Fragments
- **Async**: ExecutorService + Callbacks

### **📂 Estructura Principal**
```
com.app.leelo/
├── data/           # Room: entity, dao, database, repository
├── model/          # POJOs para UI
├── ui/             # Activities y Fragments
└── utils/          # Utilidades varias
```

### **🎯 Flujo Principal**
```
MainActivity → TextFragment → AddTextFragment → ReadingActivity
     ↓              ↓                ↓              ↓
Bottom Nav    Lista textos    Crear/editar    Lectura paginada
```

---

## ⚠️ **Reglas de Oro**

### **✅ Siempre Hacer**
- ✅ **Usar Repository**: Nunca acceder directamente a Room
- ✅ **Callbacks asíncronos**: Todas las operaciones DB
- ✅ **UI en main thread**: `runOnUiThread()` para actualizar UI
- ✅ **Validar inputs**: Antes de guardar/procesar
- ✅ **Manejar errores**: Con feedback al usuario

### **❌ Nunca Hacer**
- ❌ **Operaciones DB en UI thread**
- ❌ **Acceso directo a Room** (sin Repository)
- ❌ **Modificar UI desde background**
- ❌ **Ignorar callbacks de error**
- ❌ **Dejar memory leaks**

---

## 🎯 **Puntos de Entrada Principales**

### **🧠 TextRepository.java**
```java
// El cerebro de la aplicación
TextRepository repo = TextRepository.getInstance(context);
repo.insertText(text, callback);    // Guardar
repo.getAllTexts(callback);         // Leer todos
repo.updateText(text, callback);     // Actualizar
repo.deleteText(id, callback);      // Eliminar
```

### **📱 TextFragment.java**
```java
// Pantalla principal - lista de textos
loadTextsFromDatabase();           // Cargar datos
renderTexts();                    // Mostrar en UI
refreshTexts();                    // Recargar desde BD
```

### **➕ AddTextFragment.java**
```java
// Formulario para crear/editar textos
saveText();                        // Validar y guardar
navigateBack();                    // Volver a lista
```

### **📖 ReadingActivity.java**
```java
// Sistema de lectura paginada
divideTextIntoPages();            // Dividir texto automáticamente
setupViewPager();                 // Navegación entre páginas
```

---

## 🚨 **Problemas que Encontrarás**

### **#1 - "Cannot access database on main thread"**
**Causa**: Llamada síncrona a Room desde UI  
**Solución**: Usar callbacks del Repository

### **#2 - "getActivity() returns null"**
**Causa**: Fragment detachado  
**Solución**: Validar siempre `if (getActivity() != null)`

### **#3 - "Data not updating in UI"**
**Causa**: Modificar lista local vs recargar desde BD  
**Solución**: Usar `refreshTexts()` después de operaciones

### **#4 - "Bundle loses data"**
**Causa**: Bundle estático es volátil  
**Solución**: Usar Repository directamente

---

## 🎯 **Tareas Comunes**

### **Agregar nueva característica**
1. 📋 Definir en Entity/DAO
2. 🧠 Implementar en Repository  
3. 📱 Crear/modificar UI
4. 🧪 Probar extensivamente

### **Debugear problemas**
1. 📊 Usar Android Studio Database Inspector
2. 📝 Agregar logs en Repository
3. 🐛 Revisar callbacks y errores
4. 🔄 Testar edge cases

### **Performance**
1. ⚡ Operaciones asíncronas siempre
2. 🗄️ Usar índices en Room
3. 🎨 Optimizar layouts
4. 🧠 Evitar memory leaks

---

## 📞 **Soporte y Contribución**

### **Cómo Pedir Ayuda**
1. 📋 **Describe el problema** claramente
2. 📊 **Incluye logs** y stack traces
3. 🧪 **Muestra código** relevante
4. 🎯 **Especifica contexto**: qué estabas tratando de hacer

### **Cómo Contribuir**
1. 📖 **Leer esta documentación** completamente
2. 🏗️ **Seguir patrones existentes**
3. 🧪 **Testar cambios** extensivamente
4. 📝 **Documentar modificaciones** importantes

---

## 🎉 **¡Listo para Empezar!**

**Tu roadmap para ser productivo:**

🚀 **Day 1**: Lee QUICK_START.md → Agrega pequeña funcionalidad  
🏗️ **Day 2**: Lee PROJECT_STRUCTURE.md → Entiende arquitectura  
📊 **Day 3**: Usa Database Inspector → Debug operaciones BD  
🐛 **Day 4**: Lee TROUBLESHOOTING.md → Resuelve problemas reales  
🎯 **Day 5**: Contribuye con tu primera feature importante  

**¡Bienvenido al equipo Leelo!** 🎯

---

*Esta documentación está viva. Si algo no está claro o falta algo, por favor haz un pull request o issue.* 📝