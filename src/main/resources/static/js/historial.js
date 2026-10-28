let HISTORIAL = [];
let AUDITORIA = [];

function ymd(valor) {
  if (!valor) return '';
  const d = new Date(valor);
  if (Number.isNaN(d.getTime())) return '';
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

function usuarioQueElimino(item) {
  const numero = String(item.pedido?.numero ?? '');
  const t = new Date(item.fechaHoraEliminacion).getTime();
  const candidatos = AUDITORIA
    .filter(a => a.accion === 'CANCELAR_PEDIDO' && String(a.entidadId ?? '') === numero)
    .map(a => ({ a, distancia: Math.abs(new Date(a.fechaEvento).getTime() - t) }))
    .sort((x, y) => x.distancia - y.distancia);
  const mejor = candidatos[0];
  if (!mejor || mejor.distancia > 60 * 60 * 1000) return '—';
  return mejor.a.usuario?.nombreUsuario || 'Sistema';
}

function actualizarKpis() {
  const hoy = ymd(new Date());
  kpiTotal.textContent = HISTORIAL.length;
  kpiHoy.textContent = HISTORIAL.filter(x => ymd(x.fechaHoraEliminacion) === hoy).length;
  kpiRestaurables.textContent = HISTORIAL.length;
}

function obtenerFiltrados() {
  const q = buscarHistorial.value.trim().toLowerCase();
  const desde = desdeHistorial.value;
  const hasta = hastaHistorial.value;
  return HISTORIAL.filter(x => {
    const usuario = usuarioQueElimino(x);
    const texto = [x.idEliminacion, x.pedido?.numero, x.pedido?.cliente?.nombre, x.motivo, usuario].join(' ').toLowerCase();
    const dia = ymd(x.fechaHoraEliminacion);
    return (!q || texto.includes(q)) && (!desde || dia >= desde) && (!hasta || dia <= hasta);
  });
}

function render() {
  const xs = obtenerFiltrados();
  if (!xs.length) {
    tabla.innerHTML = `<div class="empty-state history-empty"><div class="empty-state-icon">↶</div><div class="fw-bold text-dark mb-1">No hay pedidos eliminados</div><div>${HISTORIAL.length ? 'No se encontraron resultados con los filtros actuales.' : 'Cuando se cancele un pedido pendiente aparecerá aquí.'}</div></div>`;
    return;
  }
  tabla.innerHTML = `<table class="table table-hover table-app history-table">
    <thead><tr><th>Pedido</th><th>Cliente</th><th>Motivo</th><th>Eliminado por</th><th>Fecha</th><th class="text-end">Acción</th></tr></thead>
    <tbody>${xs.map(x => `<tr>
      <td><strong>#${x.pedido.numero}</strong></td>
      <td>${escapeHtml(x.pedido.cliente.nombre)}</td>
      <td><span class="history-reason">${escapeHtml(x.motivo)}</span></td>
      <td><span class="history-user">${escapeHtml(usuarioQueElimino(x))}</span></td>
      <td>${fecha(x.fechaHoraEliminacion)}</td>
      <td class="text-end"><button class="btn btn-sm btn-outline-danger" onclick="restaurarPedido(${x.idEliminacion},${x.pedido.numero})">↶ Restaurar</button></td>
    </tr>`).join('')}</tbody>
  </table>`;
}

async function cargar() {
  try {
    const [eliminados, auditoria] = await Promise.all([
      api('/api/pedidos/eliminados/historial'),
      api('/api/auditoria')
    ]);
    HISTORIAL = eliminados;
    AUDITORIA = auditoria;
    actualizarKpis();
    render();
  } catch (e) { showError(e); }
}

async function restaurarPedido(id, numero) {
  const ok=await appConfirm({title:`Restaurar pedido #${numero}`,message:'El pedido volverá al estado Pendiente y podrá atenderse nuevamente.',note:'La restauración quedará registrada en la actividad del sistema.',confirmText:'Restaurar pedido',tone:'success',icon:'↶'});
  if(!ok)return;
  try {
    const p = await api(`/api/pedidos/eliminados/${id}/restaurar`, { method: 'POST' });
    showToast(`Pedido #${p.numero} restaurado`);
    await cargar();
  } catch (e) { showError(e); }
}

document.addEventListener('DOMContentLoaded', () => {
  cargar();
  btnFiltrarHistorial.addEventListener('click', render);
  btnLimpiarHistorial.addEventListener('click', () => { buscarHistorial.value = ''; desdeHistorial.value = ''; hastaHistorial.value = ''; render(); });
  buscarHistorial.addEventListener('input', render);
  desdeHistorial.addEventListener('change', render);
  hastaHistorial.addEventListener('change', render);
});
