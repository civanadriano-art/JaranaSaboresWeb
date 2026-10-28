let BACKUP_DISPONIBLE = false;

function formatoBytes(bytes){
  const n=Number(bytes||0);
  if(n<1024)return `${n} B`;
  if(n<1024*1024)return `${(n/1024).toFixed(1)} KB`;
  return `${(n/(1024*1024)).toFixed(1)} MB`;
}

async function cargarEstadoBackup(){
  try{
    const e=await api('/api/backups/estado');
    BACKUP_DISPONIBLE=!!e.disponible;
    const btn=document.getElementById('btnRestaurar');
    if(btn)btn.disabled=!BACKUP_DISPONIBLE;
    document.getElementById('backupArchivo').textContent=e.disponible?e.archivo:'Todavía no hay copias creadas';
    document.getElementById('backupDetalle').textContent=e.disponible
      ? `${fecha(e.fechaModificacion)} · ${formatoBytes(e.tamanoBytes)}`
      : 'Crea la primera copia para habilitar la restauración.';
  }catch(e){showError(e)}
}

async function crearBackup(){
  const btn=document.getElementById('btnCrearBackup');
  const original=btn.innerHTML;
  btn.disabled=true;btn.innerHTML='<span class="spinner-border spinner-border-sm me-2"></span>Creando copia...';
  try{
    const r=await api('/api/backups',{method:'POST'});
    showToast(`${r.mensaje}: ${r.archivo}`);
    await cargarEstadoBackup();
  }catch(e){showError(e)}
  finally{btn.disabled=false;btn.innerHTML=original}
}

async function restaurarUltimoBackup(){
  if(!BACKUP_DISPONIBLE)return;
  const ok=confirm('Se restaurará la última copia de seguridad y los datos actuales volverán al estado guardado. Antes de restaurar se creará automáticamente una copia preventiva. ¿Deseas continuar?');
  if(!ok)return;
  const btn=document.getElementById('btnRestaurar');
  const original=btn.innerHTML;
  btn.disabled=true;btn.innerHTML='<span class="spinner-border spinner-border-sm me-2"></span>Restaurando...';
  try{
    const r=await api('/api/backups/restaurar-ultimo',{method:'POST'});
    showToast(`${r.mensaje}. Copia preventiva: ${r.backupPreventivo}`,'warning');
    await cargarEstadoBackup();
  }catch(e){showError(e)}
  finally{btn.disabled=!BACKUP_DISPONIBLE;btn.innerHTML=original}
}

document.addEventListener('DOMContentLoaded',cargarEstadoBackup);
