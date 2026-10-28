let dashboardTimer=null;

function localIsoDate(){
  const d=new Date();
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`;
}

function normalizarEstado(e){return String(e||'').toLowerCase().normalize('NFD').replace(/[\u0300-\u036f]/g,'')}
function esEstado(p,nombre){return normalizarEstado(p.estado)===normalizarEstado(nombre)}
function minsFrom(v){return v?Math.max(0,Math.floor((Date.now()-new Date(v).getTime())/60000)):0}
function totalPlatos(p){return (p.detalles||[]).reduce((n,d)=>n+Number(d.cantidad||0),0)}

function configurarCabeceraPorRol(rol){
  const cfg={
    CAJERO:{eyebrow:'Operación de caja',title:'Panel de caja',subtitle:'Pedidos, cobros y entregas que requieren atención.',role:'Caja',orders:'Pedidos para caja',ordersSub:'Prioriza pedidos listos y pendientes de cobro.'},
    COCINERO:{eyebrow:'Operación de cocina',title:'Inicio de cocina',subtitle:'Visualiza la carga actual y entra rápidamente a la preparación.',role:'Cocina',orders:'Prioridad de cocina',ordersSub:'Pedidos ordenados para atender primero los que llevan más tiempo.'},
    ADMINISTRADOR:{eyebrow:'Vista general',title:'Panel administrativo',subtitle:'Resumen operativo y comercial del restaurante.',role:'Administrador',orders:'Pedidos activos',ordersSub:'Seguimiento general de la operación en tiempo real.'}
  }[rol]||{eyebrow:'Panel operativo',title:'Panel principal',subtitle:'Resumen de la operación del restaurante.',role:'Usuario',orders:'Pedidos activos',ordersSub:'Pedidos que requieren atención.'};
  dashboardEyebrow.textContent=cfg.eyebrow;
  dashboardTitle.textContent=cfg.title;
  dashboardSubtitle.textContent=cfg.subtitle;
  dashboardRole.textContent=cfg.role;
  ordersTitle.textContent=cfg.orders;
  ordersSubtitle.textContent=cfg.ordersSub;
}

function tarjetaKpi(label,value,icon,helper=''){
  return `<div class="col-sm-6 col-xl-3"><div class="kpi-card dashboard-kpi"><div class="kpi-head"><div><div class="kpi-label">${label}</div><div class="kpi-value">${value}</div>${helper?`<div class="kpi-helper">${helper}</div>`:''}</div><div class="kpi-icon">${icon}</div></div></div></div>`;
}

function prioridadPorRol(rol,{pend,prep,listos,sinCobrar,listosSinCobrar}){
  if(rol==='CAJERO'){
    if(listosSinCobrar>0)return ['Cobrar pedidos listos',`${listosSinCobrar} pedido${listosSinCobrar===1?' está':'s están'} listo${listosSinCobrar===1?'':'s'} y pendiente${listosSinCobrar===1?'':'s'} de cobro.`,'S/'];
    if(sinCobrar>0)return ['Cobros pendientes',`${sinCobrar} pedido${sinCobrar===1?'':'s'} activo${sinCobrar===1?'':'s'} aún no ${sinCobrar===1?'tiene':'tienen'} pago registrado.`,'!'];
    return ['Caja al día','No hay pedidos activos pendientes de cobro.','✓'];
  }
  if(rol==='COCINERO'){
    if(pend>0)return ['Primero: pedidos pendientes',`${pend} pedido${pend===1?'':'s'} espera${pend===1?'':'n'} el inicio de preparación. Abre Cocina para atenderlos en orden.`,'♨'];
    if(prep>0)return ['Preparación en curso',`${prep} pedido${prep===1?'':'s'} ${prep===1?'está':'están'} actualmente en cocina.`,'…'];
    return ['Cocina al día',listos>0?`${listos} pedido${listos===1?' está':'s están'} listo${listos===1?'':'s'} y esperando retiro de caja.`:'No hay pedidos esperando preparación.','✓'];
  }
  if(pend+prep>0)return ['Operación en curso',`${pend} pendientes y ${prep} en preparación.`,'▦'];
  return ['Operación estable',listos>0?`${listos} pedido${listos===1?'':'s'} listo${listos===1?'':'s'} para completar su atención.`:'No hay pedidos pendientes de preparación.','✓'];
}

function ordenarPedidos(pedidos,rol,pagados){
  const prioridad=p=>{
    if(rol==='CAJERO'){
      if(esEstado(p,'Listo')&&!pagados.has(p.numero))return 0;
      if(!pagados.has(p.numero))return 1;
      if(esEstado(p,'Listo'))return 2;
      return 3;
    }
    if(rol==='COCINERO'){
      if(esEstado(p,'Pendiente'))return 0;
      if(esEstado(p,'En Preparación'))return 1;
      return 2;
    }
    if(esEstado(p,'Pendiente'))return 0;
    if(esEstado(p,'En Preparación'))return 1;
    return 2;
  };
  return [...pedidos].sort((a,b)=>prioridad(a)-prioridad(b)||a.numero-b.numero);
}

function kitchenElapsed(p){
  if(esEstado(p,'Pendiente')){
    const m=minsFrom(p.fechaRegistro);
    return m===0?'Recién recibido':`Esperando ${m} min`;
  }
  if(esEstado(p,'En Preparación')){
    const m=minsFrom(p.fechaInicioPreparacion||p.fechaRegistro);
    return m===0?'Preparación iniciada':`Preparando ${m} min`;
  }
  const m=minsFrom(p.fechaListo||p.fechaInicioPreparacion||p.fechaRegistro);
  return m===0?'Recién terminado':`Listo hace ${m} min`;
}

function pickupMeta(p){
  const m=minsFrom(p.fechaListo||p.fechaInicioPreparacion||p.fechaRegistro);
  if(m>=20)return {minutes:m,label:`Retiro pendiente hace ${m} min`,level:'critical',icon:'!'};
  if(m>=10)return {minutes:m,label:`Esperando retiro hace ${m} min`,level:'warning',icon:'!'};
  return {minutes:m,label:m===0?'Recién listo':`Listo hace ${m} min`,level:'ok',icon:'✓'};
}

function renderPickup(p){
  const meta=pickupMeta(p);
  const count=totalPlatos(p);
  const notas=(p.detalles||[]).filter(d=>String(d.observacion||'').trim()).length;
  return `<article class="pickup-order pickup-${meta.level}">
    <div class="pickup-order-head"><div><strong>#${p.numero} · ${escapeHtml(p.cliente)}</strong><small>${meta.label}</small></div><span>${meta.icon}</span></div>
    <div class="pickup-items">${(p.detalles||[]).slice(0,2).map(d=>`<div><b>${d.cantidad}×</b> ${escapeHtml(d.plato)}</div>${d.observacion?`<div class="pickup-note">! ${escapeHtml(d.observacion)}</div>`:''}`).join('')}${(p.detalles||[]).length>2?`<div class="text-muted">+ ${(p.detalles||[]).length-2} producto(s)</div>`:''}</div>
    <div class="pickup-footer"><span>${count} ${count===1?'plato':'platos'}${notas?` · ${notas} con indicaciones`:''}</span><strong>Esperando caja</strong></div>
  </article>`;
}

function cashierPriorityMeta(p,pago){
  const base=pago?.fechaPago||p.fechaListo||p.fechaRegistro;
  const m=minsFrom(base);
  const level=m>=20?'critical':m>=10?'warning':'ok';
  if(pago){
    return {
      minutes:m,
      level,
      label:m===0?'Pagado recién':`Pagado hace ${m} min`,
      state:'Pagado · pendiente de despacho',
      icon:'✓',
      action:'Despachar',
      kind:'paid'
    };
  }
  return {
    minutes:m,
    level,
    label:m===0?'Listo para cobrar':`Sin cobrar hace ${m} min`,
    state:'Listo · sin cobrar',
    icon:'S/',
    action:'Cobrar',
    kind:'unpaid'
  };
}

function renderCashierPriority(p,pago){
  const meta=cashierPriorityMeta(p,pago);
  const count=totalPlatos(p);
  const notas=(p.detalles||[]).filter(d=>String(d.observacion||'').trim()).length;
  const action=meta.kind==='unpaid'
    ? `<a class="btn btn-sm btn-warning cashier-priority-action" href="/pagos.html?pedido=${p.numero}">Cobrar</a>`
    : `<button class="btn btn-sm btn-success cashier-priority-action" type="button" onclick="dashboardDespachar(${p.numero})">Despachar</button>`;
  return `<article class="cashier-priority-order cashier-${meta.kind} cashier-${meta.level}">
    <div class="cashier-priority-head">
      <div><strong>#${p.numero} · ${escapeHtml(p.cliente)}</strong><small>${meta.label}</small></div>
      <span class="cashier-priority-icon">${meta.icon}</span>
    </div>
    <div class="cashier-priority-state ${meta.kind}">${meta.state}</div>
    <div class="cashier-priority-items">${(p.detalles||[]).slice(0,2).map(d=>`<div><b>${d.cantidad}×</b> ${escapeHtml(d.plato)}</div>${d.observacion?`<div class="cashier-priority-note">! ${escapeHtml(d.observacion)}</div>`:''}`).join('')}${(p.detalles||[]).length>2?`<div class="text-muted">+ ${(p.detalles||[]).length-2} producto(s)</div>`:''}</div>
    <div class="cashier-priority-footer">
      <div><strong>${money(p.total)}</strong><small>${count} ${count===1?'plato':'platos'}${notas?` · ${notas} con indicaciones`:''}</small></div>
      ${action}
    </div>
  </article>`;
}

async function dashboardDespachar(numero){
  const ok=await appConfirm({
    title:`Despachar pedido #${numero}`,
    message:'El pedido ya está pagado y se marcará como despachado.',
    note:'Confirma únicamente cuando el pedido haya sido entregado al cliente.',
    confirmText:'Confirmar despacho',
    tone:'success',
    icon:'✓'
  });
  if(!ok)return;
  try{
    await api(`/api/pedidos/${numero}/despachar`,{method:'POST'});
    showToast(`Pedido #${numero} despachado`);
    await cargarDashboard();
  }catch(e){showError(e)}
}

