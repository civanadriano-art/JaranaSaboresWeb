let CURRENT_USER=null;
async function initShell(){
  try{CURRENT_USER=await api('/api/auth/me')}catch{return}
  document.querySelectorAll('[data-user-name]').forEach(x=>x.textContent=CURRENT_USER.nombre||CURRENT_USER.usuario);
  document.querySelectorAll('[data-user-role]').forEach(x=>x.textContent=(CURRENT_USER.rol||'').replace('ADMINISTRADOR','Administrador').replace('CAJERO','Caja').replace('COCINERO','Cocina'));
  document.querySelectorAll('[data-role]').forEach(x=>{const allowed=x.dataset.role.split(',');if(!allowed.includes(CURRENT_USER.rol))x.classList.add('d-none')});
  const here=location.pathname.split('/').pop()||'dashboard.html';document.querySelectorAll('.nav-link-app').forEach(a=>{if(a.getAttribute('href')?.endsWith(here))a.classList.add('active')});
}
function ensureSidebarBackdrop(){
  let b=document.querySelector('.sidebar-backdrop');
  if(!b){b=document.createElement('div');b.className='sidebar-backdrop';b.addEventListener('click',closeSidebar);document.body.appendChild(b)}
  return b;
}
function openSidebar(){document.querySelector('.sidebar')?.classList.add('open');ensureSidebarBackdrop().classList.add('show')}
function closeSidebar(){document.querySelector('.sidebar')?.classList.remove('open');document.querySelector('.sidebar-backdrop')?.classList.remove('show')}
function toggleSidebar(){document.querySelector('.sidebar')?.classList.contains('open')?closeSidebar():openSidebar()}
document.addEventListener('DOMContentLoaded',()=>{
  initShell();
  ensureSidebarBackdrop();
  document.querySelectorAll('.sidebar .nav-link-app').forEach(a=>a.addEventListener('click',()=>{if(window.innerWidth<992)closeSidebar()}));
});
window.addEventListener('resize',()=>{if(window.innerWidth>=992)closeSidebar()});
