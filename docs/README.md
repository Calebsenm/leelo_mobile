# Leelo - App de Lectura de Textos

Aplicación Android para practicar lectura con textos personalizados y paginación inteligente.

## 📋 **Resumen del Proyecto**

Leelo es una aplicación diseñada para el ejercicio de lectura, permitiendo a los usuarios:

- ✅ Agregar textos personalizados manualmente
- ✅ Importar textos desde URLs (planificado)
- ✅ Importar textos desde PDFs (planificado)
- ✅ Lectura paginada tipo libro
- ✅ Persistencia local con Room
- ✅ Navegación intuitiva entre fragmentos

## 🏗️ **Arquitectura del Proyecto**

### **Patrón Arquitectónico**
- **Repository Pattern**: Separación entre UI y base de datos
- **MVVM (Parcial)**: UI → Repository → Room → SQLite
- **Java Puro**: Sin frameworks complejos, código mantenible

### **Base de Datos**
- **Room Database**: Abstracción sobre SQLite
- **Persistencia Robusta**: Los datos sobreviven a cierres de app
- **Escalable**: Fácil migración a Firebase/MySQL en el futuro

## 🎯 **Características Principales**

### **1. Gestión de Textos**
- Agregar nuevos textos con título y contenido
- Editar textos existentes
- Eliminar textos
- Búsqueda por título
- Ordenamiento por fecha de creación

### **2. Sistema de Lectura**
- **Paginación Inteligente**: Divide textos automáticamente según tamaño de pantalla
- **Navegación Fluida**: Controles anterior/siguiente
- **Indicadores de Progreso**: Barra de progreso y contador de páginas
- **Diseño Legible**: Texto de 16sp con espaciado 1.2x

### **3. Persistencia de Datos**
- **Room Database**: Almacenamiento local robusto
- **Textos Largos**: Soporte para libros completos
- **Cache Inteligente**: Optimización para textos grandes
- **Exportación Futura**: Preparado para sincronización con nube

## 📱 **Experiencia de Usuario**

### **Flujo Principal**
1. **Inicio**: App abre en `TextFragment` (lista de textos)
2. **Agregar Texto**: Botón FAB → `AddTextFragment`
3. **Leer**: Click en texto → `ReadingActivity`
4. **Editar**: Click en "más" → `AddTextFragment` (modo edición)

### **Navegación**
- **Bottom Navigation**: Textos, Práctica, Configuración
- **Fragment Management**: Reemplazo dinámico sin stack
- **Consistencia**: Flujo intuitivo y predecible

## 🛠️ **Tecnologías Utilizadas**

### **Core Android**
- **Java**: Lenguaje principal
- **Fragments**: Arquitectura modular
- **ViewBinding**: Binding seguro de vistas
- **Material Design**: Componentes Google

### **Base de Datos**
- **Room**: Abstracción SQLite
- **SQLite**: Motor de base de datos nativo
- **Async Operations**: Ejecución en segundo plano
- **Singleton Pattern**: Conexión única a DB

### **UI/UX**
- **ViewPager2**: Navegación entre páginas
- **StaticLayout**: Cálculo de paginación
- **FloatingActionButton**: Acciones rápidas
- **BottomSheet**: Menús contextuales

## 📊 **Estado Actual del Proyecto**

### **✅ Funcionalidades Completadas**
- [x] Estructura de base de datos Room
- [x] CRUD completo de textos
- [x] Sistema de paginación inteligente
- [x] Navegación entre fragmentos
- [x] Persistencia robusta
- [x] Manejo de errores básico
- [x] Diseño Material consistente

### **🚧 Funcionalidades en Desarrollo**
- [ ] Importación desde URLs
- [ ] Importación desde PDFs
- [ ] Búsqueda avanzada
- [ ] Modo de práctica con temporizador
- [ ] Estadísticas de lectura

### **🔮 Funcionalidades Futuras**
- [ ] Sincronización con nube (Firebase)
- [ ] Modo offline avanzado
- [ ] Marcadores y notas
- [ ] Temas personalizados
- [ ] Text-to-speech

## 🚀 **Cómo Empezar**

### **Requisitos**
- Android Studio 4.0+
- JDK 11+
- Android API 26+ (Android 8.0+)
- Gradle 8.9+

### **Instalación**
```bash
# Clonar el repositorio
git clone [repository-url]
cd leelo

# Abrir en Android Studio
# Esperar a que se descarguen las dependencias
# Ejecutar la app
```

### **Configuración**
1. **Dependencias**: Las dependencias Room ya están configuradas
2. **Gradle**: Puede requerir actualizar a versión 8.13+
3. **Emulator**: Recomendado API 30+ para mejor compatibilidad

## 📁 **Estructura de Archivos**

La estructura del proyecto sigue convenciones Android con arquitectura limpia:

```
app/
├── src/main/java/com/app/leelo/
│   ├── data/                    # Capa de datos (Room)
│   │   ├── entity/            # Entidades Room (@Entity)
│   │   ├── dao/               # Data Access Objects (@Dao)
│   │   ├── database/          # AppDatabase (@Database)
│   │   └── repository/        # Repository Pattern
│   ├── model/                  # Modelos de UI (POJOs)
│   ├── ui/                     # Activities y Fragments
│   └── utils/                  # Utilidades varias
├── src/main/res/              # Recursos Android
├── docs/                      # 📚 Documentación
└── build.gradle.kts           # Configuración Gradle
```

## 🤝 **Contribución**

El proyecto sigue estándares de código limpio y patrones establecidos:

1. **Seguir arquitectura existente**
2. **Usar Room para persistencia**
3. **Mantener compatibilidad con el modelo Text**
4. **Documentar cambios importantes**
5. **Tests para nuevas funcionalidades**

## 📄 **Licencia**

[Tipo de licencia - agregar según corresponda]

---

**Última actualización**: [Fecha]  
**Versión**: 1.0.0  
**Estado**: Development  
**Maintainer**: [Tu nombre]