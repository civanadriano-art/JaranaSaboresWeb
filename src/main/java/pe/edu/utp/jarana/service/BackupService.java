package pe.edu.utp.jarana.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.utp.jarana.exception.ReglaNegocioException;

@Service
public class BackupService {

    private final EstructurasDatosService estructuras;

    public BackupService(EstructurasDatosService estructuras) {
        this.estructuras = estructuras;
    }

    private static final DateTimeFormatter NOMBRE_FECHA = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final Pattern JDBC_MYSQL = Pattern.compile("jdbc:mysql://([^:/?#]+)(?::(\\d+))?/([^?]+)");

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    @Value("${app.backup.directory:backups}")
    private String backupDirectory;

    @Value("${app.backup.mysqldump:mysqldump}")
    private String mysqldumpCommand;

    @Value("${app.backup.mysql:mysql}")
    private String mysqlCommand;

    public Map<String, Object> estado() {
        Path ultimo = ultimoBackupManual();
        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("disponible", ultimo != null);
        respuesta.put("directorio", directorio().toAbsolutePath().normalize().toString());
        if (ultimo != null) {
            respuesta.putAll(infoArchivo(ultimo));
        }
        return respuesta;
    }


    public Map<String, Object> diagnostico() {
        ConfigDb db = configDb();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("host", db.host());
        r.put("puerto", db.port());
        r.put("baseDatos", db.database());
        r.put("directorio", directorio().toAbsolutePath().normalize().toString());
        r.put("mysqldump", verificarHerramienta(mysqldumpCommand, "mysqldump"));
        r.put("mysql", verificarHerramienta(mysqlCommand, "mysql"));
        r.put("listo", Boolean.TRUE.equals(((Map<?,?>) r.get("mysqldump")).get("disponible"))
                && Boolean.TRUE.equals(((Map<?,?>) r.get("mysql")).get("disponible")));
        return r;
    }

    private Map<String, Object> verificarHerramienta(String configurado, String base) {
        Map<String, Object> info = new LinkedHashMap<>();
        try {
            String ejecutable = resolverEjecutable(configurado, base);
            ProcessBuilder pb = new ProcessBuilder(ejecutable, "--version");
            pb.redirectErrorStream(true);
            Process proceso = pb.start();
            boolean termino = proceso.waitFor(5, TimeUnit.SECONDS);
            if (!termino) {
                proceso.destroyForcibly();
                info.put("disponible", false);
                info.put("detalle", "La verificación excedió 5 segundos");
                return info;
            }
            String salida = new String(proceso.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            info.put("disponible", proceso.exitValue() == 0);
            info.put("ejecutable", ejecutable);
            info.put("version", salida.length() > 180 ? salida.substring(0, 180) : salida);
        } catch (Exception e) {
            info.put("disponible", false);
            info.put("detalle", e.getMessage());
        }
        return info;
    }

    public Map<String, Object> crearBackup() {
        Path archivo = crearBackupInterno("backup");
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("mensaje", "Copia de seguridad creada correctamente");
        r.putAll(infoArchivo(archivo));
        return r;
    }

    public Map<String, Object> restaurarUltimoBackup() {
        Path objetivo = ultimoBackupManual();
        if (objetivo == null) {
            throw new ReglaNegocioException("No existe una copia de seguridad disponible para restaurar");
        }

        // Copia preventiva del estado actual. No se usa como objetivo de esta restauración.
        Path preventivo = crearBackupInterno("pre_restauracion");
        restaurar(objetivo);
        estructuras.cargarDesdeBaseDatos();

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("mensaje", "Base de datos restaurada correctamente");
        r.put("archivoRestaurado", objetivo.getFileName().toString());
        r.put("backupPreventivo", preventivo.getFileName().toString());
        return r;
    }

    private Path crearBackupInterno(String prefijo) {
        ConfigDb db = configDb();
        Path dir = directorio();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo crear la carpeta de backups: " + e.getMessage());
        }

        String nombre = prefijo + "_" + db.database() + "_" + LocalDateTime.now().format(NOMBRE_FECHA) + ".sql";
        Path salida = dir.resolve(nombre).toAbsolutePath().normalize();
        Path error = dir.resolve(nombre + ".err").toAbsolutePath().normalize();

        String ejecutable = resolverEjecutable(mysqldumpCommand, "mysqldump");
        List<String> comando = new ArrayList<>();
        comando.add(ejecutable);
        comando.add("--host=" + db.host());
        comando.add("--port=" + db.port());
        comando.add("--user=" + dbUser);
        comando.add("--single-transaction");
        comando.add("--routines");
        comando.add("--triggers");
        comando.add("--events");
        comando.add("--default-character-set=utf8mb4");
        comando.add(db.database());

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.environment().put("MYSQL_PWD", dbPassword == null ? "" : dbPassword);
        pb.redirectOutput(salida.toFile());
        pb.redirectError(error.toFile());

        int codigo = ejecutar(pb, "crear el backup");
        if (codigo != 0) {
            String detalle = leerError(error);
            borrarSilencioso(salida);
            throw new ReglaNegocioException("No se pudo crear el backup" + (detalle.isBlank() ? "" : ": " + detalle));
        }

        borrarSilencioso(error);
        try {
            if (!Files.exists(salida) || Files.size(salida) == 0) {
                borrarSilencioso(salida);
                throw new ReglaNegocioException("MySQL no generó un archivo de backup válido");
            }
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo validar el archivo de backup: " + e.getMessage());
        }
        return salida;
    }

