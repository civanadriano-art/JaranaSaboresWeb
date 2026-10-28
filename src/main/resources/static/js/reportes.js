let ventasActuales=[];
let resumenActual=null;

function fechaISO(d){
  const y=d.getFullYear();
  const m=String(d.getMonth()+1).padStart(2,'0');
  const day=String(d.getDate()).padStart(2,'0');
  return `${y}-${m}-${day}`;
}

function textoFecha(v){
  if(!v)return '';
  return new Date(`${v}T00:00:00`).toLocaleDateString('es-PE',{day:'2-digit',month:'2-digit',year:'numeric'});
}

function metodoBonito(v){
  return ({EFECTIVO:'Efectivo',YAPE:'Yape',PLIN:'Plin',SIN_REGISTRO:'Sin registro'})[v]||v||'—';
}

function construirQuery(){
  const q=new URLSearchParams();
  if(desde.value)q.set('desde',desde.value);
  if(hasta.value)q.set('hasta',hasta.value);
  if(metodo.value)q.set('metodo',metodo.value);
  const s=q.toString();
  return s?`?${s}`:'';
}

function actualizarPeriodo(){
  let texto='Mostrando todas las ventas';
  if(desde.value&&hasta.value)texto=`Resultados del ${textoFecha(desde.value)} al ${textoFecha(hasta.value)}`;
  else if(desde.value)texto=`Resultados desde el ${textoFecha(desde.value)}`;
  else if(hasta.value)texto=`Resultados hasta el ${textoFecha(hasta.value)}`;
  if(metodo.value)texto+=` · ${metodoBonito(metodo.value)}`;
  periodoActual.textContent=texto;
}

function renderResumen(r){
  const plato=r.platoMasVendido==='Sin ventas'?'Sin ventas':`${r.platoMasVendido} · ${r.cantidadPlatoMasVendido} vendido${Number(r.cantidadPlatoMasVendido)===1?'':'s'}`;
  const cards=[
    ['Total vendido',money(r.totalVendido),'S/',''],
    ['Pedidos despachados',r.pedidosDespachados,'✓',''],
    ['Ticket promedio',money(r.ticketPromedio),'▤',''],
    ['Plato más vendido',plato,'★','fs-6'],
    ['Tiempo prom. preparación',`${Number(r.tiempoPromedioCocina||0).toLocaleString('es-PE',{maximumFractionDigits:1})} min`,'◷','']
  ];
  resumen.innerHTML=cards.map(c=>`<div class="col-sm-6 col-xl"><div class="kpi-card"><div class="kpi-head"><div><div class="kpi-label">${c[0]}</div><div class="kpi-value ${c[3]}">${escapeHtml(c[1])}</div></div><div class="kpi-icon">${c[2]}</div></div></div></div>`).join('');
}

function renderGrafico(datos){
  if(!datos?.length){graficoVentas.innerHTML='<div class="empty-state py-5">No hay ventas para graficar en este periodo.</div>';return}
  const max=Math.max(...datos.map(x=>Number(x.total||0)),1);
  graficoVentas.innerHTML=`<div class="bar-chart">${datos.map(x=>{
    const valor=Number(x.total||0);
    const alto=Math.max(8,(valor/max)*100);
    const dia=new Date(`${x.fecha}T00:00:00`).toLocaleDateString('es-PE',{day:'2-digit',month:'2-digit'});
    return `<div class="bar-item" title="${dia}: ${money(valor)}"><div class="bar-value">${money(valor)}</div><div class="bar-track"><div class="bar-fill" style="height:${alto}%"></div></div><div class="bar-label">${dia}</div></div>`;
  }).join('')}</div>`;
}

function renderPagos(datos){
  const xs=datos||[];
  const total=xs.reduce((a,x)=>a+Number(x.total||0),0);
  resumenPagos.innerHTML=xs.map(x=>{
    const valor=Number(x.total||0);
    const pct=total?Math.round((valor/total)*100):0;
    return `<div class="payment-row"><div class="d-flex justify-content-between gap-3"><span class="fw-bold">${metodoBonito(x.metodo)}</span><span>${money(valor)} <span class="text-muted">(${pct}%)</span></span></div><div class="payment-progress"><span style="width:${pct}%"></span></div></div>`;
  }).join('')||'<div class="empty-state py-4">Sin pagos registrados en este periodo.</div>';
}

