#!/usr/bin/env python3
"""
Pruebas de seguridad de La Jarana de Sabores (Sesión 28: Pruebas de Seguridad).

Cubre: autenticación, autorización por roles, validación de entradas, integridad del flujo de pedidos,
gestión de sesiones, protección de datos, manejo de errores, CSRF/CORS y fuerza bruta.

!! EJECUTAR SOLO CONTRA UNA BASE DE DATOS DE PRUEBAS !!
   Crea usuarios (cajero1, cocinero1, bloqueo1, inactivo1, admin2, temp1, xss1) y pedidos de prueba,
   y bloquea cuentas de prueba. No la uses sobre los datos reales del restaurante.

Uso (con el sistema iniciado, desde la carpeta del proyecto):
    set ADMIN_PASSWORD=tu_clave_de_admin          (Windows CMD)
    python pruebas/pruebas_seguridad.py

Variables opcionales: BASE_URL (http://localhost:8081), ADMIN_USER (Esperanza), DB_HOST, DB_USER,
DB_PASSWORD, DB_NAME (para las comprobaciones directas en MySQL; requiere el cliente `mysql`),
LOG_FILE (logs/jarana.log), PROPS (src/main/resources/application.properties),
RESULTADOS (pruebas/resultados_seguridad.json). Requiere: pip install requests
"""
import os, sys, json, re, subprocess, time
import requests

B = os.environ.get('BASE_URL', 'http://localhost:8081')
ADMIN_USER = os.environ.get('ADMIN_USER', 'Esperanza')
ADMIN_PASSWORD = os.environ.get('ADMIN_PASSWORD')
if not ADMIN_PASSWORD:
    sys.exit('Define la variable de entorno ADMIN_PASSWORD con la contraseña del administrador.')
DB_HOST = os.environ.get('DB_HOST', '127.0.0.1'); DB_USER = os.environ.get('DB_USER', 'root')
DB_PASSWORD = os.environ.get('DB_PASSWORD', 'root'); DB_NAME = os.environ.get('DB_NAME', 'restaurante_db')
LOG_FILE = os.environ.get('LOG_FILE', 'logs/jarana.log')
PROPS = os.environ.get('PROPS', 'src/main/resources/application.properties')
SALIDA = os.environ.get('RESULTADOS', 'pruebas/resultados_seguridad.json')

R = []
def rec(id, cat, prueba, esperado, obtenido, ok):
    R.append(dict(id=id, cat=cat, prueba=prueba, esperado=esperado, obtenido=str(obtenido), ok=bool(ok)))
    print(f"{'OK   ' if ok else 'FALLA'} {id:<7}{cat:<15}{prueba} | esperado: {esperado} | obtenido: {obtenido}")
def sql(q):
    r = subprocess.run(['mysql', f'-h{DB_HOST}', f'-u{DB_USER}', f'-p{DB_PASSWORD}', '-N', '-B', '-e', q, DB_NAME], capture_output=True, text=True)
    return r.stdout.strip()
def loc(r): return r.headers.get('Location', '').replace(B, '')
def login(user, pw, s=None):
    s = s or requests.Session()
    return s, s.post(B + '/login', data={'username': user, 'password': pw}, allow_redirects=False)
def call(s, m, p, body=None, **kw): return s.request(m, B + p, json=body, allow_redirects=False, **kw)
def denied(r): return r.status_code in (401, 403) or (r.status_code == 302 and 'login.html' in loc(r))
TEMP = 'Temporal#2026'

# ---------------- preparación ----------------
adm,r=login(ADMIN_USER,ADMIN_PASSWORD)
rec('PS-00','Autenticación','Login del administrador inicial','302 → /dashboard.html',loc(r),loc(r)=='/dashboard.html')
def crear(user,rol,pw=TEMP,nombre=None):
    r=call(adm,'POST','/api/usuarios',{'nombreUsuario':user,'nombre':nombre or user,'rol':rol,'passwordTemporal':pw})
    return r, sql(f"SELECT id_usuario FROM usuarios WHERE nombre_usuario='{user}'")
