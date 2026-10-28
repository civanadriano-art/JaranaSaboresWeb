let CURRENT_USER=null;

const PAGE_ROLES={
  'pedidos.html':['CAJERO','ADMINISTRADOR'],
  'pagos.html':['CAJERO','ADMINISTRADOR'],
  'cocina.html':['COCINERO','ADMINISTRADOR'],
  'catalogo.html':['ADMINISTRADOR'],
  'historial.html':['ADMINISTRADOR'],
  'reportes.html':['ADMINISTRADOR'],
  'auditoria.html':['ADMINISTRADOR'],
  'usuarios.html':['ADMINISTRADOR'],
  'backup.html':['ADMINISTRADOR']
};

function pageName(){return location.pathname.split('/').pop()||'dashboard.html'}

async function initShell(){
  try{
    CURRENT_USER=await api('/api/auth/me');
  }catch{
    location='/login.html';
    return;
  }

  const page=pageName();
  if(CURRENT_USER.debeCambiarPassword && page!=='cambiar-password.html'){
    location='/cambiar-password.html';
    return;
  }

  const allowedPageRoles=PAGE_ROLES[page];
  if(allowedPageRoles && !allowedPageRoles.includes(CURRENT_USER.rol)){
    location='/dashboard.html';
    return;
  }

  document.querySelectorAll('[data-user-name]').forEach(x=>x.textContent=CURRENT_USER.nombre||CURRENT_USER.usuario);
  document.querySelectorAll('[data-user-role]').forEach(x=>x.textContent=(CURRENT_USER.rol||'')
    .replace('ADMINISTRADOR','Administrador')
    .replace('CAJERO','Caja')
    .replace('COCINERO','Cocina'));
  document.querySelectorAll('.user-avatar').forEach(x=>x.textContent=(CURRENT_USER.nombre||CURRENT_USER.usuario||'U').trim().charAt(0).toUpperCase());

  document.querySelectorAll('[data-role]').forEach(x=>{
    const allowed=x.dataset.role.split(',').map(r=>r.trim());
    const visible=allowed.includes(CURRENT_USER.rol);
    x.classList.toggle('role-visible',visible);
    x.classList.toggle('d-none',!visible);
  });

  document.querySelectorAll('.nav-link-app').forEach(a=>{
    if(a.getAttribute('href')?.endsWith(page))a.classList.add('active');
  });

  document.querySelector('.app-shell')?.classList.add('shell-ready');
  document.dispatchEvent(new CustomEvent('app:user-ready',{detail:CURRENT_USER}));
}

function ensureSidebarBackdrop(){
  let b=document.querySelector('.sidebar-backdrop');
  if(!b){
    b=document.createElement('div');
    b.className='sidebar-backdrop';
    b.addEventListener('click',closeSidebar);
    document.body.appendChild(b);
  }
  return b;
}
function openSidebar(){document.querySelector('.sidebar')?.classList.add('open');ensureSidebarBackdrop().classList.add('show')}
function closeSidebar(){document.querySelector('.sidebar')?.classList.remove('open');document.querySelector('.sidebar-backdrop')?.classList.remove('show')}
function toggleSidebar(){document.querySelector('.sidebar')?.classList.contains('open')?closeSidebar():openSidebar()}



