let kitchenOrders=[];
let kitchenTimer=null;
let kitchenDishes=[];


async function cargarDisponibilidadPlatos(){
  try{
    kitchenDishes=await api('/api/platos');
    renderDisponibilidadPlatos();
  }catch(e){showError(e)}
}

function renderDisponibilidadPlatos(){
  const q=(document.getElementById('dishAvailabilitySearch')?.value||'').trim().toLowerCase();
  const disponibles=kitchenDishes.filter(p=>p.disponible!==false).length;
  const agotados=kitchenDishes.filter(p=>p.disponible===false).length;
  const countA=document.getElementById('dishAvailableCount');
  const countB=document.getElementById('dishSoldOutCount');
  if(countA)countA.textContent=disponibles;
  if(countB)countB.textContent=agotados;
  const filtrados=kitchenDishes.filter(p=>!q||`${p.nombre||''} ${p.categoria||''} ${p.codigo||''}`.toLowerCase().includes(q));
  const box=document.getElementById('dishAvailabilityList');
  if(!box)return;
  if(!filtrados.length){
    box.innerHTML='<div class="availability-empty">No hay platos que coincidan con la búsqueda.</div>';
    return;
  }
  box.innerHTML=`<div class="availability-list">${filtrados.map(p=>{
    const disponible=p.disponible!==false;
    return `<div class="availability-row ${disponible?'':'is-soldout'}">
      <div class="availability-dish">
        <strong>${escapeHtml(p.nombre)}</strong>
        <span>${escapeHtml(p.categoria||'Sin categoría')} · ${money(p.precio)}</span>
      </div>
      <div class="availability-actions">
        <span class="availability-badge ${disponible?'available':'soldout'}">${disponible?'Disponible':'Agotado'}</span>
        <button class="btn btn-sm ${disponible?'btn-outline-danger':'btn-outline-success'}" onclick="cambiarDisponibilidadPlato(${p.codigo},${!disponible})">${disponible?'Marcar agotado':'Marcar disponible'}</button>
      </div>
    </div>`;
  }).join('')}</div>`;
}

let pendingDishAvailability=null;

function cambiarDisponibilidadPlato(codigo,nuevaDisponibilidad){
  const plato=kitchenDishes.find(p=>Number(p.codigo)===Number(codigo));
  if(!plato)return;
  pendingDishAvailability={codigo,nuevaDisponibilidad,nombre:plato.nombre};

  const title=document.getElementById('dishConfirmTitle');
  const message=document.getElementById('dishConfirmMessage');
  const note=document.getElementById('dishConfirmNote');
  const icon=document.getElementById('dishConfirmIcon');
  const action=document.getElementById('dishConfirmAction');

  if(nuevaDisponibilidad){
    title.textContent=`Marcar ${plato.nombre} como disponible`;
    message.textContent='El plato volverá a estar disponible para nuevos pedidos de Caja.';
    note.textContent='Puedes volver a marcarlo como agotado cuando sea necesario.';
    icon.className='dish-confirm-icon available';
    icon.textContent='✓';
    action.className='btn btn-success flex-fill';
    action.textContent='Marcar disponible';
  }else{
    title.textContent=`Marcar ${plato.nombre} como agotado`;
    message.textContent='El plato dejará de estar disponible para nuevos pedidos de Caja.';
    note.textContent='Los pedidos ya registrados no se modificarán.';
    icon.className='dish-confirm-icon soldout';
    icon.textContent='!';
    action.className='btn btn-danger flex-fill';
    action.textContent='Marcar agotado';
  }

  bootstrap.Modal.getOrCreateInstance(document.getElementById('dishConfirmModal')).show();
}

async function confirmarDisponibilidadPlato(){
  if(!pendingDishAvailability)return;
  const {codigo,nuevaDisponibilidad}=pendingDishAvailability;
  const action=document.getElementById('dishConfirmAction');
  const original=action.textContent;
  action.disabled=true;
  action.textContent='Guardando...';
  try{
    await api(`/api/platos/${codigo}/disponibilidad?disponible=${nuevaDisponibilidad}`,{method:'PATCH'});
    bootstrap.Modal.getInstance(document.getElementById('dishConfirmModal'))?.hide();
    showToast(nuevaDisponibilidad?'Plato disponible para nuevos pedidos':'Plato marcado como agotado');
    pendingDishAvailability=null;
    await cargarDisponibilidadPlatos();
  }catch(e){
    showError(e);
  }finally{
    action.disabled=false;
    action.textContent=original;
  }
}