    private void restaurar(Path archivo) {
        ConfigDb db = configDb();
        Path error = directorio().resolve("restauracion_" + System.currentTimeMillis() + ".err").toAbsolutePath().normalize();
        String ejecutable = resolverEjecutable(mysqlCommand, "mysql");

        List<String> comando = new ArrayList<>();
        comando.add(ejecutable);
        comando.add("--host=" + db.host());
        comando.add("--port=" + db.port());
        comando.add("--user=" + dbUser);
        comando.add("--default-character-set=utf8mb4");
        comando.add(db.database());

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.environment().put("MYSQL_PWD", dbPassword == null ? "" : dbPassword);
        pb.redirectInput(archivo.toFile());
        pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
        pb.redirectError(error.toFile());

        int codigo = ejecutar(pb, "restaurar el backup");
        if (codigo != 0) {
            String detalle = leerError(error);
            throw new ReglaNegocioException("No se pudo restaurar el backup" + (detalle.isBlank() ? "" : ": " + detalle));
        }
        borrarSilencioso(error);
    }

    private int ejecutar(ProcessBuilder pb, String accion) {
        try {
            Process proceso = pb.start();
            return proceso.waitFor();
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo " + accion + ". Verifica que las herramientas de MySQL estén instaladas. Detalle: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ReglaNegocioException("La operación fue interrumpida");
        }
    }

    private ConfigDb configDb() {
        Matcher m = JDBC_MYSQL.matcher(jdbcUrl);
        if (!m.find()) {
            throw new ReglaNegocioException("No se pudo interpretar la URL de conexión MySQL");
        }
        String host = m.group(1);
        String port = m.group(2) == null ? "3306" : m.group(2);
        String database = m.group(3);
        return new ConfigDb(host, port, database);
    }

    private Path ultimoBackupManual() {
        Path dir = directorio();
        if (!Files.isDirectory(dir)) return null;
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith("backup_"))
                    .filter(p -> p.getFileName().toString().endsWith(".sql"))
                    .max(Comparator.comparingLong(this::ultimaModificacion))
                    .orElse(null);
        } catch (IOException e) {
            throw new ReglaNegocioException("No se pudo leer la carpeta de backups: " + e.getMessage());
        }
    }

    private long ultimaModificacion(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }

    private Map<String, Object> infoArchivo(Path archivo) {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("archivo", archivo.getFileName().toString());
        try {
            info.put("tamanoBytes", Files.size(archivo));
            info.put("fechaModificacion", Files.getLastModifiedTime(archivo).toInstant().toString());
        } catch (IOException e) {
            info.put("tamanoBytes", 0L);
            info.put("fechaModificacion", null);
        }
        return info;
    }

    private Path directorio() {
        return Paths.get(backupDirectory);
    }

    private String resolverEjecutable(String configurado, String base) {
        if (configurado != null && !configurado.isBlank() && (configurado.contains("\\") || configurado.contains("/") || Paths.get(configurado).isAbsolute())) {
            Path p = Paths.get(configurado);
            if (Files.isRegularFile(p)) return p.toAbsolutePath().toString();
            throw new ReglaNegocioException("No se encontró " + base + " en: " + configurado);
        }

        String nombre = (configurado == null || configurado.isBlank()) ? base : configurado;
        String path = System.getenv("PATH");
        if (path != null) {
            for (String dir : path.split(Pattern.quote(File.pathSeparator))) {
                Path candidato = Paths.get(dir, nombre);
                if (Files.isRegularFile(candidato)) return candidato.toAbsolutePath().toString();
                if (esWindows() && !nombre.toLowerCase().endsWith(".exe")) {
                    Path exe = Paths.get(dir, nombre + ".exe");
                    if (Files.isRegularFile(exe)) return exe.toAbsolutePath().toString();
                }
            }
        }

        if (esWindows()) {
            String[] raices = {System.getenv("ProgramFiles"), System.getenv("ProgramFiles(x86)")};
            for (String raiz : raices) {
                if (raiz == null || raiz.isBlank()) continue;
                Path mysqlRoot = Paths.get(raiz, "MySQL");
                if (!Files.isDirectory(mysqlRoot)) continue;
                try (Stream<Path> s = Files.walk(mysqlRoot, 5)) {
                    Path encontrado = s.filter(Files::isRegularFile)
                            .filter(p -> p.getFileName().toString().equalsIgnoreCase(base + ".exe"))
                            .findFirst().orElse(null);
                    if (encontrado != null) return encontrado.toAbsolutePath().toString();
                } catch (IOException ignored) {
                    // Se probará el nombre configurado como último recurso.
                }
            }
        }

        return nombre;
    }

    private boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private String leerError(Path error) {
        try {
            if (!Files.exists(error)) return "";
            String s = Files.readString(error, StandardCharsets.UTF_8).trim();
            return s.length() > 500 ? s.substring(0, 500) : s;
        } catch (IOException e) {
            return "";
        } finally {
            borrarSilencioso(error);
        }
    }

    private void borrarSilencioso(Path p) {
        try { Files.deleteIfExists(p); } catch (IOException ignored) { }
    }

    private record ConfigDb(String host, String port, String database) { }
}
