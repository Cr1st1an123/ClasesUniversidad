# SIMA Planillas — Requerimientos (v0.1)

Sistema web para el registro de trabajadores y el control de asistencia de SIMA,
base del futuro cálculo de planillas.

## 1. Contexto

| Tema | Decisión |
|---|---|
| Cliente | Empresa real (SIMA) |
| Tamaño | Más de 500 trabajadores, varias áreas y sedes |
| Personal | Operarios técnicos: soldadores, mecánicos, electricistas, etc. |
| Tipo de app | Web (navegador) |
| Stack | Django + PostgreSQL, empaquetado con Docker |
| Despliegue | Por definir (servidor interno o nube); Docker lo hace portable |

## 2. Alcance de la versión 1

### Incluye
1. **Registro de trabajadores**: datos personales, DNI, cargo/oficio, área, sede,
   fecha de ingreso, sueldo.
2. **Contrato y régimen**: tipo de contrato, régimen laboral, fecha de inicio y fin,
   cese. Contratos históricos por trabajador.
3. **Importación masiva desde Excel** de los trabajadores actuales, con validación
   y reporte de errores por fila.
4. **Control de asistencia con huella digital** mediante relojes biométricos.
5. **Turnos rotativos** (mañana/tarde/noche, incluso turnos que cruzan la medianoche).
6. **Usuarios con roles** (ver sección 4).

### Fuera de alcance (versiones siguientes)
- Cálculo de planilla (descuentos ONP/AFP, renta de 5ta, EsSalud, horas extras)
- Boletas de pago
- CTS, gratificaciones, vacaciones, PLAME

## 3. Asistencia biométrica

- SIMA aún **no tiene lectores**. Recomendación: relojes biométricos de pared en red
  (p. ej. ZKTeco compatibles con protocolo **PUSH/ADMS**):
  - El reloj envía las marcaciones al servidor por HTTP sin software en las PCs.
  - Guarda las marcaciones si se cae la red y las reenvía después.
  - Enrolamiento de huellas en el propio equipo, vinculado por código o DNI del trabajador.
- El sistema guarda las **marcaciones en bruto** (inmutables) y aparte calcula la
  asistencia diaria (entrada, salida, tardanza, falta, horas trabajadas) según el
  turno asignado.
- Se deja una interfaz de integración genérica para poder cambiar de marca de reloj.
- Contingencia: registro manual de marcación, solo con justificación y auditoría.

## 4. Roles y permisos

| Acción | Administrador | RR.HH. / Registrador | Jefe de área |
|---|:-:|:-:|:-:|
| Gestionar usuarios y roles | ✔ | | |
| Catálogos (áreas, sedes, cargos, turnos, regímenes) | ✔ | | |
| Gestionar relojes biométricos | ✔ | | |
| Crear trabajadores / contratos | ✔ | ✔ | |
| Editar trabajadores | ✔ | ✔ | ✔ (solo su área) |
| Importar Excel | ✔ | ✔ | |
| Asignar turnos / programación | ✔ | ✔ | ✔ (solo su área) |
| Ver asistencia | ✔ | ✔ | ✔ (solo su área) |
| Justificar tardanzas y faltas | ✔ | ✔ | ✔ (solo su área) |

- Todos los cambios quedan en una **bitácora de auditoría** (quién, qué y cuándo).
- Los datos biométricos se tratan como datos sensibles (Ley 29733 de Protección
  de Datos Personales): acceso restringido y consentimiento del trabajador.

## 5. Modelo de datos inicial (borrador)

- `Sede`, `Area` (con jefe), `Cargo` (oficio), `RegimenLaboral`, `TipoContrato`
- `Trabajador`: código, DNI, nombres, apellidos, fecha de nacimiento, sexo, cargo,
  área, sede, fecha de ingreso, sueldo, estado (activo/cesado)
- `Contrato`: trabajador, tipo, régimen, fecha de inicio y fin, sueldo, motivo de cese
- `Turno`: nombre, hora de entrada y salida, tolerancia, cruza la medianoche
- `ProgramacionTurno`: trabajador, fecha, turno (rotación)
- `Reloj`: número de serie, sede, IP, última conexión
- `Marcacion`: trabajador, reloj, fecha y hora, tipo, origen (biométrico/manual)
- `AsistenciaDiaria`: trabajador, fecha, turno, entrada, salida, tardanza (min),
  estado, justificación
- `Justificacion`: asistencia, motivo, adjunto, usuario, fecha

## 6. Pendientes por definir

- [ ] Columnas de la plantilla Excel actual (compartir una **sin datos reales**).
- [ ] Lista exacta de regímenes laborales y tipos de contrato (los trabajadores son
      operarios técnicos; confirmar si hay régimen general, construcción civil, etc.).
- [ ] Reglas de asistencia: tolerancia de tardanza, cuándo cuenta como falta,
      cómo se arma la rotación de turnos (ciclos fijos o programación manual).
- [ ] Número de sedes y de relojes biométricos a instalar.
- [ ] Servidor de despliegue.

## 7. Plan de trabajo

1. **Base del proyecto**: Django, PostgreSQL, Docker, login y roles.
2. **Catálogos y trabajadores**: CRUD, contratos, búsqueda y filtros (>500 registros).
3. **Importación Excel**.
4. **Turnos y programación rotativa**.
5. **Asistencia**: recepción de marcaciones (PUSH/ADMS), cálculo diario y justificaciones.
6. **Reportes de asistencia** (exportar a Excel).