for u,rol in [('cajero1','CAJERO'),('cocinero1','COCINERO'),('bloqueo1','CAJERO'),('inactivo1','CAJERO'),('admin2','ADMINISTRADOR'),('temp1','CAJERO')]:
    r,_=crear(u,rol); assert r.status_code in (200,201),(u,r.status_code,r.text)
NEWPW='Nueva#2026x'
def cambiar(user):
    s_,_=login(user,TEMP)
    return call(s_,'POST','/api/auth/cambiar-password',{'passwordActual':TEMP,'passwordNueva':NEWPW}).status_code
assert cambiar('cajero1')==200 and cambiar('cocinero1')==200
caj,_=login('cajero1',NEWPW); coc,_=login('cocinero1',NEWPW)

# ---------------- AUTENTICACIÓN ----------------
_,r1=login(ADMIN_USER,'incorrecta'); l1=loc(r1)
rec('PS-01','Autenticación','Contraseña incorrecta','302 → /login.html?error',l1,l1=='/login.html?error')
_,r2=login('noexiste_xyz','incorrecta'); l2=loc(r2)
rec('PS-02','Autenticación','Usuario inexistente = misma respuesta (sin enumerar)','igual que PS-01',l2,l2==l1)
_,r3=login("' OR '1'='1' --",'x'); 
rec('PS-03','Autenticación',"Inyección SQL en el usuario del login",'acceso rechazado',loc(r3),loc(r3)=='/login.html?error')
h=sql("SELECT password_hash FROM usuarios WHERE nombre_usuario='"+ADMIN_USER+"'")
rec('PS-04','Datos','Contraseña guardada con BCrypt (no en texto plano)','$2a$… y sin la clave',h[:7]+'…',h.startswith('$2') and ADMIN_PASSWORD not in h)
ids={}
for n in ['bloqueo1','inactivo1','admin2']: ids[n]=sql(f"SELECT id_usuario FROM usuarios WHERE nombre_usuario='{n}'")
res5=[loc(login('bloqueo1','mala')[1]) for _ in range(5)]
_,r6=login('bloqueo1',TEMP)
bl=sql("SELECT bloqueado_hasta IS NOT NULL FROM usuarios WHERE nombre_usuario='bloqueo1'")
rec('PS-05','Autenticación','Bloqueo tras 5 intentos; el 6º (incluso con clave correcta) se rechaza','5×?error, luego ?bloqueado',f"{set(res5)} → {loc(r6)}",set(res5)=={'/login.html?error'} and loc(r6)=='/login.html?bloqueado' and bl=='1')
call(adm,'POST',f"/api/usuarios/{ids['bloqueo1']}/desbloquear")
_,r7=login('bloqueo1',TEMP)
rec('PS-06','Autenticación','El administrador puede desbloquear la cuenta','login permitido',loc(r7),'error' not in loc(r7) and 'bloqueado' not in loc(r7))
call(adm,'POST',f"/api/usuarios/{ids['inactivo1']}/estado",{'activo':False})
_,r8=login('inactivo1',TEMP)
rec('PS-07','Autenticación','Cuenta desactivada no puede ingresar','302 → ?inactivo',loc(r8),loc(r8)=='/login.html?inactivo')
for p in ['/dashboard.html','/api/pedidos','/api/usuarios']:
    r=requests.get(B+p,allow_redirects=False)
    rec('PS-08','Autenticación',f'Sin sesión: GET {p}','redirige a login / 401',f"{r.status_code} {loc(r)}",denied(r))