function renderPedido(p,rol,pagados){
  if(rol==='COCINERO'){
    const count=totalPlatos(p);
    const notas=(p.detalles||[]).filter(d=>String(d.observacion||'').trim()||Number(d.costoExtra||0)>0).length;
    const pending=esEstado(p,'Pendiente');
    return `<div class="col-md-6"><article class="order-card dashboard-order-card kitchen-dashboard-order kitchen-work-${pending?'pending':'prep'}">
      <div class="d-flex justify-content-between gap-2">
        <div><h6 class="mb-1">#${p.numero} · ${escapeHtml(p.cliente)}</h6><span class="dashboard-kitchen-time ${pending?'wait':'prep'}">${kitchenElapsed(p)}</span></div>
        <span class="status ${statusClass(p.estado)}">${p.estado}</span>
      </div>
      <ul class="order-items">${(p.detalles||[]).slice(0,3).map(d=>`<li><b>${d.cantidad}×</b> ${escapeHtml(d.plato)}${d.observacion?`<div class="dashboard-kitchen-note">! ${escapeHtml(d.observacion)}</div>`:''}</li>`).join('')}${(p.detalles||[]).length>3?`<li class="text-muted">+ ${(p.detalles||[]).length-3} producto(s)</li>`:''}</ul>
      <div class="d-flex justify-content-between align-items-center mt-3"><span class="small text-muted">${count} ${count===1?'plato':'platos'}${notas?` · ${notas} con indicaciones`:''}</span><a class="btn btn-sm btn-outline-danger" href="/cocina.html">${pending?'Iniciar en cocina':'Ver preparación'}</a></div>
    </article></div>`;
  }

  const pagado=pagados.has(p.numero);
  const paymentBadge=(rol==='CAJERO'||rol==='ADMINISTRADOR')
    ? `<span class="payment-state ${pagado?'paid':'unpaid'}">${pagado?'Pagado':'Sin cobrar'}</span>`:'';
  const extras=p.detalles?.filter(d=>Number(d.costoExtra||0)>0).length||0;
  return `<div class="col-md-6"><article class="order-card dashboard-order-card">
    <div class="d-flex justify-content-between gap-2">
      <div><h6 class="mb-1">#${p.numero} · ${escapeHtml(p.cliente)}</h6><small class="text-muted">${fecha(p.fechaRegistro)}</small></div>
      <div class="d-flex flex-column align-items-end gap-1"><span class="status ${statusClass(p.estado)}">${p.estado}</span>${paymentBadge}</div>
    </div>
    <ul class="order-items">${(p.detalles||[]).slice(0,3).map(d=>`<li>${d.cantidad}x ${escapeHtml(d.plato)}${Number(d.costoExtra||0)>0?' <span class="text-muted">(+ extra)</span>':''}</li>`).join('')}${(p.detalles||[]).length>3?`<li class="text-muted">+ ${(p.detalles||[]).length-3} producto(s)</li>`:''}</ul>
    <div class="d-flex justify-content-between align-items-end mt-3"><div><span class="small text-muted d-block">${p.telefono?escapeHtml(p.telefono):'Sin teléfono'}</span>${extras?`<span class="small text-muted">${extras} extra${extras===1?'':'s'}</span>`:''}</div><span class="order-total">${money(p.total)}</span></div>
  </article></div>`;
}

