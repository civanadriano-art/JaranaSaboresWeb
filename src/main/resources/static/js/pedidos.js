let platos=[];
let editandoNumero=null;
let pedidosCache=[];
let pagosMap={};
let catalogoTimer=null;

async function inicio(){
  platos=(await api('/api/platos')).filter(p=>p.disponible!==false);
  agregarLinea();
  conectarFiltros();
  await cargar();
  if(catalogoTimer)clearInterval(catalogoTimer);
  catalogoTimer=setInterval(()=>refrescarPlatosDisponibles(),10000);
  window.addEventListener('focus',()=>refrescarPlatosDisponibles());
}

async function refrescarPlatosDisponibles(){
  try{
    const nuevos=(await api('/api/platos')).filter(p=>p.disponible!==false);
    const codigosNuevos=new Set(nuevos.map(p=>Number(p.codigo)));
    let seleccionAgotada=false;
    platos=nuevos;
    document.querySelectorAll('.linea .plato').forEach(select=>{
      const actual=Number(select.value||0);
      const conservar=actual&&codigosNuevos.has(actual);
      select.innerHTML=`<option value="">Seleccione plato...</option>${platos.map(p=>`<option value="${p.codigo}">${escapeHtml(p.nombre)} · ${money(p.precio)}</option>`).join('')}`;
      if(conservar)select.value=String(actual);
      else if(actual){seleccionAgotada=true;select.value='';}
    });
    if(seleccionAgotada)showToast('Un plato seleccionado fue marcado como agotado por Cocina','warning');
    calcularResumen();
  }catch(e){console.warn('No se pudo actualizar la disponibilidad de platos',e)}
}

function conectarFiltros(){
  buscarPedido?.addEventListener('input',renderLista);
  filtroEstado?.addEventListener('change',renderLista);
  filtroPago?.addEventListener('change',renderLista);
}

function agregarLinea(valor={}){
  const div=document.createElement('div');
  div.className='order-line linea';
  div.innerHTML=`
    <div class="row g-2 align-items-end">
      <div class="col-lg-4 col-md-6">
        <label class="form-label small text-muted mb-1">Plato</label>
        <select class="form-select plato" required>
          <option value="">Seleccione plato...</option>
          ${platos.map(p=>`<option value="${p.codigo}" ${Number(valor.codigoPlato)===p.codigo?'selected':''}>${escapeHtml(p.nombre)} · ${money(p.precio)}</option>`).join('')}
        </select>
      </div>
      <div class="col-lg-1 col-md-2 col-4">
        <label class="form-label small text-muted mb-1">Cant.</label>
        <input type="number" min="1" value="${valor.cantidad||1}" class="form-control cantidad" aria-label="Cantidad">
      </div>
      <div class="col-lg-4 col-md-6">
        <label class="form-label small text-muted mb-1">Observación <span class="fw-normal">(opcional)</span></label>
        <input class="form-control obs" placeholder="Ej. sin cebolla, porción adicional..." value="${escapeHtml(valor.observacion||'')}">
      </div>
      <div class="col-lg-2 col-md-3 col-6">
        <label class="form-label small text-muted mb-1">Extra S/ <span class="fw-normal">(opcional)</span></label>
        <input type="number" min="0" step="0.10" class="form-control extra" placeholder="0.00" value="${Number(valor.costoExtra||0)||''}">
      </div>
      <div class="col-lg-1 col-md-1 col-2 d-grid">
        <button type="button" class="btn btn-outline-danger btn-remove-line" aria-label="Quitar plato">×</button>
      </div>
    </div>`;
  div.querySelectorAll('select,input').forEach(el=>el.addEventListener('input',calcularResumen));
  div.querySelector('.btn-remove-line').addEventListener('click',()=>{
    if(document.querySelectorAll('.linea').length===1){
      div.querySelector('.plato').value='';
      div.querySelector('.cantidad').value=1;
      div.querySelector('.obs').value='';
      div.querySelector('.extra').value='';
    }else div.remove();
    calcularResumen();
  });
  lineas.appendChild(div);
  calcularResumen();
}