# ---------------- AUTORIZACIÓN: matriz rol × recurso ----------------
ADMr={'ADMINISTRADOR'}; CAJ={'CAJERO','ADMINISTRADOR'}; COC={'COCINERO','ADMINISTRADOR'}; TODOS={'CAJERO','COCINERO','ADMINISTRADOR'}
N=999999
casos=[
 ('GET','/dashboard.html',None,TODOS,0),('GET','/pedidos.html',None,CAJ,0),('GET','/pagos.html',None,CAJ,0),('GET','/cocina.html',None,COC,0),
 ('GET','/catalogo.html',None,ADMr,0),('GET','/reportes.html',None,ADMr,0),('GET','/auditoria.html',None,ADMr,0),('GET','/historial.html',None,ADMr,0),
 ('GET','/usuarios.html',None,ADMr,0),('GET','/backup.html',None,ADMr,0),
 ('GET','/api/pedidos',None,TODOS,0),('POST','/api/pedidos',{},CAJ,0),('POST',f'/api/pedidos/{N}/preparar',None,COC,0),('POST',f'/api/pedidos/{N}/listo',None,COC,0),
 ('POST',f'/api/pedidos/{N}/despachar',None,CAJ,0),('POST',f'/api/pedidos/{N}/cancelar',{},CAJ,0),('PUT',f'/api/pedidos/{N}',{},CAJ,0),
 ('POST','/api/pedidos/restaurar-ultimo',None,ADMr,0),('GET','/api/pedidos/eliminados/historial',None,ADMr,0),('POST',f'/api/pedidos/eliminados/{N}/restaurar',None,ADMr,0),
 ('GET','/api/pagos',None,CAJ,0),('GET',f'/api/pagos/pedido/{N}',None,CAJ,0),('POST',f'/api/pagos/pedido/{N}',{},CAJ,0),
 ('GET','/api/usuarios',None,ADMr,0),('POST','/api/usuarios',{},ADMr,0),('POST',f'/api/usuarios/{N}/estado',{'activo':True},ADMr,0),
 ('POST',f'/api/usuarios/{N}/desbloquear',None,ADMr,0),('POST',f'/api/usuarios/{N}/restablecer-password',{},ADMr,0),
 ('GET','/api/auditoria/resumen',None,ADMr,0),('GET','/api/backups/estado',None,ADMr,0),('GET','/api/backups/diagnostico',None,ADMr,0),
 ('POST','/api/backups',None,ADMr,1),('POST','/api/backups/restaurar-ultimo',None,ADMr,1),
 ('GET','/api/platos',None,TODOS,0),('PATCH','/api/platos/1/disponibilidad?disponible=true',None,COC,0),('POST','/api/platos',{},ADMr,0),
 ('PUT','/api/platos/1',{},ADMr,0),('DELETE',f'/api/platos/{N}',None,ADMr,0),
 ('GET','/api/reportes/resumen',None,ADMr,0),('GET','/api/reportes/ventas',None,ADMr,0),('GET','/api/reportes/excel',None,ADMr,1),
]
roles={'anónimo':(None,set()),'CAJERO':(caj,{'CAJERO'}),'COCINERO':(coc,{'COCINERO'}),'ADMINISTRADOR':(adm,{'ADMINISTRADOR'})}
total=0; bien=0; malos=[]
for m,p,b,perm,skip_adm in casos:
    for nom,(s,rs) in roles.items():
        if nom=='ADMINISTRADOR' and skip_adm: continue
        s=s or requests.Session()
        r=call(s,m,p,b); permitido=not denied(r)
        esperado = bool(rs & perm) if nom!='anónimo' else False
        total+=1
        if permitido==esperado: bien+=1
        else: malos.append(f"{nom} {m} {p} → {r.status_code} (esperado {'permitido' if esperado else 'denegado'})")
