async function api(url, options={}){
  const res=await fetch(url,{headers:{'Content-Type':'application/json',...(options.headers||{})},...options});
  if(res.status===204)return null;
  const text=await res.text(); let data={};
  try{data=text?JSON.parse(text):{}}catch{data={detalle:text}}
  if(!res.ok){if(res.status===401){location='/login.html';return} throw new Error(data.error||data.detalle||data.message||`HTTP ${res.status}`)}
  return data;
}
function money(v){return new Intl.NumberFormat('es-PE',{style:'currency',currency:'PEN'}).format(Number(v||0))}
function fecha(v){return v?new Date(v).toLocaleString('es-PE',{dateStyle:'short',timeStyle:'short'}):'—'}
function escapeHtml(v=''){return String(v).replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]))}
function statusClass(e=''){const x=e.toLowerCase();if(x.includes('prepar'))return 'status-preparacion';if(x==='listo')return 'status-listo';if(x.includes('desp'))return 'status-despachado';if(x.includes('cancel'))return 'status-cancelado';return 'status-pendiente'}
function showToast(msg,type='success'){
  let z=document.querySelector('.toast-zone');if(!z){z=document.createElement('div');z.className='toast-zone';document.body.appendChild(z)}
  const el=document.createElement('div');el.className=`alert alert-${type==='error'?'danger':type} shadow mb-2`;el.textContent=msg;z.appendChild(el);setTimeout(()=>el.remove(),3200)
}
function showError(e){showToast(e?.message||String(e),'error')}
