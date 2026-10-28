let EVENTOS = [];
let EVENTOS_FILTRADOS = [];
let PAGINA = 1;
const POR_PAGINA = 20;

const ACCIONES = {
  REGISTRAR_PEDIDO: ['Pedido registrado', 'info'],
  MODIFICAR_PEDIDO: ['Pedido modificado', 'secondary'],
  INICIAR_PREPARACION: ['Preparación iniciada', 'warning'],
  MARCAR_LISTO: ['Pedido listo', 'success'],
  REGISTRAR_PAGO: ['Pago registrado', 'success'],
  DESPACHAR_PEDIDO: ['Pedido despachado', 'primary'],
  CANCELAR_PEDIDO: ['Pedido cancelado', 'danger'],
  RESTAURAR_PEDIDO: ['Pedido restaurado', 'secondary'],
  REGISTRAR_PLATO: ['Plato registrado', 'info'],
  MODIFICAR_PLATO: ['Plato modificado', 'secondary'],
  MARCAR_PLATO_DISPONIBLE: ['Plato disponible', 'success'],
  MARCAR_PLATO_AGOTADO: ['Plato agotado', 'warning'],
  DESACTIVAR_PLATO: ['Plato retirado', 'danger'],
  CREAR_USUARIO: ['Usuario creado', 'info'],
  ACTIVAR_USUARIO: ['Usuario activado', 'success'],
  DESACTIVAR_USUARIO: ['Usuario desactivado', 'danger'],
  DESBLOQUEAR_USUARIO: ['Usuario desbloqueado', 'success'],
  RESTABLECER_PASSWORD: ['Contraseña restablecida', 'warning'],
  CAMBIAR_PASSWORD: ['Contraseña cambiada', 'secondary'],
  CREAR_BACKUP: ['Backup creado', 'info'],
  RESTAURAR_BACKUP: ['Backup restaurado', 'warning']
};