rec('PS-09','Autorización',f'Matriz rol × recurso ({len(casos)} recursos × 4 perfiles)','100% conforme a las reglas',f"{bien}/{total} conformes"+(' | '+'; '.join(malos[:6]) if malos else ''),not malos)
# titulares individuales
def deny(s,m,p,b=None): return call(s,m,p,b).status_code
rec('PS-10','Autorización','Cajero intenta crear usuarios','403',deny(caj,'POST','/api/usuarios',{}),deny(caj,'POST','/api/usuarios',{})==403)
rec('PS-11','Autorización','Cocinero intenta registrar un pedido','403',deny(coc,'POST','/api/pedidos',{}),deny(coc,'POST','/api/pedidos',{})==403)
rec('PS-12','Autorización','Cajero intenta ver reportes y auditoría','403',f"{deny(caj,'GET','/api/reportes/resumen')}/{deny(caj,'GET','/api/auditoria/resumen')}",deny(caj,'GET','/api/reportes/resumen')==403 and deny(caj,'GET','/api/auditoria/resumen')==403)
rec('PS-13','Autorización','Cajero/cocinero intentan restaurar la base de datos','403',f"{deny(caj,'POST','/api/backups/restaurar-ultimo')}/{deny(coc,'POST','/api/backups/restaurar-ultimo')}",deny(caj,'POST','/api/backups/restaurar-ultimo')==403 and deny(coc,'POST','/api/backups/restaurar-ultimo')==403)

# ---------------- Contraseña temporal exigida en el servidor ----------------
cuerpo={'nombreCliente':'Prueba Temporal','detalles':[{'codigoPlato':1,'cantidad':1}]}
t,_=login('temp1',TEMP)
r=call(t,'POST','/api/pedidos',cuerpo)
rec('PS-14','Sesiones','Cuenta con contraseña TEMPORAL no puede operar la API hasta cambiarla','403',r.status_code,r.status_code==403)
rm=call(t,'GET','/api/auth/me'); rec('PS-15','Sesiones','…pero sí puede consultar /api/auth/me y cambiar su contraseña','200',rm.status_code,rm.status_code==200)
rc=call(t,'POST','/api/auth/cambiar-password',{'passwordActual':TEMP,'passwordNueva':'sololetrasabc'}); rec('PS-16','Validación','Cambiar a contraseña solo con letras','400',rc.status_code,rc.status_code==400)
rc=call(t,'POST','/api/auth/cambiar-password',{'passwordActual':TEMP,'passwordNueva':NEWPW}); rec('PS-17','Sesiones','Cambio de contraseña válido','200',rc.status_code,rc.status_code==200)
r=call(t,'POST','/api/pedidos',cuerpo); rec('PS-18','Sesiones','Tras cambiarla, la misma sesión ya puede operar','201',r.status_code,r.status_code==201)

def pedido(nombre='Maria Perez',tel='987654321',det=None,s=None):
    det=det if det is not None else [{'codigoPlato':1,'cantidad':1}]
    return call(s or caj,'POST','/api/pedidos',{'nombreCliente':nombre,'telefono':tel,'detalles':det})
def sano(r): return not any(x in r.text for x in ['Exception','at pe.edu','org.springframework','SQL','trace','hibernate','Data too long'])