function renderVentas(xs){
  cantidadResultados.textContent=`${xs.length} pedido${xs.length===1?'':'s'} despachado${xs.length===1?'':'s'}`;
  if(!xs.length){ventas.innerHTML='<div class="empty-state">No se encontraron ventas con los filtros seleccionados.</div>';return}
  ventas.innerHTML=`<table class="table table-hover table-app"><thead><tr><th>Pedido</th><th>Cliente</th><th>Despacho</th><th>Tiempo preparación</th><th>Pago</th><th class="text-end">Total</th></tr></thead><tbody>${xs.map(v=>`<tr><td><strong>#${v.numero}</strong></td><td>${escapeHtml(v.cliente)}</td><td>${fecha(v.fechaHoraDespacho)}</td><td>${v.minutosCocina?`${v.minutosCocina} min`:'—'}</td><td><span class="payment-badge payment-${String(v.metodoPago).toLowerCase()}">${metodoBonito(v.metodoPago)}</span></td><td class="text-end"><strong>${money(v.total)}</strong></td></tr>`).join('')}</tbody></table>`;
}

async function cargarReporte(){
  try{
    const query=construirQuery();
    const [r,xs]=await Promise.all([
      api('/api/reportes/resumen'+query),
      api('/api/reportes/ventas'+query)
    ]);
    resumenActual=r;
    ventasActuales=xs;
    renderResumen(r);
    renderGrafico(r.ventasPorDia);
    renderPagos(r.ventasPorMetodo);
    renderVentas(xs);
    actualizarPeriodo();
  }catch(e){showError(e)}
}

function marcarPeriodo(btn){
  document.querySelectorAll('[data-periodo]').forEach(x=>x.classList.remove('active','btn-jarana'));
  document.querySelectorAll('[data-periodo]').forEach(x=>x.classList.add('btn-outline-secondary'));
  if(btn){btn.classList.remove('btn-outline-secondary');btn.classList.add('active','btn-jarana')}
}

function aplicarPeriodo(tipo,btn){
  const hoy=new Date();
  if(tipo==='hoy'){
    desde.value=fechaISO(hoy);hasta.value=fechaISO(hoy);
  }else if(tipo==='semana'){
    const inicio=new Date(hoy);const dia=(hoy.getDay()+6)%7;inicio.setDate(hoy.getDate()-dia);
    desde.value=fechaISO(inicio);hasta.value=fechaISO(hoy);
  }else if(tipo==='mes'){
    desde.value=fechaISO(new Date(hoy.getFullYear(),hoy.getMonth(),1));hasta.value=fechaISO(hoy);
  }else{
    desde.value='';hasta.value='';
  }
  marcarPeriodo(btn);
  cargarReporte();
}

function csvCampo(v){
  const s=String(v??'').replace(/"/g,'""');
  return `"${s}"`;
}

function exportarExcel(){
  if(!ventasActuales.length){showToast('No hay ventas para exportar','warning');return}
  const filas=[['Pedido','Cliente','Despacho','Tiempo preparación','Método de pago','Total']];
  ventasActuales.forEach(v=>filas.push([
    `#${v.numero}`,
    v.cliente,
    fecha(v.fechaHoraDespacho),
    v.minutosCocina?`${v.minutosCocina} min`:'—',
    metodoBonito(v.metodoPago),
    Number(v.total||0).toFixed(2)
  ]));
  const contenido='\uFEFF'+filas.map(f=>f.map(csvCampo).join(';')).join('\r\n');
  const blob=new Blob([contenido],{type:'text/csv;charset=utf-8;'});
  const url=URL.createObjectURL(blob);
  const a=document.createElement('a');a.href=url;a.download=`reporte_jarana_${fechaISO(new Date())}.csv`;document.body.appendChild(a);a.click();a.remove();URL.revokeObjectURL(url);
}

function exportarPDF(){
  window.print();
}

document.addEventListener('DOMContentLoaded',()=>{
  metodo.addEventListener('change',()=>marcarPeriodo(null));
  desde.addEventListener('change',()=>marcarPeriodo(null));
  hasta.addEventListener('change',()=>marcarPeriodo(null));
  cargarReporte();
});