async function cargarDashboard(){
  if(!CURRENT_USER)return;
  try{
    const rol=CURRENT_USER.rol;
    configurarCabeceraPorRol(rol);
    const pedidos=await api('/api/pedidos');
    const pend=pedidos.filter(p=>esEstado(p,'Pendiente')).length;
    const prep=pedidos.filter(p=>esEstado(p,'En Preparación')).length;
    const listos=pedidos.filter(p=>esEstado(p,'Listo')).length;

    let pagados=new Set();
    let pagosPorPedido=new Map();
    if(rol==='CAJERO'||rol==='ADMINISTRADOR'){
      try{
        const pagos=await api('/api/pagos');
        pagosPorPedido=new Map(pagos.filter(x=>x.pedido?.numero).map(x=>[x.pedido.numero,x]));
        pagados=new Set(pagosPorPedido.keys());
      }catch{}
    }
    const sinCobrar=pedidos.filter(p=>!pagados.has(p.numero)).length;
    const listosSinCobrar=pedidos.filter(p=>esEstado(p,'Listo')&&!pagados.has(p.numero)).length;
    const pagadosActivos=pedidos.filter(p=>pagados.has(p.numero)).length;

    let cards=[];
    if(rol==='CAJERO'){
      cards=[
        tarjetaKpi('Pedidos activos',pedidos.length,'▦','En atención'),
        tarjetaKpi('Sin cobrar',sinCobrar,'S/','Requieren pago'),
        tarjetaKpi('Listos',listos,'✓','Preparación terminada'),
        tarjetaKpi('Pagados',pagadosActivos,'●','Pedidos activos con pago')
      ];
    }else if(rol==='COCINERO'){
      const platosCola=pedidos.filter(p=>!esEstado(p,'Listo')).reduce((n,p)=>n+totalPlatos(p),0);
      cards=[
        tarjetaKpi('Por iniciar',pend,'⌛','Pedidos pendientes'),
        tarjetaKpi('En preparación',prep,'♨','Trabajándose ahora'),
        tarjetaKpi('Listos',listos,'✓','Esperando retiro'),
        tarjetaKpi('Platos por preparar',platosCola,'☷','Pendientes + preparación')
      ];
    }else{
      let ventasHoy='S/ 0.00';
      try{
        const hoy=localIsoDate();
        const resumen=await api(`/api/reportes/resumen?desde=${hoy}&hasta=${hoy}`);
        ventasHoy=money(resumen.totalVendido||0);
      }catch{}
      cards=[
        tarjetaKpi('Pedidos activos',pedidos.length,'▦','Operación actual'),
        tarjetaKpi('Pendientes',pend,'⌛','Por preparar'),
        tarjetaKpi('Listos',listos,'✓','Terminados'),
        tarjetaKpi('Ventas de hoy',ventasHoy,'S/','Pedidos despachados')
      ];
    }
    kpis.innerHTML=cards.join('');

    let ordenados=ordenarPedidos(pedidos,rol,pagados);
    if(rol==='COCINERO'){
      const trabajo=ordenados.filter(p=>!esEstado(p,'Listo')).slice(0,6);
      const retiro=pedidos.filter(p=>esEstado(p,'Listo')).sort((a,b)=>pickupMeta(b).minutes-pickupMeta(a).minutes);
      ordersTitle.textContent='Trabajo de cocina';
      ordersSubtitle.textContent='Primero los pendientes y preparaciones que llevan más tiempo.';
      document.getElementById('pedidos').innerHTML=trabajo.length
        ?trabajo.map(p=>renderPedido(p,rol,pagados)).join('')
        :'<div class="col-12"><div class="empty-state dashboard-empty kitchen-work-empty"><div class="empty-state-icon">✓</div><strong>Sin pedidos por preparar</strong><div class="small mt-1">No hay pedidos pendientes ni en preparación.</div></div></div>';

      priorityEyebrow.textContent='Esperando retiro';
      priorityTitle.textContent=retiro.length?`${retiro.length} pedido${retiro.length===1?' listo':'s listos'}`:'Sin pedidos listos';
      priorityText.textContent=retiro.length?'Cocina ya terminó estos pedidos. Caja debe completar su retiro o despacho.':'Cuando termines un pedido aparecerá aquí.';
      priorityIcon.textContent=retiro.some(p=>pickupMeta(p).level==='critical')?'!':'✓';
      priorityList.innerHTML=retiro.length?retiro.slice(0,4).map(renderPickup).join(''):'<div class="pickup-empty">No hay pedidos esperando retiro.</div>';
      priorityCard.classList.remove('cashier-priority-card');
      priorityCard.classList.add('kitchen-pickup-card');
      if(document.getElementById('dashboardCocinaLink'))dashboardCocinaLink.style.display='none';
    }else{
      ordenados=ordenados.slice(0,6);
      document.getElementById('pedidos').innerHTML=ordenados.length
        ?ordenados.map(p=>renderPedido(p,rol,pagados)).join('')
        :'<div class="col-12"><div class="empty-state dashboard-empty"><div class="empty-state-icon">✓</div><strong>Sin pedidos pendientes</strong><div class="small mt-1">La operación está al día en este momento.</div></div></div>';

      if(rol==='CAJERO'){
        const listosCaja=pedidos.filter(p=>esEstado(p,'Listo'));
        const porCobrar=listosCaja.filter(p=>!pagados.has(p.numero)).sort((a,b)=>cashierPriorityMeta(b,null).minutes-cashierPriorityMeta(a,null).minutes);
        const porDespachar=listosCaja.filter(p=>pagados.has(p.numero)).sort((a,b)=>cashierPriorityMeta(b,pagosPorPedido.get(b.numero)).minutes-cashierPriorityMeta(a,pagosPorPedido.get(a.numero)).minutes);
        const atencion=[...porCobrar,...porDespachar];
        priorityEyebrow.textContent='Atención de caja';
        priorityTitle.textContent=atencion.length?`${atencion.length} pedido${atencion.length===1?' por atender':'s por atender'}`:'Caja al día';
        priorityText.textContent=atencion.length?'Primero cobra los pedidos listos sin pago; después despacha los que ya están pagados.':'No hay pedidos listos pendientes de cobro o despacho.';
        priorityIcon.textContent=porCobrar.some(p=>cashierPriorityMeta(p,null).level==='critical')?'!':porCobrar.length?'S/':'✓';
        priorityList.innerHTML=atencion.length?atencion.slice(0,5).map(p=>renderCashierPriority(p,pagosPorPedido.get(p.numero))).join(''):'<div class="cashier-priority-empty">No hay pedidos que requieran atención de Caja.</div>';
        priorityCard.classList.remove('kitchen-pickup-card');
        priorityCard.classList.add('cashier-priority-card');
      }else{
        const [pt,px,pi]=prioridadPorRol(rol,{pend,prep,listos,sinCobrar,listosSinCobrar});
        priorityEyebrow.textContent='Atención prioritaria';
        priorityTitle.textContent=pt;
        priorityText.textContent=px;
        priorityIcon.textContent=pi;
        priorityList.innerHTML='';
        priorityCard.classList.remove('kitchen-pickup-card','cashier-priority-card');
      }
      if(document.getElementById('dashboardCocinaLink'))dashboardCocinaLink.style.display='';
    }
    if(document.getElementById('quickTitle'))quickTitle.textContent=rol==='COCINERO'?'Panel de cocina':'Accesos rápidos';
    quickSubtitle.textContent=rol==='CAJERO'?'Registra pedidos y cobros desde un solo lugar.':rol==='COCINERO'?'Abre el panel para iniciar y terminar preparaciones.':'Accesos principales para supervisar la operación.';
    lastUpdate.textContent=`Actualizado ${new Date().toLocaleTimeString('es-PE',{hour:'2-digit',minute:'2-digit'})}`;
  }catch(e){showError(e)}
}

function iniciarDashboard(){
  if(dashboardTimer)clearInterval(dashboardTimer);
  cargarDashboard();
  dashboardTimer=setInterval(cargarDashboard,CURRENT_USER?.rol==='COCINERO'?10000:15000);
}

document.addEventListener('app:user-ready',iniciarDashboard);
document.addEventListener('DOMContentLoaded',()=>{if(CURRENT_USER)iniciarDashboard()});