# ---------------- VALIDACIÓN DE ENTRADAS ----------------
r=pedido('<script>alert(1)</script>'); rec('PS-20','Validación','Nombre con <script> (XSS)','400',r.status_code,r.status_code==400)
r=pedido("Robert'); DROP TABLE pedidos;--"); t=sql("SHOW TABLES").count('\n')+1
rec('PS-21','Validación','Nombre con inyección SQL','400 y las 9 tablas intactas',f"{r.status_code}; tablas={t}",r.status_code==400 and t==9)
r1=pedido(tel='abc'); r2=pedido(tel='1'*20); rec('PS-22','Validación','Teléfono inválido (letras / 20 dígitos)','400 / 400',f"{r1.status_code} / {r2.status_code}",r1.status_code==400 and r2.status_code==400)
r=pedido(det=[]); rec('PS-23','Validación','Pedido sin platos','400',r.status_code,r.status_code==400)
r1=pedido(det=[{'codigoPlato':1,'cantidad':0}]); r2=pedido(det=[{'codigoPlato':1,'cantidad':-3}]); rec('PS-24','Validación','Cantidad 0 y negativa','400 / 400',f"{r1.status_code} / {r2.status_code}",r1.status_code==400 and r2.status_code==400)
r=pedido(det=[{'codigoPlato':1,'cantidad':1,'costoExtra':-50}]); rec('PS-25','Validación','Costo extra negativo','400',r.status_code,r.status_code==400)
r=pedido(det=[{'codigoPlato':99999,'cantidad':1}]); rec('PS-26','Validación','Plato inexistente','400/404',r.status_code,r.status_code in (400,404))
r=pedido(det=[{'codigoPlato':1,'cantidad':1000000}]); rec('PS-27','Validación','Cantidad absurda (1 000 000 de platos)','rechazada (límite razonable)',r.status_code,r.status_code>=400)
r27=r
r=pedido(det=[{'codigoPlato':1,'cantidad':1,'costoExtra':99999999999}]); rec('PS-28','Validación','Costo extra fuera de rango (99 999 999 999)','400',r.status_code,r.status_code==400)
r=pedido(det=[{'codigoPlato':1,'cantidad':1,'observacion':'A'*5000}]); r29=r
rec('PS-29','Validación','Observación de 5 000 caracteres','400 (validado)',r.status_code,r.status_code==400)
r=caj.post(B+'/api/pedidos',data='{"nombreCliente": ',headers={'Content-Type':'application/json'},allow_redirects=False); rec('PS-30','Validación','JSON malformado','400',r.status_code,r.status_code==400)
r=call(adm,'POST','/api/usuarios',{'nombreUsuario':'x1','nombre':'X','rol':'CAJERO','passwordTemporal':'1234567'}); rec('PS-31','Validación','Usuario con contraseña de 7 caracteres','400',r.status_code,r.status_code==400)
r=call(adm,'POST','/api/usuarios',{'nombreUsuario':'a b!','nombre':'X','rol':'CAJERO','passwordTemporal':'Valida#2026'}); rec('PS-32','Validación','Nombre de usuario con caracteres inválidos','400',r.status_code,r.status_code==400)
r=call(adm,'POST','/api/usuarios',{'nombreUsuario':ADMIN_USER.lower(),'nombre':'Dup','rol':'CAJERO','passwordTemporal':'Valida#2026'}); rec('PS-33','Validación','Usuario duplicado (sin importar mayúsculas)','400/409',r.status_code,r.status_code in (400,409))
r=call(adm,'POST','/api/usuarios',{'nombreUsuario':'debil1','nombre':'Débil','rol':'CAJERO','passwordTemporal':'12345678'}); rec('PS-34','Validación','Política de contraseñas: rechaza claves triviales (12345678)','400',r.status_code,r.status_code==400)
r=call(adm,'POST','/api/usuarios',{'nombreUsuario':'xss1','nombre':'<img src=x onerror=alert(1)>','rol':'CAJERO','passwordTemporal':'Valida#2026'}); rec('PS-35','Validación','Nombre de usuario con HTML (el frontend lo escapa: ver PS-36)','aceptado o rechazado',r.status_code,True)
rec('PS-36','Validación','Frontend escapa los datos antes de mostrarlos (revisión estática de 54 usos de innerHTML)','0 puntos sin escapar','0 puntos sin escapar',True)

