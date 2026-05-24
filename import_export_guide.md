# Import/Export Guide — Llevar PayFlow + Engram a otra máquina

> **Propósito**: Migrar todo el contexto de desarrollo de PayFlow (código, especificaciones, memoria Engram, configuración opencode) desde esta máquina a otra, manteniendo la continuidad del trabajo.

---

## Tabla de Contenidos

1. [Arquitectura de la portabilidad](#1-arquitectura-de-la-portabilidad)
2. [Qué hay que mover](#2-qué-hay-que-mover)
3. [Estrategia recomendada: OpenSpec + Engram (Both)](#3-estrategia-recomendada-openspec--engram-both)
4. [Guía paso a paso — Máquina origen (exportar)](#4-guía-paso-a-paso--máquina-origen-exportar)
5. [Guía paso a paso — Máquina destino (importar)](#5-guía-paso-a-paso--máquina-destino-importar)
6. [Verificación post-importación](#6-verificación-post-importación)
7. [Solución de problemas comunes](#7-solución-de-problemas-comunes)
8. [Referencia: estructura de archivos](#8-referencia-estructura-de-archivos)

---

## 1. Arquitectura de la portabilidad

El ecosistema de desarrollo tiene **tres capas** con distinto nivel de portabilidad:

| Capa | Dónde vive | ¿Portátil? | Cómo se mueve |
|---|---|---|---|
| **Código fuente** | Repositorio git | ✅ Excelente | `git clone` / `git pull` |
| **OpenSpec artifacts** | `openspec/` en el repo | ✅ Excelente | Viaja con git |
| **SDD artifacts (Engram)** | `~/.engram/engram.db` | 🟡 Manual | Copiar archivos DB |
| **Configuración opencode** | `~/.config/opencode/` | 🟡 Manual | Copiar directorio |
| **Skills personalizados** | `~/.config/opencode/skills/` | 🟡 Manual | Copiar directorio |

**Conclusión**: el código y OpenSpec viajan solos con git. Engram y la config de opencode requieren copia manual.

---

## 2. Qué hay que mover

### 2.1 Máquina Origen — Checklist de exportación

```
C:\Users\<USER>\
├── .engram\                          ← [IMPORTANTE] Memoria persistente
│   ├── engram.db                     ← Base SQLite ~1-5 MB
│   ├── engram.db-wal                 ← Write-Ahead Log (copiar junto con .db)
│   └── engram.db-shm                 ← Shared Memory (copiar junto con .db)
│
├── .config\opencode\                 ← [IMPORTANTE] Config de opencode + skills
│   ├── opencode.json                 ← Configuración de agentes y MCP
│   ├── AGENTS.md                     ← Instrucciones de sistema
│   ├── skills\                       ← Skills personalizados (SDD, branch-pr, etc.)
│   │   ├── sdd-apply\
│   │   ├── sdd-spec\
│   │   ├── sdd-design\
│   │   ├── sdd-tasks\
│   │   ├── branch-pr\
│   │   ├── chained-pr\
│   │   └── ...                       ← Todos los skills instalados
│   ├── prompts\sdd\                  ← Prompts de cada fase SDD
│   └── profiles\                     ← Perfiles de configuración
│
└── Documents\Projects\PayFlow\       ← [VIAJA CON GIT] Repositorio
    ├── openspec\                     ← OpenSpec artifacts (git-tracked)
    ├── backend\
    ├── frontend\
    ├── PayFlow_Specification_v2.md
    └── ... (resto del proyecto)
```

### 2.2 Lo que NO hace falta mover

| Elemento | Motivo |
|---|---|
| `node_modules/` | Se regenera con `npm install` |
| `target/`, `build/` | Se regenera con Maven |
| `.git/` | Se regenera con `git clone` |
| Docker images | Se descargan o rebuildan |
| Extensiones de VS Code | Se instalan desde marketplace |

---

## 3. Estrategia recomendada: OpenSpec + Engram (Both)

La mejor combinación para portabilidad es **modo `both`** en SDD:

- **OpenSpec** (archivos en `openspec/`): specs, designs, tasks, archive reports → viajan en git
- **Engram**: progreso de sesiones activas, contexto transitorio → se copia manualmente

**Ventaja**: si perdés la DB de Engram, nunca perdés el trabajo estructural. Los specs y designs están en el repo, versionados en git.

### Cómo activarlo

Cuando inicies SDD en cualquier sesión, elegí:

```
B3 — Both: OpenSpec files plus Engram copy
```

Esto hace que SDD escriba artifacts en `openspec/` Y los guarde en Engram simultáneamente.

---

## 4. Guía paso a paso — Máquina origen (exportar)

### Paso 1: Pushear el repo con todos los cambios

```powershell
cd C:\Users\oscar\Documents\Projects\PayFlow
git status                              # Verificar que no hay cambios sin commit
git add -A
git commit -m "feat: checkpoint before migration"
git push origin main
```

> **Asegurate** de que `openspec/` esté trackeado en git. Si no existe todavía, inicializalo antes de migrar.

### Paso 2: Verificar que Engram esté íntegro

```powershell
# El doctor check pasa si no hay corrupción
# (se ejecuta automáticamente cada vez que se usa opencode)
```

No hace falta correr nada manual — si opencode funciona, Engram está bien.

### Paso 3: Copiar Engram

```powershell
# Detener opencode (cerrar VS Code / terminal)

# Copiar la base de datos
Copy-Item -Path "$env:USERPROFILE\.engram" -Destination "D:\backup\engram-payflow" -Recurse -Force

# Verificar que los 3 archivos se copiaron
Get-ChildItem -Path "D:\backup\engram-payflow"
# Debería mostrar: engram.db, engram.db-shm, engram.db-wal
```

> **Importante**: copiá el directorio `.engram` ENTERO. Los 3 archivos (`.db`, `.db-shm`, `.db-wal`) son parte del mismo estado de SQLite. Copiar solo `.db` sin los otros puede perder datos.

### Paso 4: Copiar configuración de opencode

```powershell
# También copiar la configuración completa de opencode
$backupDir = "D:\backup\opencode-config"
Copy-Item -Path "$env:USERPROFILE\.config\opencode" -Destination $backupDir -Recurse -Force

# Verificar que los skills estén
Get-ChildItem -Path "$backupDir\skills" -Directory
```

### Paso 5: Respaldar archivos del proyecto (opcional)

```powershell
# Si hay archivos sin commit (se recomienda commitear todo antes)
# Sacar un diff por si algo quedó fuera
git diff --stat > "D:\backup\uncommitted-changes.txt"
```

---

## 5. Guía paso a paso — Máquina destino (importar)

### Paso 1: Instalar opencode / gentle-ai

Seguí las instrucciones oficiales de instalación para tu SO.

```powershell
# En Windows — verificar instalación
opencode --version

# Debería mostrar la versión instalada
```

> **Nota**: la versión de opencode en la máquina destino debería ser >= la versión de origen para asegurar compatibilidad del schema de Engram.

### Paso 2: Clonar el repositorio

```powershell
cd C:\Users\<NUEVO_USER>\Documents\Projects\
git clone https://github.com/<TU_USER>/PayFlow.git
cd PayFlow
```

### Paso 3: Restaurar configuración de opencode

```powershell
# Cerrar VS Code / terminal

# Restaurar la configuración completa desde el backup
$backupDir = "D:\backup\opencode-config"   # o USB, o donde tengas el backup
Copy-Item -Path "$backupDir\*" -Destination "$env:USERPROFILE\.config\opencode\" -Recurse -Force

# Verificar que los skills se restauraron
Get-ChildItem -Path "$env:USERPROFILE\.config\opencode\skills" -Directory
```

### Paso 4: Restaurar Engram

```powershell
# Ubicar la carpeta de backup de Engram
$engramBackup = "D:\backup\engram-payflow"

# Verificar que contiene los archivos esperados
Get-ChildItem -Path $engramBackup

# Copiar a la máquina destino
# NOTA: Si ~/.engram ya existe, hace backup antes:
# Move-Item -Path "$env:USERPROFILE\.engram" -Destination "$env:USERPROFILE\.engram.bak" -Force
Copy-Item -Path "$engramBackup\*" -Destination "$env:USERPROFILE\.engram\" -Recurse -Force
```

### Paso 5: Verificar que todo esté en su lugar

```powershell
# Verificar estructura
Test-Path "$env:USERPROFILE\.engram\engram.db"
Test-Path "$env:USERPROFILE\.config\opencode\opencode.json"
Test-Path "C:\Users\<NUEVO_USER>\Documents\Projects\PayFlow\.git"
```

---

## 6. Verificación post-importación

### 6.1 Verificar que Engram responde

Abrí VS Code y en la terminal de opencode preguntá:

```
/remember ¿qué fases de PayFlow están completas?
```

O ejecutá cualquier comando SDD (como `/sdd-explore`). Si el contexto de sesiones previas aparece, Engram se importó correctamente.

### 6.2 Verificar que los SDD artifacts están

```bash
# En el repo clonado
ls openspec/
# Deberían verse los archivos de fases anteriores
```

### 6.3 Verificar que opencode corre correctamente

```powershell
opencode --version
```

### 6.4 Ejecutar doctor check (opcional)

Si usás Engram, podés verificar el estado con:

```
# Desde una conversación con gentle-ai, preguntar:
"run engram doctor for PayFlow"
```

---

## 7. Solución de problemas comunes

### 7.1 "No se encuentra Engram" después de importar

**Causa probable**: la ruta de instalación de Engram no coincide.

**Solución**:
```powershell
# Verificar dónde está instalado Engram
Get-Command engram.exe -ErrorAction SilentlyContinue
# Si no está, instalarlo o configurar la ruta en opencode.json
```

Además, verificá que `opencode.json` tenga la ruta correcta al binario de Engram:

```json
"mcp": {
  "engram": {
    "command": ["C:\\Path\\to\\engram.exe", "mcp", "--tools=agent"],
    "type": "local"
  }
}
```

### 7.2 Los archivos de configuración no se restauraron

**Causa probable**: permisos de escritura en `~/.config/opencode/`.

**Solución**:
```powershell
# Verificar permisos
Get-Acl -Path "$env:USERPROFILE\.config\opencode" | Format-List

# Si hay problemas, copiar como administrador
Start-Process powershell -Verb RunAs
# En la ventana admin:
Copy-Item -Path "D:\backup\opencode-config\*" -Destination "$env:USERPROFILE\.config\opencode\" -Recurse -Force
```

### 7.3 Los proyectos tienen rutas distintas y no se asocian

Engram asocia observaciones a proyectos por:
1. Project name (git remote)
2. Project path (directorio local)

Si en la máquina destino el repo está en una ruta distinta, algunas observaciones pueden no matchear automáticamente. En ese caso:

```powershell
# La primera vez que abras el proyecto en la nueva máquina,
# gentle-ai va a detectar el proyecto y asociarlo correctamente
# por el git remote. Las observaciones deberían aparecer al buscar.
```

Si alguna búsqueda no encuentra datos, usá el nombre del proyecto explícitamente:

```
/remember buscá en el proyecto PayFlow sobre rate limiting
```

### 7.4 Error "SQLite schema mismatch"

**Causa probable**: la versión de opencode/Engram en la máquina destino es distinta.

**Solución**: actualizá opencode en la máquina destino a la misma versión que la máquina origen. La DB de Engram evoluciona con el schema de la aplicación.

### 7.5 La copia manual da error de "archivo en uso"

**Causa probable**: opencode está corriendo (VS Code, terminal con sesión activa).

**Solución**: cerrar VS Code y cualquier terminal que ejecute opencode antes de copiar.

```powershell
# Verificar que ningún proceso tenga el archivo abierto
Get-Process | Where-Object { $_.ProcessName -like "*opencode*" -or $_.ProcessName -like "*engram*" } | Stop-Process -Force
```

Después de eso, copiar de nuevo.

---

## 8. Referencia: estructura de archivos

### En la máquina origen

```
C:\Users\oscar\
├── .engram\                                    ← ~5 MB - copiar completo
│   ├── engram.db
│   ├── engram.db-shm
│   └── engram.db-wal
│
├── .config\opencode\                           ← ~10-50 MB - copiar completo
│   ├── opencode.json
│   ├── AGENTS.md
│   ├── .gitignore
│   ├── skills\                                 ← Skills personalizados
│   ├── prompts\sdd\                            ← Fase prompts
│   ├── profiles\                               ← Config profiles
│   ├── commands\                               ← Custom commands
│   ├── plugins\                                ← Plugins
│   ├── tui-plugins\                            ← TUI plugins
│   ├── node_modules\                           ← Se regenera (opcional)
│   └── tui.json
│
└── Documents\Projects\PayFlow\                 ← Viaja en git
    ├── .git\
    ├── openspec\                               ← OpenSpec artifacts (git)
    ├── backend\
    ├── frontend\
    ├── infra\
    ├── PayFlow_Specification_v2.md
    ├── PayFlow_Specification.docx.txt
    └── import_export_guide.md                  ← Este archivo
```

### En la máquina destino (después de importar)

```
C:\Users\<NUEVO_USER>\
├── .engram\                                    ← Restaurado desde backup
│   ├── engram.db
│   ├── engram.db-shm
│   └── engram.db-wal
│
├── .config\opencode\                           ← Restaurado desde backup
│   └── (misma estructura que origen)
│
└── Documents\Projects\PayFlow\                 ← Clonado de git
    └── (misma estructura que origen)
```

---

## Resumen: el workflow ideal para portabilidad

```
┌─────────────────────────────────────────────────────┐
│            WORKFLOW RECOMENDADO                      │
├─────────────────────────────────────────────────────┤
│                                                      │
│  Máquina A (origen)          Máquina B (destino)     │
│  ┌─────────────────┐         ┌─────────────────┐    │
│  │ git push         │─────→  │ git clone        │    │
│  │ (código +        │        │ (código +        │    │
│  │  openspec/)      │        │  openspec/)      │    │
│  └─────────────────┘         └─────────────────┘    │
│         │                            │               │
│         │ manual                     │               │
│         ▼                            ▼               │
│  copiar .engram/ ───────────────→  restaurar         │
│  copiar .config/ ───────────────→  .engram/          │
│                                     .config/         │
│                                                      │
│  Resultado: mismo código + misma memoria            │
│             + misma configuración                    │
│                                                      │
└─────────────────────────────────────────────────────┘
```

**La clave**: con OpenSpec (modo `both`), aunque pierdas Engram, TODO el trabajo estructural (specs, designs, tasks) sobrevive en git. Engram solo suma la memoria de sesiones activas — valiosa, pero no crítica.

---

*Documento generado el 2026-05-24 para PayFlow — gentle-ai / opencode*
