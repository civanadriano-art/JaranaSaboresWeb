let USUARIOS=[];
let USUARIO_ACTUAL=null;

const modalNuevo=()=>bootstrap.Modal.getOrCreateInstance(document.getElementById('modalNuevo'));
const modalPassword=()=>bootstrap.Modal.getOrCreateInstance(document.getElementById('modalPassword'));

function bloqueado(u){return Boolean(u.bloqueadoHasta && new Date(u.bloqueadoHasta)>new Date())}
function estadoUsuario(u){if(!u.activo)return 'INACTIVO';if(bloqueado(u))return 'BLOQUEADO';return 'ACTIVO'}
function rolTexto(r){return ({ADMINISTRADOR:'Administrador',CAJERO:'Cajero',COCINERO:'Cocinero'})[r]||r}
function ultimoAcceso(v){return v?fecha(v):'Nunca'}
function esActual(u){return Boolean(USUARIO_ACTUAL && u.nombreUsuario.toLowerCase()===String(USUARIO_ACTUAL.usuario||'').toLowerCase())}
function estadoBadge(u){
  const estado=estadoUsuario(u);
  if(estado==='INACTIVO')return '<span class="badge text-bg-secondary">Inactivo</span>';
  if(estado==='BLOQUEADO')return '<span class="badge text-bg-danger">Bloqueado</span>';
  return '<span class="badge text-bg-success">Activo</span>';
}

async function cargarUsuarios(){
  try{
    if(!USUARIO_ACTUAL) USUARIO_ACTUAL=await api('/api/auth/me');
    USUARIOS=await api('/api/usuarios');
    actualizarKpis();
    renderUsuarios();
  }catch(e){showError(e)}
}

function actualizarKpis(){
  kpiTotal.textContent=USUARIOS.length;
  kpiActivos.textContent=USUARIOS.filter(u=>estadoUsuario(u)==='ACTIVO').length;
  kpiBloqueados.textContent=USUARIOS.filter(u=>estadoUsuario(u)==='BLOQUEADO').length;
  kpiInactivos.textContent=USUARIOS.filter(u=>estadoUsuario(u)==='INACTIVO').length;
}

function usuariosFiltrados(){
  const q=buscarUsuario.value.trim().toLowerCase();
  const rol=filtroRol.value;
  const estado=filtroEstado.value;
  return USUARIOS.filter(u=>{
    const coincideTexto=!q || String(u.nombreUsuario||'').toLowerCase().includes(q) || String(u.nombre||'').toLowerCase().includes(q);
    const coincideRol=!rol || u.rol===rol;
    const coincideEstado=!estado || estadoUsuario(u)===estado;
    return coincideTexto && coincideRol && coincideEstado;
  });
}

function botonesAccion(u){
  if(esActual(u)){
    return `<div class="user-actions"><a class="btn btn-sm btn-outline-dark" href="/cambiar-password.html">Cambiar mi contraseña</a><span class="badge rounded-pill text-bg-light border align-self-center">Tu cuenta</span></div>`;
  }
  const unlock=bloqueado(u)?`<button class="btn btn-sm btn-outline-warning" onclick="desbloquear(${u.idUsuario})">Desbloquear</button>`:'';
  const state=`<button class="btn btn-sm ${u.activo?'btn-outline-danger':'btn-outline-success'}" onclick="cambiarEstado(${u.idUsuario},${!u.activo})">${u.activo?'Desactivar':'Activar'}</button>`;
  return `<div class="user-actions"><button class="btn btn-sm btn-outline-dark" onclick="abrirPassword(${u.idUsuario})">Restablecer contraseña</button>${unlock}${state}</div>`;
}