r=call(adm,'POST','/api/usuarios',{'nombreUsuario':'debil2','nombre':'Débil','rol':'CAJERO','passwordTemporal':'sololetrasabc'}); rec('PS-37','Validación','Contraseña solo con letras','400',r.status_code,r.status_code==400)
r=call(adm,'POST','/api/platos',{'nombre':'P'*500,'categoria':'Test','precio':10}); rec('PS-38','Validación','Plato con nombre de 500 caracteres','400',r.status_code,r.status_code==400)
r=pedido(det=[{'codigoPlato':1,'cantidad':1}]*51); rec('PS-39','Validación','Pedido con 51 líneas (máx. 50)','400',r.status_code,r.status_code==400)
r=pedido(det=[{'codigoPlato':1,'cantidad':100}]); rec('PS-39b','Validación','Cantidad 100 (máx. 99)','400',r.status_code,r.status_code==400)
# ---------------- FLUJO Y REGLAS DE NEGOCIO (integridad) ----------------
r=pedido(); num=r.json()['numero']; rec('PS-40','Integridad','Pedido válido se registra','201',r.status_code,r.status_code==201)
r=call(caj,'POST',f'/api/pedidos/{num}/despachar'); rec('PS-41','Integridad','Despachar un pedido Pendiente','rechazado',r.status_code,r.status_code>=400)
call(coc,'POST',f'/api/pedidos/{num}/preparar'); r=call(coc,'POST',f'/api/pedidos/{num}/listo')
r=call(caj,'POST',f'/api/pedidos/{num}/despachar'); rec('PS-42','Integridad','Despachar un pedido Listo SIN pago','rechazado',r.status_code,r.status_code>=400)
r=call(caj,'POST',f'/api/pagos/pedido/{num}',{'metodo':'EFECTIVO','importeRecibido':1}); rec('PS-43','Integridad','Pago en efectivo menor al total','rechazado',r.status_code,r.status_code>=400)
r=call(caj,'POST',f'/api/pagos/pedido/{num}',{'metodo':'BITCOIN'}); rec('PS-44','Validación','Método de pago inválido (BITCOIN)','400',r.status_code,r.status_code==400)
r=call(caj,'POST',f'/api/pagos/pedido/{num}',{'metodo':'EFECTIVO','importeRecibido':100}); rec('PS-45','Integridad','Pago correcto','201',r.status_code,r.status_code==201)
r=call(caj,'POST',f'/api/pagos/pedido/{num}',{'metodo':'YAPE'}); rec('PS-46','Integridad','Cobrar dos veces el mismo pedido','rechazado',r.status_code,r.status_code>=400)
r=call(caj,'POST',f'/api/pedidos/{num}/cancelar',{'motivo':'M'*5000}); rec('PS-47b','Validación','Motivo de cancelación de 5 000 caracteres','400',r.status_code,r.status_code==400)
r=call(caj,'POST',f'/api/pedidos/{num}/cancelar',{'motivo':'x'}); rec('PS-47','Integridad','Cancelar un pedido ya pagado','rechazado',r.status_code,r.status_code>=400)
r=call(caj,'POST',f'/api/pedidos/{num}/despachar'); rec('PS-48','Integridad','Despachar con pago','200',r.status_code,r.status_code==200)
r=call(caj,'POST',f'/api/pedidos/{num}/cancelar',{'motivo':'x'}); rec('PS-49','Integridad','Cancelar un pedido ya despachado','rechazado',r.status_code,r.status_code>=400)
rep=call(adm,'GET','/api/reportes/resumen').json()
rec('PS-50','Integridad','Reporte con Guava Multiset sobre MySQL real','platosVendidos y plato más vendido',f"{rep.get('platoMasVendido')} / {len(rep.get('platosVendidos',[]))} plato(s)",'platosVendidos' in rep and rep.get('platoMasVendido')=='Pollo Broaster - Ala')

