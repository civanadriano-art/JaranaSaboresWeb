let platos = [];
let modalPlato;

function codigoVisual(codigo) {
  return `P${String(codigo).padStart(3, '0')}`;
}

function actualizarIndicadores() {
  document.getElementById('kpiTotal').textContent = platos.length;
  document.getElementById('kpiDisponibles').textContent = platos.filter(p => p.disponible !== false).length;
  document.getElementById('kpiAgotados').textContent = platos.filter(p => p.disponible === false).length;
}

function cargarCategorias() {
  const select = document.getElementById('filtroCategoria');
  const actual = select.value;
  const categorias = [...new Set(platos.map(p => p.categoria || 'Sin categoría'))].sort((a, b) => a.localeCompare(b, 'es'));
  select.innerHTML = '<option value="">Todas</option>' + categorias.map(c => `<option value="${escapeHtml(c)}">${escapeHtml(c)}</option>`).join('');
  if (categorias.includes(actual)) select.value = actual;
}

function obtenerFiltrados() {
  const q = document.getElementById('buscarPlato').value.trim().toLowerCase();
  const categoria = document.getElementById('filtroCategoria').value;
  const disponibilidad = document.getElementById('filtroDisponibilidad').value;

  return platos.filter(p => {
    const codigo = codigoVisual(p.codigo).toLowerCase();
    const coincideTexto = !q || (p.nombre || '').toLowerCase().includes(q) || codigo.includes(q) || String(p.codigo).includes(q);
    const coincideCategoria = !categoria || (p.categoria || 'Sin categoría') === categoria;
    const coincideDisponibilidad = !disponibilidad || (disponibilidad === 'disponible' ? p.disponible !== false : p.disponible === false);
    return coincideTexto && coincideCategoria && coincideDisponibilidad;
  });
}

function renderTabla() {
  const ps = obtenerFiltrados();
  document.getElementById('resultadoCatalogo').textContent = `${ps.length} ${ps.length === 1 ? 'plato encontrado' : 'platos encontrados'}`;

  if (!ps.length) {
    document.getElementById('tabla').innerHTML = '<div class="empty-state py-5">No hay platos que coincidan con los filtros.</div>';
    return;
  }

  document.getElementById('tabla').innerHTML = `
    <table class="table table-hover table-app catalog-table">
      <thead><tr><th>Código</th><th>Plato</th><th>Categoría</th><th>Precio</th><th>Disponibilidad</th><th class="text-end">Acciones</th></tr></thead>
      <tbody>${ps.map(p => `
        <tr>
          <td><strong>${codigoVisual(p.codigo)}</strong></td>
          <td><div class="fw-semibold">${escapeHtml(p.nombre)}</div></td>
          <td>${escapeHtml(p.categoria || 'Sin categoría')}</td>
          <td><strong>${money(p.precio)}</strong></td>
          <td>${p.disponible !== false
              ? '<span class="catalog-availability available">● Disponible</span>'
              : '<span class="catalog-availability soldout">● Agotado</span>'}</td>
          <td><div class="catalog-actions">
            <button class="btn btn-sm btn-outline-primary" type="button" onclick="editarPlato(${p.codigo})">Editar</button>
            <button class="btn btn-sm ${p.disponible !== false ? 'btn-outline-warning' : 'btn-outline-success'}" type="button" onclick="cambiarDisponibilidad(${p.codigo}, ${p.disponible === false})">${p.disponible !== false ? 'Marcar agotado' : 'Marcar disponible'}</button>
            <button class="btn btn-sm btn-outline-danger" type="button" onclick="retirarPlato(${p.codigo})">Retirar</button>
          </div></td>
        </tr>`).join('')}</tbody>
    </table>`;
}

async function cargar() {
  try {
    platos = await api('/api/platos');
    actualizarIndicadores();
    cargarCategorias();
    renderTabla();
  } catch (e) {
    showError(e);
  }
}

function limpiarFormulario() {
  document.getElementById('codigo').value = '';
  document.getElementById('nombre').value = '';
  document.getElementById('categoria').value = 'Platos de fondo';
  document.getElementById('precio').value = '';
  document.getElementById('disponible').checked = true;
}

function abrirNuevo() {
  limpiarFormulario();
  document.getElementById('modalPlatoTitulo').textContent = 'Nuevo plato';
  document.getElementById('modalPlatoSubtitulo').textContent = 'Completa los datos del plato.';
  document.getElementById('btnGuardarPlato').textContent = 'Guardar plato';
  modalPlato.show();
  setTimeout(() => document.getElementById('nombre').focus(), 200);
}

function editarPlato(codigo) {
  const p = platos.find(x => x.codigo === codigo);
  if (!p) return;
  document.getElementById('codigo').value = p.codigo;
  document.getElementById('nombre').value = p.nombre || '';
  document.getElementById('categoria').value = p.categoria || 'Platos de fondo';
  document.getElementById('precio').value = p.precio;
  document.getElementById('disponible').checked = p.disponible !== false;
  document.getElementById('modalPlatoTitulo').textContent = 'Editar plato';
  document.getElementById('modalPlatoSubtitulo').textContent = `${codigoVisual(p.codigo)} · ${p.nombre}`;
  document.getElementById('btnGuardarPlato').textContent = 'Guardar cambios';
  modalPlato.show();
}

async function guardarPlato(e) {
  e.preventDefault();
  try {
    const codigo = document.getElementById('codigo').value;
    const body = JSON.stringify({
      nombre: document.getElementById('nombre').value.trim(),
      categoria: document.getElementById('categoria').value,
      precio: Number(document.getElementById('precio').value),
      disponible: document.getElementById('disponible').checked
    });
    if (codigo) await api(`/api/platos/${codigo}`, { method: 'PUT', body });
    else await api('/api/platos', { method: 'POST', body });
    modalPlato.hide();
    showToast(codigo ? 'Plato actualizado' : 'Plato registrado');
    await cargar();
  } catch (e) {
    showError(e);
  }
}

async function cambiarDisponibilidad(codigo, disponible) {
  const p = platos.find(x => x.codigo === codigo);
  if (!p) return;
  try {
    await api(`/api/platos/${codigo}/disponibilidad?disponible=${disponible}`, { method: 'PATCH' });
    showToast(disponible ? `${p.nombre} vuelve a estar disponible` : `${p.nombre} fue marcado como agotado`);
    await cargar();
  } catch (e) {
    showError(e);
  }
}

async function retirarPlato(codigo) {
  const p = platos.find(x => x.codigo === codigo);
  if (!p) return;
  if (!confirm(`¿Retirar “${p.nombre}” del catálogo?\n\nYa no aparecerá para registrar nuevos pedidos.`)) return;
  try {
    await api(`/api/platos/${codigo}`, { method: 'DELETE' });
    showToast('Plato retirado del catálogo');
    await cargar();
  } catch (e) {
    showError(e);
  }
}

function limpiarFiltros() {
  document.getElementById('buscarPlato').value = '';
  document.getElementById('filtroCategoria').value = '';
  document.getElementById('filtroDisponibilidad').value = '';
  renderTabla();
}

document.addEventListener('DOMContentLoaded', () => {
  modalPlato = new bootstrap.Modal(document.getElementById('modalPlato'));
  document.getElementById('formPlato').addEventListener('submit', guardarPlato);
  document.getElementById('buscarPlato').addEventListener('input', renderTabla);
  document.getElementById('filtroCategoria').addEventListener('change', renderTabla);
  document.getElementById('filtroDisponibilidad').addEventListener('change', renderTabla);
  cargar();
});
