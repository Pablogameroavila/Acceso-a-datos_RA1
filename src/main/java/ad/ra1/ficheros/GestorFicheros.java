package ad.ra1.ficheros;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Gestión de ficheros y directorios con {@code java.nio.file} (criterio a).
 *
 * <p>Se usa {@link Path} para representar rutas y {@link Files} para operar con ellas.
 * Los métodos lanzan {@link IOException} (o una subclase, como {@code NoSuchFileException})
 * y dejan que quien los llama decida qué hacer con el error.</p>
 */
public final class GestorFicheros {

    /** Crea un directorio y, si hace falta, los que falten por encima. No falla si ya existe. */
    public Path crearDirectorio(Path directorio) throws IOException {
        return Files.createDirectories(directorio);
    }

    /**
     * Crea un fichero vacío (y los directorios que falten).
     *
     * @throws java.nio.file.FileAlreadyExistsException si el fichero ya existe
     */
    public Path crearFichero(Path fichero) throws IOException {
        Path padre = fichero.toAbsolutePath().getParent();
        if (padre != null) {
            Files.createDirectories(padre);
        }
        return Files.createFile(fichero);
    }

    /**
     * Copia un fichero.
     *
     * @param sobrescribir si es {@code false} y el destino existe, lanza {@code FileAlreadyExistsException}
     */
    public Path copiar(Path origen, Path destino, boolean sobrescribir) throws IOException {
        if (sobrescribir) {
            return Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        return Files.copy(origen, destino);
    }

    /** Mueve (o renombra) un fichero o directorio. */
    public Path mover(Path origen, Path destino, boolean sobrescribir) throws IOException {
        if (sobrescribir) {
            return Files.move(origen, destino, StandardCopyOption.REPLACE_EXISTING);
        }
        return Files.move(origen, destino);
    }

    /**
     * Elimina un fichero o un directorio VACÍO.
     *
     * @return {@code true} si existía y se ha borrado
     * @throws java.nio.file.DirectoryNotEmptyException si es un directorio con contenido
     */
    public boolean eliminar(Path ruta) throws IOException {
        return Files.deleteIfExists(ruta);
    }

    /** Elimina un directorio con todo su contenido (¡sin papelera!). Si no existe, no hace nada. */
    public void eliminarRecursivo(Path raiz) throws IOException {
        if (!Files.exists(raiz)) {
            return;
        }
        Files.walkFileTree(raiz, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.delete(dir); // el directorio ya está vacío
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Lista el contenido directo de un directorio, ordenado por nombre. */
    public List<Path> listar(Path directorio) throws IOException {
        try (Stream<Path> flujo = Files.list(directorio)) { // el Stream mantiene el directorio abierto: se cierra
            return flujo.sorted().toList();
        }
    }

    /** Busca, solo en el directorio indicado, los ficheros cuyo nombre cumple un patrón glob (p. ej. {@code *.xml}). */
    public List<Path> buscar(Path directorio, String patronGlob) throws IOException {
        List<Path> resultado = new ArrayList<>();
        try (DirectoryStream<Path> flujo = Files.newDirectoryStream(directorio, patronGlob)) {
            for (Path p : flujo) {
                resultado.add(p);
            }
        }
        resultado.sort(null);
        return resultado;
    }

    /** Busca en el directorio y en todos sus subdirectorios los ficheros cuyo nombre cumple el patrón glob. */
    public List<Path> buscarRecursivo(Path directorio, String patronGlob) throws IOException {
        PathMatcher comparador = FileSystems.getDefault().getPathMatcher("glob:" + patronGlob);
        try (Stream<Path> flujo = Files.walk(directorio)) {
            return flujo.filter(Files::isRegularFile)
                    .filter(p -> comparador.matches(p.getFileName()))
                    .sorted()
                    .toList();
        }
    }

    /** Suma el tamaño en bytes de todos los ficheros de un directorio y sus subdirectorios. */
    public long tamanoTotal(Path directorio) throws IOException {
        try (Stream<Path> flujo = Files.walk(directorio)) {
            long total = 0;
            for (Path p : (Iterable<Path>) flujo.filter(Files::isRegularFile)::iterator) {
                total += Files.size(p);
            }
            return total;
        }
    }

    /** Devuelve un texto con los atributos principales de un fichero o directorio. */
    public String describir(Path ruta) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(ruta, BasicFileAttributes.class);
        LocalDateTime modificado = LocalDateTime.ofInstant(attrs.lastModifiedTime().toInstant(), ZoneId.systemDefault());
        return "Nombre:       " + ruta.getFileName() + "\n"
                + "Tipo:         " + (attrs.isDirectory() ? "directorio" : "fichero") + "\n"
                + "Tamaño:       " + attrs.size() + " bytes\n"
                + "Modificado:   " + modificado.withNano(0) + "\n"
                + "Legible:      " + Files.isReadable(ruta) + "\n"
                + "Escribible:   " + Files.isWritable(ruta);
    }

    /**
     * Resuelve una ruta relativa dentro de un directorio base y COMPRUEBA que no se sale de él.
     * Evita los ataques de «path traversal» (rutas como {@code ../../secreto.txt}).
     *
     * @throws IllegalArgumentException si la ruta resultante queda fuera del directorio base
     */
    public Path resolverSeguro(Path base, String relativa) {
        Path baseNormal = base.toAbsolutePath().normalize();
        Path resultado = baseNormal.resolve(relativa).normalize();
        if (!resultado.startsWith(baseNormal)) {
            throw new IllegalArgumentException("La ruta «" + relativa + "» se sale del directorio permitido");
        }
        return resultado;
    }

    // ----------------------------------------------------------- escritura segura (se usa desde XML)

    /** Crea un fichero temporal en el MISMO directorio que el destino (condición para poder moverlo de forma atómica). */
    public Path crearTemporalJuntoA(Path destino) throws IOException {
        Path padre = destino.toAbsolutePath().getParent();
        Files.createDirectories(padre);
        return Files.createTempFile(padre, "tmp-", ".part");
    }

    /**
     * Sustituye el destino por el temporal. Si el sistema de ficheros lo permite, la sustitución es atómica:
     * quien lea el destino verá el contenido antiguo o el nuevo completo, nunca un fichero a medias.
     */
    public void reemplazar(Path temporal, Path destino) throws IOException {
        try {
            Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporal, destino, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
