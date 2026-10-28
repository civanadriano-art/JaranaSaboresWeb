let USUARIOS=[];
const modalNuevo=()=>bootstrap.Modal.getOrCreateInstance(document.getElementById('modalNuevo'));
const modalPassword=()=>bootstrap.Modal.getOrCreateInstance(document.getElementById('modalPassword'));
function bloqueado(u){return u.bloqueadoHasta && new Date(u.bloqueadoHasta)>new Date()}
function rolTexto(r){return ({ADMINISTRADOR:'Administrador',CAJERO:'Cajero',COCINERO:'Cocinero'})[r]||r}
async function cargarUsuarios(){
  try{
    USUARIOS=await api('/api/usuarios');
    kpiTotal.textContent=USUARIOS.length;kpiActivos.textContent=USUARIOS.filter(x=>x.activo).length;kpiBloqueados.textContent=USUARIOS.filter(bloqueado).length;
    usuariosTabla.innerHTML=`<table class="table table-hover table-app align-middle mb-0"><thead><tr><th>Usuario</th><th>Nombre</th><th>Rol</th><th>Estado</th><th>Último acceso</th><th class="text-end">Acciones</th></tr></thead><tbody>${USUARIOS.map(u=>`<tr><td><strong>${escapeHtml(u.nombreUsuario)}</strong>${u.debeCambiarPassword?'<div><span class="badge text-bg-warning mt-1">Cambio de clave pendiente</span></div>':''}</td><td>${escapeHtml(u.nombre||'—')}</td><td>${rolTexto(u.rol)}</td><td>${!u.activo?'<span class="badge text-bg-secondary">Inactivo</span>':bloqueado(u)?'<span class="badge text-bg-danger">Bloqueado</span>':'<span class="badge text-bg-success">Activo</span>'}</td><td>${fecha(u.ultimoAcceso)}</td><td class="text-end"><div class="btn-group btn-group-sm"><button class="btn btn-outline-dark" onclick="abrirPassword(${u.idUsuario})">Contraseña</button>${bloqueado(u)?`<button class="btn btn-outline-warning" onclick="desbloquear(${u.idUsuario})">Desbloquear</button>`:''}<button class="btn ${u.activo?'btn-outline-danger':'btn-outline-success'}" onclick="cambiarEstado(${u.idUsuario},${!u.activo})">${u.activo?'Desactivar':'Activar'}</button></div></td></tr>`).join('')}</tbody></table>`;
  }catch(e){showError(e)}
}
nuevoUsuarioForm.addEventListener('submit',async e=>{e.preventDefault();try{await api('/api/usuarios',{method:'POST',body:JSON.stringify({nombreUsuario:nuevoLogin.value.trim(),nombre:nuevoNombre.value.trim(),rol:nuevoRol.value,passwordTemporal:nuevoPassword.value})});showToast('Usuario creado correctamente');nuevoUsuarioForm.reset();modalNuevo().hide();await cargarUsuarios()}catch(e){showError(e)}});
function abrirPassword(id){const u=USUARIOS.find(x=>x.idUsuario===id);if(!u)return;passwordId.value=id;passwordUsuario.textContent=u.nombreUsuario;passwordTemporal.value='';modalPassword().show();setTimeout(()=>passwordTemporal.focus(),200)}
passwordForm.addEventListener('submit',async e=>{e.preventDefault();try{await api(`/api/usuarios/${passwordId.value}/restablecer-password`,{method:'POST',body:JSON.stringify({passwordTemporal:passwordTemporal.value})});showToast('Contraseña temporal asignada');modalPassword().hide();await cargarUsuarios()}catch(e){showError(e)}});
async function desbloquear(id){try{await api(`/api/usuarios/${id}/desbloquear`,{method:'POST'});showToast('Usuario desbloqueado');await cargarUsuarios()}catch(e){showError(e)}}
async function cambiarEstado(id,activo){const u=USUARIOS.find(x=>x.idUsuario===id);if(!confirm(`${activo?'Activar':'Desactivar'} la cuenta ${u?.nombreUsuario||''}?`))return;try{await api(`/api/usuarios/${id}/estado`,{method:'POST',body:JSON.stringify({activo})});showToast(activo?'Usuario activado':'Usuario desactivado');await cargarUsuarios()}catch(e){showError(e)}}
document.addEventListener('DOMContentLoaded',cargarUsuarios);