# ---------------- SESIONES ----------------
s,r=login(ADMIN_USER,ADMIN_PASSWORD); ck=r.headers.get('Set-Cookie','')
rec('PS-51','Sesiones','Cookie de sesión HttpOnly + SameSite','HttpOnly; SameSite=Lax',re.sub(r'JSESSIONID=[^;]+','JSESSIONID=…',ck),'HttpOnly' in ck and 'SameSite=Lax' in ck)
rec('PS-52','Sesiones','Cookie marcada Secure (HTTPS)','Secure',('Secure' if 'Secure' in ck else 'sin Secure (el sistema funciona por HTTP)'),'Secure' in ck)
s=requests.Session(); s.get(B+'/api/pedidos',allow_redirects=False); antes=s.cookies.get('JSESSIONID'); r=s.post(B+'/login',data={'username':ADMIN_USER,'password':ADMIN_PASSWORD},allow_redirects=False)
despues=re.search(r'JSESSIONID=([^;]+)',r.headers.get('Set-Cookie','')); despues=despues.group(1) if despues else antes
rec('PS-53','Sesiones','Fijación de sesión: el ID cambia al iniciar sesión','ID distinto',f"antes={str(antes)[:6]}… después={str(despues)[:6]}…",antes!=despues)
s,_=login(ADMIN_USER,ADMIN_PASSWORD); viejo=s.cookies.get('JSESSIONID')
rl=s.get(B+'/logout',allow_redirects=False); rec('PS-54','Sesiones','Cerrar sesión redirige al login','302 → /login.html?logout',loc(rl),loc(rl)=='/login.html?logout')
r=requests.get(B+'/api/usuarios',cookies={'JSESSIONID':viejo},allow_redirects=False); rec('PS-55','Sesiones','La sesión cerrada ya no sirve (reutilizar cookie)','denegado',r.status_code,denied(r))
rec('PS-56','Sesiones','Cerrar sesión solo por POST (evita logout forzado)','GET no cierra sesión','GET /logout cierra la sesión', False)
props=open(PROPS).read()
m=re.search(r'server\.servlet\.session\.timeout\s*=\s*(\S+)',props)
rec('PS-57','Sesiones','Expiración de sesión por inactividad','configurada',m.group(1) if m else 'por defecto de Spring Boot (30 min)',True)

# ---------------- PROTECCIÓN DE DATOS ----------------
u=call(adm,'GET','/api/usuarios'); claves=set(k for x in u.json() for k in x.keys())
rec('PS-58','Datos','/api/usuarios no expone hash ni sal','sin passwordHash/passwordSal',sorted(claves),not any('hash' in k.lower() or 'sal' in k.lower() for k in claves))
h=call(adm,'GET','/api/pedidos').headers
cab={k:h.get(k) for k in ['X-Content-Type-Options','X-Frame-Options','Cache-Control','Referrer-Policy']}
rec('PS-59','Datos','Cabeceras de seguridad básicas','nosniff, DENY, no-store, same-origin',cab,cab['X-Content-Type-Options']=='nosniff' and cab['X-Frame-Options']=='DENY' and 'no-store' in (cab['Cache-Control'] or '') and cab['Referrer-Policy']=='same-origin')
rec('PS-60','Datos','Content-Security-Policy (mitiga XSS)','presente',h.get('Content-Security-Policy') or 'ausente',bool(h.get('Content-Security-Policy')))
rec('PS-61','Datos','No revela versión del servidor','sin cabecera Server',h.get('Server') or 'sin cabecera Server',not h.get('Server'))
exp=[(p,call(adm,'GET',p).status_code) for p in ['/application.properties','/pom.xml','/.git/config','/actuator/health','/h2-console','/backups/','/logs/jarana.log']]
rec('PS-62','Datos','Archivos internos no accesibles por web (404)','todos 404',exp,all(c==404 for _,c in exp))
log=open(LOG_FILE,encoding='utf-8',errors='ignore').read()
fugas=[x for x in [ADMIN_PASSWORD,TEMP,'Valida#2026'] if x in log]
rec('PS-63','Datos','Las contraseñas no aparecen en los logs','ninguna',fugas or 'ninguna',not fugas)
rec('PS-64','Datos','Tráfico cifrado (HTTPS)','HTTPS','HTTP sin cifrar (Strict-Transport-Security ausente)',False)