function renderUsuarios(){
  const lista=usuariosFiltrados();
  resultadoUsuarios.textContent=`${lista.length} ${lista.length===1?'resultado':'resultados'}`;
  if(!lista.length){
    usuariosTabla.innerHTML='<div class="empty-state py-5"><div class="fs-2 mb-2">⌕</div><strong>No se encontraron usuarios</strong><div class="small mt-1">Prueba con otros filtros o términos de búsqueda.</div></div>';
    return;
  }
  usuariosTabla.innerHTML=`<table class="table table-hover table-app align-middle mb-0 users-table"><thead><tr><th>Usuario</th><th>Nombre</th><th>Rol</th><th>Estado</th><th>Último acceso</th><th class="text-end">Acciones</th></tr></thead><tbody>${lista.map(u=>`<tr>
    <td><div class="fw-bold">${escapeHtml(u.nombreUsuario)}</div>${u.debeCambiarPassword?'<span class="badge user-password-badge mt-1">Cambio de clave pendiente</span>':''}</td>
    <td>${escapeHtml(u.nombre||'—')}</td>
    <td><span class="user-role-pill">${rolTexto(u.rol)}</span></td>
    <td>${estadoBadge(u)}</td>
    <td>${ultimoAcceso(u.ultimoAcceso)}</td>
    <td class="text-end">${botonesAccion(u)}</td>
  </tr>`).join('')}</tbody></table>`;
}

nuevoUsuarioForm.addEventListener('submit',async e=>{
  e.preventDefault();
  if(nuevoPassword.value!==nuevoPasswordConfirm.value){showToast('Las contraseñas no coinciden','error');nuevoPasswordConfirm.focus();return}
  try{
    await api('/api/usuarios',{method:'POST',body:JSON.stringify({nombreUsuario:nuevoLogin.value.trim(),nombre:nuevoNombre.value.trim(),rol:nuevoRol.value,passwordTemporal:nuevoPassword.value})});
    showToast('Usuario creado correctamente');
    nuevoUsuarioForm.reset();modalNuevo().hide();await cargarUsuarios();
  }catch(e){showError(e)}
});

function abrirPassword(id){
  const u=USUARIOS.find(x=>x.idUsuario===id);if(!u)return;
  if(esActual(u)){location='/cambiar-password.html';return}
  passwordId.value=id;passwordUsuario.textContent=`${u.nombreUsuario} · ${u.nombre||rolTexto(u.rol)}`;passwordTemporal.value='';passwordTemporalConfirm.value='';modalPassword().show();setTimeout(()=>passwordTemporal.focus(),200)
}

passwordForm.addEventListener('submit',async e=>{
  e.preventDefault();
  if(passwordTemporal.value!==passwordTemporalConfirm.value){showToast('Las contraseñas no coinciden','error');passwordTemporalConfirm.focus();return}
  try{
    await api(`/api/usuarios/${passwordId.value}/restablecer-password`,{method:'POST',body:JSON.stringify({passwordTemporal:passwordTemporal.value})});
    showToast('Contraseña temporal asignada');modalPassword().hide();await cargarUsuarios();
  }catch(e){showError(e)}
});

async function desbloquear(id){
  const u=USUARIOS.find(x=>x.idUsuario===id);if(!u)return;
  if(!confirm(`¿Desbloquear la cuenta ${u.nombreUsuario}?`))return;
  try{await api(`/api/usuarios/${id}/desbloquear`,{method:'POST'});showToast('Usuario desbloqueado');await cargarUsuarios()}catch(e){showError(e)}
}

async function cambiarEstado(id,activo){
  const u=USUARIOS.find(x=>x.idUsuario===id);if(!u)return;
  if(esActual(u) && !activo){showToast('No puedes desactivar tu propia cuenta','error');return}
  const accion=activo?'activar':'desactivar';
  if(!confirm(`¿Seguro que deseas ${accion} la cuenta ${u.nombreUsuario}?`))return;
  try{await api(`/api/usuarios/${id}/estado`,{method:'POST',body:JSON.stringify({activo})});showToast(activo?'Usuario activado':'Usuario desactivado');await cargarUsuarios()}catch(e){showError(e)}
}

function aplicarFiltros(){renderUsuarios()}
buscarUsuario.addEventListener('input',aplicarFiltros);
filtroRol.addEventListener('change',aplicarFiltros);
filtroEstado.addEventListener('change',aplicarFiltros);
limpiarFiltros.addEventListener('click',()=>{buscarUsuario.value='';filtroRol.value='';filtroEstado.value='';renderUsuarios();buscarUsuario.focus()});

document.addEventListener('DOMContentLoaded',cargarUsuarios);
