# TODO - Sistema de Vocabulario

## Objetivo
Implementar un sistema de aprendizaje de vocabulario en la app de lectura.

## Completado ✅

### 1. Base de Datos
- [x] Crear entidad `Word` con campos: id, word, meaning, state, createdAt, updatedAt
- [x] Crear `WordEntity.java` para Room
- [x] Crear `WordDao` con métodos CRUD
- [x] Agregar `WordEntity` a `AppDatabase`
- [x] Crear `WordRepository` (interfaz e implementación)
- [x] Agregar `RepositoryProvider` para acceso singleton

### 2. Capa de Presentación
- [x] Crear `WordViewModel` para gestionar palabras
- [x] Crear `WordViewModelFactory`
- [x] ViewModel con filtros: ALL, NEW, LEARNING, LEARNED

### 3. UI - Colores
- [x] Agregar colores en `colors.xml`:
  - word_new (#FF6B6B) - Rojo
  - word_learning (#4ECDC4) - Cyan
  - word_learned (#95A5A6) - Gris

### 4. UI - Palabras (WordsFragment)
- [x] Crear `fragment_words.xml` con TabLayout para filtros
- [x] Mostrar contador de palabras por estado
- [x] Crear `item_word.xml` para lista de palabras
- [x] Crear `WordsFragment.java` con adapter
- [x] Crear `bg_state_chip.xml` drawable

### 5. UI - Diálogos
- [x] Crear `dialog_add_word.xml` para agregar/editar palabras
- [x] Input para significado
- [x] Radio buttons para estado

### 6. UI - Lectura (ReadingActivity)
- [x] Todas las palabras son clickeables y se colorean
- [x] Si NO está en DB → color ROJO (Nueva)
- [x] Si está en DB → mostrar según estado (LEARNING=cyan, LEARNED=gris)
- [x] Click abre diálogo para guardar/editar/eliminar
- [x] Mejora UI del diálogo

## Correcciones realizadas
- [x] Agregar `getWordById` a WordDao y WordRepository
- [x] Corregir WordViewModel.getWordByIdLive() - usaba método incorrecto
- [x] ReadingActivity.saveWord() - ahora actualiza si la palabra existe
- [x] Eliminar RepositoryProvider duplicado en WordRepositoryImpl
- [x] Agregar null checks
- [x] Map para guardar estado de palabras
- [x] Implementar ClickableSpan para clicks en palabras
- [x] UI mejorada del diálogo
- [x] Corregir lógica de neutral button
- [x] Agregar Mapa de significados (savedMeaningsMap)