function calcularResumen(){
  let cantidadTotal=0,extras=0,total=0;
  document.querySelectorAll('.linea').forEach(x=>{
    const codigo=Number(x.querySelector('.plato').value);
    const cantidad=Math.max(0,Number(x.querySelector('.cantidad').value||0));
    const extra=Math.max(0,Number(x.querySelector('.extra').value||0));
    const plato=platos.find(p=>Number(p.codigo)===codigo);
    cantidadTotal+=cantidad;
    extras+=extra;
    total+=(Number(plato?.precio||0)*cantidad)+extra;
  });
  resumenCantidad.textContent=cantidadTotal;
  resumenExtras.textContent=money(extras);
  resumenTotal.textContent=money(total);
}

pedidoForm.addEventListener('submit',async e=>{
  e.preventDefault();
  try{
    const detalles=[...document.querySelectorAll('.linea')].map(x=>({
      codigoPlato:Number(x.querySelector('.plato').value),
      cantidad:Number(x.querySelector('.cantidad').value),
      observacion:x.querySelector('.obs').value.trim(),
      costoExtra:Number(x.querySelector('.extra').value||0)
    }));
    if(!detalles.length||detalles.some(d=>!d.codigoPlato||d.cantidad<1))throw new Error('Agrega al menos un plato válido al pedido');
    const payload={nombreCliente:cliente.value.trim(),telefono:telefono.value.trim(),detalles};
    if(editandoNumero){
      await api('/api/pedidos/'+editandoNumero,{method:'PUT',body:JSON.stringify(payload)});
      showToast('Pedido actualizado correctamente');
    }else{
      const p=await api('/api/pedidos',{method:'POST',body:JSON.stringify(payload)});
      showToast('Pedido #'+p.numero+' registrado');
    }
    cancelarEdicion();
    await cargar();
  }catch(err){showError(err)}
});

async function editarPedido(n){
  try{
    const pago=await api(`/api/pagos/pedido/${n}`).catch(()=>({pagado:false}));
    if(pago.pagado)return showToast('Un pedido pagado ya no puede modificarse','warning');
    const p=await api('/api/pedidos/'+n);
    if(p.estado!=='Pendiente')return showToast('Solo se pueden editar pedidos Pendientes','warning');
    editandoNumero=n;
    cliente.value=p.cliente;
    telefono.value=p.telefono||'';
    lineas.innerHTML='';
    p.detalles.forEach(d=>agregarLinea({codigoPlato:d.codigo,cantidad:d.cantidad,observacion:d.observacion,costoExtra:d.costoExtra}));
    btnGuardar.textContent='Guardar cambios';
    btnCancelarEdicion.classList.remove('d-none');
    formTitle.textContent='Editar pedido #'+n;
    calcularResumen();
    document.getElementById('pedidoFormCard').scrollIntoView({behavior:'smooth',block:'start'});
  }catch(e){showError(e)}
}

function cancelarEdicion(){
  editandoNumero=null;
  pedidoForm.reset();
  lineas.innerHTML='';
  agregarLinea();
  btnGuardar.textContent='Registrar pedido';
  btnCancelarEdicion.classList.add('d-none');
  formTitle.textContent='Nuevo pedido';
  calcularResumen();
}

async function cargar(){
  try{
    pedidosCache=await api('/api/pedidos');
    const pagos=await Promise.all(pedidosCache.map(async p=>[p.numero,(await api(`/api/pagos/pedido/${p.numero}`).catch(()=>({pagado:false}))).pagado]));
    pagosMap=Object.fromEntries(pagos);
    actualizarKpis();
    renderLista();
  }catch(e){showError(e)}
}

function actualizarKpis(){
  kpiActivos.textContent=pedidosCache.length;
  kpiPendientes.textContent=pedidosCache.filter(p=>p.estado==='Pendiente').length;
  kpiListos.textContent=pedidosCache.filter(p=>p.estado==='Listo').length;
  kpiSinCobrar.textContent=pedidosCache.filter(p=>!pagosMap[p.numero]).length;
}

