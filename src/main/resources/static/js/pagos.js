let pedidos=[];
let pagosCache=[];
let pedidosPendientes=[];

async function cargarPedidos(){
  pedidos=await api('/api/pedidos');
  const estadosPago=await Promise.all(pedidos.map(async p=>[p.numero,(await api(`/api/pagos/pedido/${p.numero}`).catch(()=>({pagado:false}))).pagado]));
  const mapaPago=Object.fromEntries(estadosPago);
  pedidosPendientes=pedidos.filter(p=>!mapaPago[p.numero]&&p.estado!=='Cancelado');
  pedidoSelect.innerHTML='<option value="">Seleccione pedido...</option>'+pedidosPendientes.map(p=>`<option value="${p.numero}">#${p.numero} · ${escapeHtml(p.cliente)} · ${money(p.total)}${p.estado==='Listo'?' · LISTO':''}</option>`).join('');
  const solicitado=new URLSearchParams(location.search).get('pedido');
  if(solicitado&&pedidosPendientes.some(p=>String(p.numero)===solicitado))pedidoSelect.value=solicitado;
  actualizarPedidoSeleccionado();
  actualizarKpis();
}

function actualizarPedidoSeleccionado(){
  const p=pedidosPendientes.find(x=>x.numero===Number(pedidoSelect.value));
  totalPedido.textContent=money(p?.total||0);
  btnConfirmarPago.disabled=!p;
  paymentSecureState(p);
  if(!p){
    pedidoResumen.innerHTML='<div class="selected-order-empty">Selecciona un pedido para ver el detalle.</div>';
    calcularVuelto();
    return;
  }
  pedidoResumen.innerHTML=`
    <div class="d-flex justify-content-between gap-3 mb-2"><div><span class="selected-order-label">Pedido</span><strong>#${p.numero}</strong></div><div class="text-end"><span class="selected-order-label">Estado</span><span class="status ${statusClass(p.estado)}">${p.estado}</span></div></div>
    <div class="selected-order-client"><strong>${escapeHtml(p.cliente)}</strong><span>${p.telefono?escapeHtml(p.telefono):'Sin teléfono'}</span></div>
    <div class="selected-order-items">${(p.detalles||[]).map(d=>`<div><span>${d.cantidad}× ${escapeHtml(d.plato)}</span><strong>${money((Number(d.precioUnitario||0)*Number(d.cantidad||0))+Number(d.costoExtra||0))}</strong></div>`).join('')}</div>`;
  calcularVuelto();
}

function paymentSecureState(p){
  const badge=document.querySelector('.payment-secure-badge');
  if(!badge)return;
  if(!p){badge.textContent='Pago pendiente';badge.classList.remove('ready');return}
  if(p.estado==='Listo'){badge.textContent='Listo para cobrar';badge.classList.add('ready')}
  else{badge.textContent='Pedido activo';badge.classList.remove('ready')}
}

function seleccionarMetodo(valor){
  metodo.value=valor;
  document.querySelectorAll('.payment-method').forEach(b=>b.classList.toggle('active',b.dataset.method===valor));
  actualizarMetodo();
}

function actualizarMetodo(){
  const efectivo=metodo.value==='EFECTIVO';
  recibidoWrap.classList.toggle('d-none',!efectivo);
  importeRecibido.required=efectivo;
  if(!efectivo)importeRecibido.value='';
  calcularVuelto();
}

function calcularVuelto(){
  const p=pedidosPendientes.find(x=>x.numero===Number(pedidoSelect.value));
  const total=Number(p?.total||0);
  const recibido=Number(importeRecibido.value||0);
  const vuelto=metodo.value==='EFECTIVO'?Math.max(0,recibido-total):0;
  vueltoEstimado.textContent=money(vuelto);
  vueltoEstimado.classList.toggle('text-danger',metodo.value==='EFECTIVO'&&recibido>0&&recibido<total);
}

pedidoSelect.addEventListener('change',actualizarPedidoSeleccionado);
importeRecibido.addEventListener('input',calcularVuelto);
document.querySelectorAll('.payment-method').forEach(b=>b.addEventListener('click',()=>seleccionarMetodo(b.dataset.method)));