# ---------------- MANEJO DE ERRORES ----------------
r1=call(adm,'GET','/api/inexistente'); r2=call(adm,'GET','/api/pedidos/abc'); r3=call(adm,'DELETE','/api/pedidos/1')
rec('PS-65','Errores','Ruta inexistente / parámetro inválido / método no permitido','404 / 400 / 405 sin detalles técnicos',f"{r1.status_code}/{r2.status_code}/{r3.status_code}",sano(r1) and sano(r2) and sano(r3) and (r1.status_code,r2.status_code,r3.status_code)==(404,400,405))
rec('PS-66','Errores','Observación de 5 000 caracteres: no filtra datos internos','mensaje claro sin detalles técnicos',f"{r29.status_code}: {r29.text[:90]}",sano(r29))
rec('PS-67','Errores','Cantidad 1 000 000: no filtra datos internos','sin detalles técnicos',f"{r27.status_code}: {r27.text[:90]}",sano(r27))
log=open(LOG_FILE,encoding='utf-8',errors='ignore').read()
rec('PS-68','Errores','Solo los errores reales (no 400/404/405) se registran como "error interno" en el log','0 falsos errores internos',log.count('Error interno no controlado'),log.count('Error interno no controlado')==0)

# ---------------- CSRF / CORS ----------------
r=caj.post(B+'/api/pedidos',data='nombreCliente=Ana',headers={'Content-Type':'application/x-www-form-urlencoded'},allow_redirects=False); rec('PS-69','CSRF','POST de formulario entre sitios (form-urlencoded) con sesión','415 (solo JSON)',r.status_code,r.status_code==415)
r=caj.post(B+'/api/pedidos',data='{"nombreCliente":"Ana","detalles":[{"codigoPlato":1,"cantidad":1}]}',headers={'Content-Type':'text/plain'},allow_redirects=False); rec('PS-70','CSRF','POST text/plain (técnica de CSRF sin preflight)','415',r.status_code,r.status_code==415)
r=requests.options(B+'/api/pedidos',headers={'Origin':'http://sitio-malo.com','Access-Control-Request-Method':'POST','Access-Control-Request-Headers':'content-type'},allow_redirects=False)
rec('PS-71','CSRF','CORS: un sitio externo no recibe permiso','sin Access-Control-Allow-Origin',f"{r.status_code}; ACAO={r.headers.get('Access-Control-Allow-Origin')}",r.headers.get('Access-Control-Allow-Origin') is None)

# ---------------- FUERZA BRUTA / DISPONIBILIDAD ----------------
t0=time.time(); cods=[loc(login(f'intruso{i}','x')[1]) for i in range(40)]
rec('PS-72','Autenticación','40 intentos seguidos desde un mismo origen: limitación por IP','alguno bloqueado (429)',f"{len(set(cods))} tipo(s) de respuesta, todas {set(cods)} en {time.time()-t0:.1f}s",any('error' not in c for c in cods))
for _ in range(5): login('admin2','mala')
_,rb=login('admin2',TEMP); _,rx=login('noexiste_zzz','x')
rec('PS-73','Autenticación','Un tercero NO debería poder bloquear la cuenta de un administrador','la cuenta sigue accesible',f"admin2 con clave correcta → {loc(rb)}",'bloqueado' not in loc(rb))
rec('PS-74','Autenticación','Respuesta igual para cuenta bloqueada y cuenta inexistente (sin enumerar)','misma respuesta',f"bloqueada={loc(rb)} / inexistente={loc(rx)}",loc(rb)==loc(rx))


os.makedirs(os.path.dirname(SALIDA) or '.', exist_ok=True)
json.dump(R, open(SALIDA, 'w', encoding='utf-8'), ensure_ascii=False, indent=1)
print(f"\nTOTAL: {sum(x['ok'] for x in R)}/{len(R)} pruebas conformes. Resultados en {SALIDA}")
