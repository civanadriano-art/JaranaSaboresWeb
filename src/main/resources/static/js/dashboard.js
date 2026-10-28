async function cargarDashboard(){
  try{
    const pedidos=await api('/api/pedidos');
    let resumen=null; try{resumen=await api('/api/reportes/resumen')}catch{}
    const pend=pedidos.filter(p=>p.estado==='Pendiente').length;
    const prep=pedidos.filter(p=>p.estado==='En Preparación').length;
    const listos=pedidos.filter(p=>p.estado==='Listo').length;
    const cards=[
      ['Pedidos activos',pedidos.length,'▦'],['Pendientes',pend,'⌛'],['En preparación',prep,'♨'],['Listos',listos,'✓']
    ];
    if(resumen && CURRENT_USER?.rol==='ADMINISTRADOR') cards.splice(3,1,['Ventas',money(resumen.totalVendido),'S/']);
    kpis.innerHTML=cards.map(c=>`<div class="col-sm-6 col-xl-3"><div class="kpi-card"><div class="kpi-head"><div><div class="kpi-label">${c[0]}</div><div class="kpi-value">${c[1]}</div></div><div class="kpi-icon">${c[2]}</div></div></div></div>`).join('');
    document.getElementById('pedidos').innerHTML=pedidos.length?pedidos.slice(0,6).map(p=>`<div class="col-md-6"><article class="order-card"><div class="d-flex justify-content-between gap-2"><div><h6 class="mb-1">#${p.numero} · ${escapeHtml(p.cliente)}</h6><small class="text-muted">${fecha(p.fechaRegistro)}</small></div><span class="status ${statusClass(p.estado)}">${p.estado}</span></div><ul class="order-items">${p.detalles.map(d=>`<li>${d.cantidad}x ${escapeHtml(d.plato)}</li>`).join('')}</ul><div class="d-flex justify-content-between align-items-center mt-3"><span class="small text-muted">${p.telefono?escapeHtml(p.telefono):'Sin teléfono'}</span><span class="order-total">${money(p.total)}</span></div></article></div>`).join(''):'<div class="empty-state">No hay pedidos activos en este momento.</div>';
  }catch(e){showError(e)}
}
document.addEventListener('DOMContentLoaded',()=>{setTimeout(cargarDashboard,100);setInterval(cargarDashboard,15000)});