function kitchenState(p){
  const e=String(p.estado||'').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'');
  if(e==='pendiente')return 'PENDIENTE';
  if(e.includes('prepar'))return 'EN_PREPARACION';
  if(e==='listo')return 'LISTO';
  return e.toUpperCase();
}

function minutesSince(value){
  if(!value)return 0;
  const ms=Date.now()-new Date(value).getTime();
  return Math.max(0,Math.floor(ms/60000));
}

function elapsedText(p){
  const st=kitchenState(p);
  if(st==='PENDIENTE'){
    const m=minutesSince(p.fechaRegistro);
    return {label:m===0?'Recién recibido':`Esperando ${m} min`,tone:'wait'};
  }
  if(st==='EN_PREPARACION'){
    const m=minutesSince(p.fechaInicioPreparacion||p.fechaRegistro);
    return {label:m===0?'Preparación iniciada':`Preparando ${m} min`,tone:'prep'};
  }
  const m=minutesSince(p.fechaListo||p.fechaInicioPreparacion||p.fechaRegistro);
  return {label:m===0?'Recién terminado':`Listo hace ${m} min`,tone:'ready'};
}

function itemCount(p){return (p.detalles||[]).reduce((n,d)=>n+Number(d.cantidad||0),0)}
function hasNotes(p){return (p.detalles||[]).some(d=>String(d.observacion||'').trim()||Number(d.costoExtra||0)>0)}

function kitchenCard(p){
  const state=kitchenState(p);
  const elapsed=elapsedText(p);
  const count=itemCount(p);
  const details=(p.detalles||[]).map(d=>{
    const obs=String(d.observacion||'').trim();
    const extra=Number(d.costoExtra||0);
    return `<li class="kitchen-item">
      <div class="kitchen-item-main"><span class="kitchen-qty">${Number(d.cantidad||0)}×</span><strong>${escapeHtml(d.plato)}</strong></div>
      ${obs?`<div class="kitchen-note"><span>!</span><div><small>Observación</small>${escapeHtml(obs)}</div></div>`:''}
      ${extra>0?`<div class="kitchen-extra">+ Extra solicitado · ${money(extra)}</div>`:''}
    </li>`;
  }).join('');

  const action=state==='PENDIENTE'
    ? `<button class="btn btn-warning kitchen-action" onclick="accion(${p.numero},'preparar')">♨ Iniciar preparación</button>`
    : state==='EN_PREPARACION'
      ? `<button class="btn btn-success kitchen-action" onclick="accion(${p.numero},'listo')">✓ Marcar como listo</button>`
      : `<div class="kitchen-ready-message"><span>✓</span><div><strong>Preparación terminada</strong><small>Esperando retiro de caja</small></div></div>`;

  return `<article class="order-card kitchen-order-card state-${state.toLowerCase()}">
    <div class="kitchen-card-head">
      <div>
        <div class="d-flex align-items-center gap-2 flex-wrap"><h5 class="mb-0">#${p.numero}</h5><span class="kitchen-elapsed ${elapsed.tone}">${elapsed.label}</span></div>
        <div class="kitchen-customer">${escapeHtml(p.cliente)}</div>
        <small class="text-muted">Recibido ${new Date(p.fechaRegistro).toLocaleTimeString('es-PE',{hour:'2-digit',minute:'2-digit'})}</small>
      </div>
      <div class="kitchen-item-count"><strong>${count}</strong><span>${count===1?'plato':'platos'}</span></div>
    </div>
    <ul class="kitchen-items">${details}</ul>
    <div class="kitchen-card-footer">${action}</div>
  </article>`;
}

function kitchenEmpty(title,text,icon){
  return `<div class="kitchen-empty"><span>${icon}</span><strong>${title}</strong><small>${text}</small></div>`;
}