// Cuadros propios del sistema: reemplazan alert(), confirm() y prompt().
function systemDialog(options={}){
  const {
    title='Confirmar acción',
    message='',
    note='',
    confirmText='Confirmar',
    cancelText='Cancelar',
    tone='danger',
    icon='!',
    input=null
  }=options;

  return new Promise(resolve=>{
    const id='systemDialog_'+Date.now()+'_'+Math.random().toString(16).slice(2);
    const toneClass={danger:'danger',warning:'warning',success:'success',info:'info'}[tone]||'danger';
    const inputHtml=input ? (input.multiline!==false
      ? `<div class="text-start mt-3"><label class="form-label" for="${id}_field">${escapeHtml(input.label||'Detalle')}</label><textarea id="${id}_field" class="form-control system-dialog-field" rows="3" maxlength="${Number(input.maxLength||250)}" placeholder="${escapeHtml(input.placeholder||'')}">${escapeHtml(input.value||'')}</textarea><div class="invalid-feedback">${escapeHtml(input.requiredMessage||'Completa este campo para continuar.')}</div></div>`
      : `<div class="text-start mt-3"><label class="form-label" for="${id}_field">${escapeHtml(input.label||'Detalle')}</label><input id="${id}_field" class="form-control system-dialog-field" type="${escapeHtml(input.type||'text')}" maxlength="${Number(input.maxLength||120)}" placeholder="${escapeHtml(input.placeholder||'')}" value="${escapeHtml(input.value||'')}"><div class="invalid-feedback">${escapeHtml(input.requiredMessage||'Completa este campo para continuar.')}</div></div>`
    ) : '';

    const wrapper=document.createElement('div');
    wrapper.innerHTML=`<div class="modal fade system-dialog-modal" id="${id}" tabindex="-1" aria-hidden="true">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content system-dialog-card tone-${toneClass}">
          <div class="modal-body p-4 text-center">
            <div class="system-dialog-icon tone-${toneClass}">${escapeHtml(icon)}</div>
            <h5 class="system-dialog-title">${escapeHtml(title)}</h5>
            ${message?`<p class="system-dialog-message">${escapeHtml(message)}</p>`:''}
            ${note?`<div class="system-dialog-note">${escapeHtml(note)}</div>`:''}
            ${inputHtml}
          </div>
          <div class="modal-footer system-dialog-actions border-0 pt-0 px-4 pb-4">
            <button type="button" class="btn btn-outline-secondary flex-fill" data-bs-dismiss="modal">${escapeHtml(cancelText)}</button>
            <button type="button" class="btn system-dialog-confirm flex-fill">${escapeHtml(confirmText)}</button>
          </div>
        </div>
      </div>
    </div>`;
    const modalEl=wrapper.firstElementChild;
    document.body.appendChild(modalEl);
    const modal=bootstrap.Modal.getOrCreateInstance(modalEl,{backdrop:'static',keyboard:true});
    const confirmBtn=modalEl.querySelector('.system-dialog-confirm');
    confirmBtn.classList.add(toneClass==='danger'?'btn-danger':toneClass==='success'?'btn-success':toneClass==='warning'?'btn-warning':'btn-primary');
    const field=modalEl.querySelector('.system-dialog-field');
    let settled=false;

    const finish=value=>{
      if(settled)return;
      settled=true;
      resolve(value);
      modal.hide();
    };

    confirmBtn.addEventListener('click',()=>{
      if(field){
        const value=field.value.trim();
        if(input.required!==false && !value){field.classList.add('is-invalid');field.focus();return}
        finish(value);
      }else finish(true);
    });
    field?.addEventListener('input',()=>field.classList.remove('is-invalid'));
    modalEl.addEventListener('hidden.bs.modal',()=>{
      if(!settled){settled=true;resolve(input?null:false)}
      modalEl.remove();
    },{once:true});
    modalEl.addEventListener('shown.bs.modal',()=>field?.focus(),{once:true});
    modal.show();
  });
}

function appConfirm(options){return systemDialog({...options,input:null})}
function appPrompt(options){return systemDialog({...options,input:{multiline:true,required:true,...(options?.input||{})}})}

document.addEventListener('DOMContentLoaded',()=>{
  initShell();
  ensureSidebarBackdrop();
  document.querySelectorAll('.sidebar .nav-link-app').forEach(a=>a.addEventListener('click',()=>{if(window.innerWidth<992)closeSidebar()}));
});
window.addEventListener('resize',()=>{if(window.innerWidth>=992)closeSidebar()});