function accionInfo(codigo) {
  return ACCIONES[codigo] || [String(codigo || '').replaceAll('_', ' ').toLowerCase().replace(/^./, c => c.toUpperCase()), 'secondary'];
}
function diaISO(valor) {
  if (!valor) return '';
  const d = new Date(valor); if (Number.isNaN(d.getTime())) return '';
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
}
function entidadTexto(x) {
  const e = (x.entidad || 'Evento').toLowerCase().replace(/^./, c => c.toUpperCase());
  return `${e}${x.entidadId ? ' #' + x.entidadId : ''}`;
}
function llenarSelects() {
  const usuarios = [...new Set(EVENTOS.map(x => x.usuario?.nombreUsuario || 'Sistema'))].sort((a,b)=>a.localeCompare(b));
  auditUsuario.innerHTML = '<option value="">Todos</option>' + usuarios.map(u => `<option value="${escapeHtml(u)}">${escapeHtml(u)}</option>`).join('');
  const acciones = [...new Set(EVENTOS.map(x => x.accion))].sort((a,b)=>accionInfo(a)[0].localeCompare(accionInfo(b)[0]));
  auditAccion.innerHTML = '<option value="">Todas</option>' + acciones.map(a => `<option value="${escapeHtml(a)}">${escapeHtml(accionInfo(a)[0])}</option>`).join('');
}
function filtrar() {
  const q = auditBuscar.value.trim().toLowerCase();
  const usuario = auditUsuario.value;
  const accion = auditAccion.value;
  const desde = auditDesde.value;
  const hasta = auditHasta.value;
  EVENTOS_FILTRADOS = EVENTOS.filter(x => {
    const u = x.usuario?.nombreUsuario || 'Sistema';
    const friendly = accionInfo(x.accion)[0];
    const texto = [u, x.accion, friendly, x.entidad, x.entidadId, x.descripcion].join(' ').toLowerCase();
    const d = diaISO(x.fechaEvento);
    return (!q || texto.includes(q)) && (!usuario || u === usuario) && (!accion || x.accion === accion) && (!desde || d >= desde) && (!hasta || d <= hasta);
  });
  PAGINA = 1;
  render();
}
function render() {
  const total = EVENTOS_FILTRADOS.length;
  const paginas = Math.max(1, Math.ceil(total / POR_PAGINA));
  if (PAGINA > paginas) PAGINA = paginas;
  const inicio = (PAGINA - 1) * POR_PAGINA;
  const xs = EVENTOS_FILTRADOS.slice(inicio, inicio + POR_PAGINA);
  auditConteo.textContent = total ? `Mostrando ${inicio + 1}-${Math.min(inicio + POR_PAGINA, total)} de ${total}` : '0 resultados';
  if (!xs.length) {
    auditoriaTabla.innerHTML = '<div class="empty-state"><div class="empty-state-icon">◎</div><div class="fw-bold text-dark mb-1">No se encontraron eventos</div><div>Prueba cambiando los filtros de búsqueda.</div></div>';
    auditPaginacion.innerHTML = '';
    return;
  }
  auditoriaTabla.innerHTML = `<table class="table table-hover table-app audit-table"><thead><tr><th>Fecha</th><th>Usuario</th><th>Acción</th><th>Entidad</th><th>Descripción</th><th></th></tr></thead><tbody>${xs.map(x => {
    const [nombre, tipo] = accionInfo(x.accion);
    return `<tr><td>${fecha(x.fechaEvento)}</td><td><strong>${escapeHtml(x.usuario?.nombreUsuario || 'Sistema')}</strong></td><td><span class="audit-action audit-${tipo}">${escapeHtml(nombre)}</span></td><td>${escapeHtml(entidadTexto(x))}</td><td class="audit-description">${escapeHtml(x.descripcion || '—')}</td><td class="text-end"><button class="btn btn-sm btn-outline-secondary" onclick="verDetalle(${x.idEvento})">Ver</button></td></tr>`;
  }).join('')}</tbody></table>`;
  renderPaginacion(paginas);
}
function renderPaginacion(paginas) {
  if (paginas <= 1) { auditPaginacion.innerHTML = ''; return; }
  const nums = [];
  for (let i = 1; i <= paginas; i++) if (i === 1 || i === paginas || Math.abs(i-PAGINA) <= 1) nums.push(i);
  const unique = [...new Set(nums)];
  let prev = 0;
  const botones = unique.map(n => {
    const gap = prev && n - prev > 1 ? '<span class="px-1 text-muted">…</span>' : '';
    prev = n;
    return `${gap}<button class="btn btn-sm ${n===PAGINA?'btn-jarana':'btn-outline-secondary'}" onclick="irPagina(${n})">${n}</button>`;
  }).join('');
  auditPaginacion.innerHTML = `<div class="d-flex justify-content-between align-items-center gap-2 flex-wrap"><button class="btn btn-sm btn-outline-secondary" ${PAGINA===1?'disabled':''} onclick="irPagina(${PAGINA-1})">← Anterior</button><div class="d-flex align-items-center gap-1">${botones}</div><button class="btn btn-sm btn-outline-secondary" ${PAGINA===paginas?'disabled':''} onclick="irPagina(${PAGINA+1})">Siguiente →</button></div>`;
}
function irPagina(n) { PAGINA = n; render(); window.scrollTo({ top: 0, behavior: 'smooth' }); }
function verDetalle(id) {
  const x = EVENTOS.find(e => e.idEvento === id); if (!x) return;
  const [nombre, tipo] = accionInfo(x.accion);
  detalleEventoFecha.textContent = fecha(x.fechaEvento);
  detalleEventoBody.innerHTML = `<div class="audit-detail-grid"><div><span>Usuario</span><strong>${escapeHtml(x.usuario?.nombreUsuario || 'Sistema')}</strong></div><div><span>Acción</span><strong><span class="audit-action audit-${tipo}">${escapeHtml(nombre)}</span></strong></div><div><span>Entidad</span><strong>${escapeHtml(entidadTexto(x))}</strong></div><div class="audit-detail-wide"><span>Descripción</span><strong>${escapeHtml(x.descripcion || '—')}</strong></div></div>`;
  bootstrap.Modal.getOrCreateInstance(document.getElementById('detalleEventoModal')).show();
}
async function cargarAuditoria() {
  try {
    const [xs, resumen] = await Promise.all([api('/api/auditoria'), api('/api/auditoria/resumen')]);
    EVENTOS = xs;
    EVENTOS_FILTRADOS = xs;
    auditTotal.textContent = resumen.totalEventos ?? xs.length;
    auditHoy.textContent = resumen.eventosHoy ?? 0;
    auditUsuarios.textContent = resumen.usuariosConActividad ?? 0;
    llenarSelects();
    render();
  } catch (e) { showError(e); }
}
document.addEventListener('DOMContentLoaded', () => {
  cargarAuditoria();
  [auditBuscar, auditUsuario, auditAccion, auditDesde, auditHasta].forEach(el => el.addEventListener(el.tagName === 'INPUT' && el.type === 'text' ? 'input' : 'change', filtrar));
  auditLimpiar.addEventListener('click', () => { auditBuscar.value=''; auditUsuario.value=''; auditAccion.value=''; auditDesde.value=''; auditHasta.value=''; filtrar(); });
});