function renderKitchen(){
  const q=(document.getElementById('kitchenSearch')?.value||'').trim().toLowerCase();
  const view=document.getElementById('kitchenView')?.value||'TODOS';
  const matches=p=>{
    if(view!=='TODOS'&&kitchenState(p)!==view)return false;
    if(!q)return true;
    const haystack=[p.numero,p.cliente,p.telefono,...(p.detalles||[]).flatMap(d=>[d.plato,d.observacion])].join(' ').toLowerCase();
    return haystack.includes(q);
  };

  const visible=kitchenOrders.filter(matches);
  const pend=visible.filter(p=>kitchenState(p)==='PENDIENTE').sort((a,b)=>a.numero-b.numero);
  const prep=visible.filter(p=>kitchenState(p)==='EN_PREPARACION').sort((a,b)=>a.numero-b.numero);
  const ready=visible.filter(p=>kitchenState(p)==='LISTO').sort((a,b)=>a.numero-b.numero);

  document.getElementById('countPend').textContent=pend.length;
  document.getElementById('countPrep').textContent=prep.length;
  document.getElementById('countListo').textContent=ready.length;
  document.getElementById('colPend').innerHTML=pend.map(kitchenCard).join('')||kitchenEmpty('Sin pedidos pendientes','La cola por iniciar está vacía.','✓');
  document.getElementById('colPrep').innerHTML=prep.map(kitchenCard).join('')||kitchenEmpty('Nada en preparación','No hay pedidos cocinándose ahora.','♨');
  document.getElementById('colListo').innerHTML=ready.map(kitchenCard).join('')||kitchenEmpty('Sin pedidos listos','Los pedidos terminados aparecerán aquí.','✓');

  document.querySelectorAll('.kitchen-column-wrap').forEach(col=>{
    const state=col.dataset.column;
    col.classList.toggle('d-none',view!=='TODOS'&&view!==state);
  });
}

function renderKitchenKpis(){
  const pend=kitchenOrders.filter(p=>kitchenState(p)==='PENDIENTE');
  const prep=kitchenOrders.filter(p=>kitchenState(p)==='EN_PREPARACION');
  const ready=kitchenOrders.filter(p=>kitchenState(p)==='LISTO');
  const dishes=[...pend,...prep].reduce((n,p)=>n+itemCount(p),0);
  const notes=[...pend,...prep].filter(hasNotes).length;
  document.getElementById('kitchenKpis').innerHTML=`
    <div class="col-sm-6 col-xl-3"><div class="kpi-card kitchen-kpi"><div><div class="kpi-label">Por iniciar</div><div class="kpi-value">${pend.length}</div><div class="kpi-helper">Pedidos pendientes</div></div><div class="kpi-icon">⌛</div></div></div>
    <div class="col-sm-6 col-xl-3"><div class="kpi-card kitchen-kpi"><div><div class="kpi-label">En preparación</div><div class="kpi-value">${prep.length}</div><div class="kpi-helper">Trabajándose ahora</div></div><div class="kpi-icon">♨</div></div></div>
    <div class="col-sm-6 col-xl-3"><div class="kpi-card kitchen-kpi"><div><div class="kpi-label">Listos</div><div class="kpi-value">${ready.length}</div><div class="kpi-helper">Esperando retiro</div></div><div class="kpi-icon">✓</div></div></div>
    <div class="col-sm-6 col-xl-3"><div class="kpi-card kitchen-kpi"><div><div class="kpi-label">Platos en cola</div><div class="kpi-value">${dishes}</div><div class="kpi-helper">${notes} pedido${notes===1?'':'s'} con indicaciones</div></div><div class="kpi-icon">☷</div></div></div>`;
}

async function cargarCocina(manual=false){
  try{
    kitchenOrders=await api('/api/pedidos');
    renderKitchenKpis();
    renderKitchen();
    document.getElementById('kitchenUpdated').textContent=new Date().toLocaleTimeString('es-PE',{hour:'2-digit',minute:'2-digit'});
    if(manual)showToast('Panel de cocina actualizado');
  }catch(e){showError(e)}
}

async function accion(numero,accionPedido){
  try{
    await api(`/api/pedidos/${numero}/${accionPedido}`,{method:'POST'});
    showToast(accionPedido==='preparar'?'Preparación iniciada':'Pedido marcado como listo');
    await cargarCocina();
  }catch(e){showError(e)}
}

function iniciarCocina(){
  document.getElementById('kitchenSearch')?.addEventListener('input',renderKitchen);
  document.getElementById('kitchenView')?.addEventListener('change',renderKitchen);
  document.getElementById('dishAvailabilitySearch')?.addEventListener('input',renderDisponibilidadPlatos);
  document.getElementById('dishConfirmAction')?.addEventListener('click',confirmarDisponibilidadPlato);
  document.getElementById('dishConfirmModal')?.addEventListener('hidden.bs.modal',()=>{pendingDishAvailability=null});
  if(kitchenTimer)clearInterval(kitchenTimer);
  cargarCocina();
  kitchenTimer=setInterval(()=>cargarCocina(),10000);
}

document.addEventListener('app:user-ready',iniciarCocina);