function renderLista(){
  const q=(buscarPedido?.value||'').trim().toLowerCase();
  const estado=filtroEstado?.value||'';
  const pago=filtroPago?.value||'';
  const filtrados=pedidosCache.filter(p=>{
    const texto=[p.numero,p.cliente,p.telefono,...(p.detalles||[]).map(d=>`${d.plato} ${d.observacion||''}`)].join(' ').toLowerCase();
    const coincideTexto=!q||texto.includes(q);
    const coincideEstado=!estado||p.estado===estado;
    const pagado=!!pagosMap[p.numero];
    const coincidePago=!pago||(pago==='PAGADO'?pagado:!pagado);
    return coincideTexto&&coincideEstado&&coincidePago;
  });
  resultadosPedidos.textContent=`${filtrados.length} ${filtrados.length===1?'pedido':'pedidos'}`;
  if(!filtrados.length){
    lista.innerHTML=`<div class="empty-state py-5"><div class="empty-state-icon">▦</div><strong>No hay pedidos que coincidan</strong><div class="small mt-1">Prueba cambiando los filtros de búsqueda.</div></div>`;
    return;
  }
  lista.innerHTML=`<table class="table table-hover table-app orders-table"><thead><tr><th>Ticket</th><th>Cliente</th><th>Estado</th><th>Detalle</th><th>Total</th><th>Pago</th><th class="text-end">Acciones</th></tr></thead><tbody>${filtrados.map(p=>{
    const pagado=!!pagosMap[p.numero];
    const acciones=p.estado==='Pendiente'&&!pagado
      ? `<button class="btn btn-sm btn-outline-primary" onclick="editarPedido(${p.numero})">Editar</button><button class="btn btn-sm btn-outline-danger" onclick="cancelar(${p.numero})">Cancelar</button>`
      : p.estado==='Listo'&&pagado
        ? `<button class="btn btn-sm btn-success" onclick="despachar(${p.numero})">Despachar</button>`
        : p.estado==='Listo'&&!pagado
          ? `<a class="btn btn-sm btn-warning" href="/pagos.html?pedido=${p.numero}">Cobrar</a>`
          : `<span class="text-muted small">Sin acciones</span>`;
    return `<tr>
      <td><strong>#${p.numero}</strong><br><small class="text-muted">${fecha(p.fechaRegistro)}</small></td>
      <td><strong>${escapeHtml(p.cliente)}</strong><br><small class="text-muted">${p.telefono?escapeHtml(p.telefono):'Sin teléfono'}</small></td>
      <td><span class="status ${statusClass(p.estado)}">${p.estado}</span></td>
      <td><div class="order-detail-list">${p.detalles.map(d=>`<div><strong>${d.cantidad}×</strong> ${escapeHtml(d.plato)}${d.observacion?`<div class="small text-muted">${escapeHtml(d.observacion)}</div>`:''}${Number(d.costoExtra||0)>0?`<div class="small text-success">Extra: ${money(d.costoExtra)}</div>`:''}</div>`).join('')}</div></td>
      <td><strong>${money(p.total)}</strong></td>
      <td>${pagado?'<span class="payment-state paid">Pagado</span>':'<span class="payment-state unpaid">Sin cobrar</span>'}</td>
      <td><div class="order-actions">${acciones}</div></td>
    </tr>`
  }).join('')}</tbody></table>`;
}

function limpiarFiltrosPedidos(){
  buscarPedido.value='';filtroEstado.value='';filtroPago.value='';renderLista();
}

async function cancelar(n){
  const motivo=await appPrompt({title:`Cancelar pedido #${n}`,message:'Indica por qué se está cancelando este pedido.',note:'El pedido pasará al historial de eliminados y podrá ser restaurado por un administrador.',confirmText:'Cancelar pedido',tone:'danger',icon:'×',input:{label:'Motivo de cancelación',placeholder:'Ej. Pedido duplicado, cliente desistió...',maxLength:255,requiredMessage:'Escribe el motivo de cancelación.'}});
  if(motivo===null)return;
  try{
    await api(`/api/pedidos/${n}/cancelar`,{method:'POST',body:JSON.stringify({motivo:motivo.trim()})});
    showToast('Pedido cancelado');
    if(editandoNumero===n)cancelarEdicion();
    cargar();
  }catch(e){showError(e)}
}

async function despachar(n){
  const ok=await appConfirm({title:`Despachar pedido #${n}`,message:'El pedido se marcará como despachado y se generará la venta correspondiente.',note:'Solo continúa si el pedido ya fue entregado al cliente.',confirmText:'Confirmar despacho',tone:'success',icon:'✓'});
  if(!ok)return;
  try{await api(`/api/pedidos/${n}/despachar`,{method:'POST'});showToast('Pedido #'+n+' despachado');cargar()}catch(e){showError(e)}
}

document.addEventListener('DOMContentLoaded',()=>inicio().catch(showError));