pagoForm.addEventListener('submit',async e=>{
  e.preventDefault();
  try{
    const p=pedidosPendientes.find(x=>x.numero===Number(pedidoSelect.value));
    if(!p)return showToast('Selecciona un pedido','warning');
    const recibido=metodo.value==='EFECTIVO'?Number(importeRecibido.value):null;
    if(metodo.value==='EFECTIVO'&&(!recibido||recibido<Number(p.total)))return showToast('El importe recibido no puede ser menor al total','warning');
    const nombreMetodo=metodo.value==='EFECTIVO'?'Efectivo':metodo.value.charAt(0)+metodo.value.slice(1).toLowerCase();
    const ok=await appConfirm({title:`Confirmar pago · Pedido #${p.numero}`,message:`Se registrará un pago de ${money(p.total)} mediante ${nombreMetodo}.`,note:metodo.value==='EFECTIVO'?`Importe recibido: ${money(recibido)} · Vuelto estimado: ${money(Number(recibido)-Number(p.total))}`:'Verifica que el pago haya sido recibido antes de confirmar.',confirmText:'Confirmar pago',tone:'success',icon:'S/'});
    if(!ok)return;
    const body={metodo:metodo.value,importeRecibido:recibido};
    const pago=await api(`/api/pagos/pedido/${p.numero}`,{method:'POST',body:JSON.stringify(body)});
    const mensaje=metodo.value==='EFECTIVO'?`Pago registrado. Vuelto: ${money(pago.vuelto)}`:'Pago registrado correctamente';
    showToast(mensaje);
    pagoForm.reset();seleccionarMetodo('EFECTIVO');
    await Promise.all([cargarPedidos(),cargarPagos()]);
  }catch(e){showError(e)}
});

async function cargarPagos(){
  try{
    pagosCache=await api('/api/pagos');
    actualizarKpis();
    renderPagos();
  }catch(e){showError(e)}
}

function esHoy(fechaPago){
  if(!fechaPago)return false;
  const d=new Date(fechaPago),h=new Date();
  return d.getFullYear()===h.getFullYear()&&d.getMonth()===h.getMonth()&&d.getDate()===h.getDate();
}

function actualizarKpis(){
  if(typeof kpiCobroPendiente==='undefined')return;
  kpiCobroPendiente.textContent=pedidosPendientes.length;
  kpiListosSinCobrar.textContent=pedidosPendientes.filter(p=>p.estado==='Listo').length;
  const hoy=pagosCache.filter(x=>esHoy(x.fechaPago));
  kpiPagosHoy.textContent=hoy.length;
  kpiCobradoHoy.textContent=money(hoy.reduce((s,x)=>s+Number(x.importeTotal||0),0));
}

function metodoNombre(m){return m==='EFECTIVO'?'Efectivo':m==='YAPE'?'Yape':m==='PLIN'?'Plin':m||'—'}
function metodoClase(m){return m==='EFECTIVO'?'payment-efectivo':m==='YAPE'?'payment-yape':m==='PLIN'?'payment-plin':''}

function renderPagos(){
  const q=(buscarPago?.value||'').trim().toLowerCase();
  const filtro=filtroMetodoPago?.value||'';
  const xs=pagosCache.filter(x=>{
    const texto=[x.pedido?.numero,x.operador?.nombre,x.operador?.nombreUsuario,x.metodo].join(' ').toLowerCase();
    return (!q||texto.includes(q))&&(!filtro||x.metodo===filtro);
  });
  contadorPagos.textContent=`${xs.length} ${xs.length===1?'pago':'pagos'}`;
  if(!xs.length){
    pagosTabla.innerHTML='<div class="empty-state py-5"><div class="empty-state-icon">S/</div><strong>No hay pagos que coincidan</strong><div class="small mt-1">Prueba cambiando los filtros.</div></div>';
    return;
  }
  pagosTabla.innerHTML=`<table class="table table-hover table-app payments-table"><thead><tr><th>Fecha</th><th>Pedido</th><th>Método</th><th>Total</th><th>Recibido</th><th>Vuelto</th><th>Operador</th></tr></thead><tbody>${xs.map(x=>`<tr>
    <td>${fecha(x.fechaPago)}</td>
    <td><strong>#${x.pedido.numero}</strong></td>
    <td><span class="payment-badge ${metodoClase(x.metodo)}">${metodoNombre(x.metodo)}</span></td>
    <td><strong>${money(x.importeTotal)}</strong></td>
    <td>${x.metodo==='EFECTIVO'?money(x.importeRecibido):'<span class="text-muted">—</span>'}</td>
    <td>${x.metodo==='EFECTIVO'?money(x.vuelto):'<span class="text-muted">—</span>'}</td>
    <td>${escapeHtml(x.operador?.nombre||x.operador?.nombreUsuario||'—')}</td>
  </tr>`).join('')}</tbody></table>`;
}

function limpiarFiltrosPagos(){buscarPago.value='';filtroMetodoPago.value='';renderPagos()}
buscarPago?.addEventListener('input',renderPagos);
filtroMetodoPago?.addEventListener('change',renderPagos);

document.addEventListener('DOMContentLoaded',()=>{seleccionarMetodo('EFECTIVO');Promise.all([cargarPedidos(),cargarPagos()]).catch(showError)});
