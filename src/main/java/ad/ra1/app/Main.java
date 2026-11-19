package ad.ra1.app;

import ad.ra1.ficheros.AccesoFicheros;
import ad.ra1.ficheros.BenchmarkLectura;
import ad.ra1.ficheros.GestorFicheros;
import ad.ra1.modelo.AlumnoDam;
import ad.ra1.servicio.ClaseDamException;
import ad.ra1.servicio.ServicioClaseDam;
import ad.ra1.xml.ConversorXml;
import ad.ra1.xml.EscritorXml;
import ad.ra1.xml.LectorXml;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Menú de demostración del proyecto RA1 - Clase de 2º de DAM.
 * Cada opción indica el criterio del RA1 que ilustra.
 */
public class Main {

    // Usa tu ruta en Ubuntu si existe; si se ejecuta en otro ordenador, usa la ruta relativa del proyecto
    private static final Path RAIZ = Files.exists(Path.of("/home/pablo/Documentos/2DAM/Acceso a datos/RA1"))
            ? Path.of("/home/pablo/Documentos/2DAM/Acceso a datos/RA1")
            : Path.of(".");

    private static final Path DATOS = RAIZ.resolve("datos");
    private static final Path SALIDA = RAIZ.resolve("salida");

    private static final GestorFicheros gestor = new GestorFicheros();
    private static final AccesoFicheros acceso = new AccesoFicheros();
    private static final ServicioClaseDam servicioDam = new ServicioClaseDam();

    public static void main(String[] args) {
        Scanner teclado = new Scanner(System.in);
        boolean salir = false;
        while (!salir) {
            System.out.println();
            System.out.println("===== CLASE 2º DAM - RA1 Acceso a Datos =====");
            System.out.println("1. Ficheros y directorios                  (criterio a)");
            System.out.println("2. Formas de acceso: texto, binario, azar  (criterio b)");
            System.out.println("3. Leer XML: DOM, SAX, StAX y XPath        (criterio c)");
            System.out.println("4. Escribir y modificar XML                (criterio d)");
            System.out.println("5. Convertir XML a HTML, CSV y JSON        (criterio e)");
            System.out.println("6. Excepciones: casos de error             (criterio f)");
            System.out.println("7. Comparar velocidades de lectura         (criterio b)");
            System.out.println("0. Salir");
            System.out.print("Elige: ");
            String opcion = teclado.nextLine().trim();
            try {
                switch (opcion) {
                    case "1" -> demoFicheros(teclado);
                    case "2" -> demoAcceso();
                    case "3" -> demoLecturaXml();
                    case "4" -> demoEscrituraXml();
                    case "5" -> demoConversion();
                    case "6" -> demoExcepciones();
                    case "7" -> BenchmarkLectura.ejecutar(200_000, System.out);
                    case "0" -> salir = true;
                    default -> System.out.println("Opción no válida.");
                }
            } catch (IOException | RuntimeException e) {
                System.out.println("[ERROR] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("[ERROR INESPERADO] " + e);
            }
        }
        System.out.println("Fin.");
    }

    // ------------------------------------------------------------------ a (Adaptado a 2º DAM)
    private static void demoFicheros(Scanner teclado) throws IOException {
        Path base = SALIDA.resolve("clase_2dam");
        Path informes2026 = base.resolve(Path.of("informes", "2026"));
        gestor.eliminarRecursivo(base); // empezamos limpios

        System.out.println("-- Crear directorios y ficheros (2º DAM) --");
        gestor.crearDirectorio(informes2026);

        Path moduloXml = base.resolve("acceso_datos.xml");
        Files.writeString(moduloXml, "<modulo curso=\"2DAM\"><nombre>Acceso a Datos</nombre><horas>120</horas></modulo>\n");

        Path alumnosTxt = base.resolve("alumnos_2dam.txt");
        Files.writeString(alumnosTxt, "01;Pablo;2DAM\n02;Lucía;2DAM\n");
        System.out.println("Creado en: " + base);

        System.out.println("\n-- Copiar y mover --");
        Path copiaXml = informes2026.resolve("acta_ad_2026.xml");
        gestor.copiar(moduloXml, copiaXml, false);

        Path copiaTxt = informes2026.resolve("alumnos-copia.txt");
        gestor.copiar(alumnosTxt, copiaTxt, false);
        gestor.mover(copiaTxt, informes2026.resolve("alumnos-archivados.txt"), false);

        System.out.println("\n-- Listar y buscar --");
        gestor.listar(informes2026).forEach(p -> System.out.println("  " + p.getFileName() + (Files.isDirectory(p) ? "/" : "")));
        System.out.println("Ficheros *.xml (recursivo): " + gestor.buscarRecursivo(base, "*.xml"));

        System.out.println("\n-- Atributos --");
        System.out.println(gestor.describir(copiaXml));
        System.out.println("Tamaño total del directorio: " + gestor.tamanoTotal(base) + " bytes");

        System.out.println("\n-- Ruta segura --");
        System.out.println("Permitida:  " + gestor.resolverSeguro(base, "informes/2026/acta_ad_2026.xml").getFileName());
        try {
            gestor.resolverSeguro(base, "../../examen_secreto.txt");
        } catch (IllegalArgumentException e) {
            System.out.println("Rechazada:  " + e.getMessage());
        }

        // PAUSA PARA QUE HAGAS LA CAPTURA 'AD-1.a_antes.png'
        System.out.println("\n[PAUSA] Abre en tu explorador: " + informes2026);
        System.out.print("Haz la captura 'AD-1.a_antes.png' y pulsa ENTER para continuar y limpiar...");
        teclado.nextLine();

        System.out.println("\n-- Eliminar --");
        try {
            gestor.eliminar(base); // el directorio no está vacío
        } catch (java.nio.file.DirectoryNotEmptyException e) {
            System.out.println("eliminar() no borra un directorio con contenido (DirectoryNotEmptyException).");
        }
        gestor.eliminarRecursivo(base);
        System.out.println("¿Existe todavía 'clase_2dam'? " + Files.exists(base));
    }

    // ------------------------------------------------------------------ b (Adaptado a 2º DAM)
    private static void demoAcceso() throws IOException {
        Files.createDirectories(SALIDA);
        Path csv = DATOS.resolve("alumnos_2dam.csv");
        Path binario = SALIDA.resolve("alumnos_2dam.dat");
        Path texto = SALIDA.resolve("registro_asistencia.txt");

        System.out.println("-- Texto secuencial: leer el CSV de 2º DAM --");
        List<AlumnoDam> alumnos = acceso.leerCsv(csv);
        alumnos.forEach(a -> System.out.println("  " + a));

        System.out.println("\n-- Texto: escribir y añadir al final --");
        acceso.escribirLineas(texto, List.of("Pasada de lista inicial - 2º DAM"), false);
        acceso.escribirLineas(texto, List.of("Actualización de faltas ejecutada"), true);
        System.out.println("  " + acceso.leerLineasEnMemoria(texto));

        System.out.println("\n-- Binario secuencial --");
        acceso.escribirBinario(binario, alumnos);
        System.out.println("  " + alumnos.size() + " alumnos × " + AccesoFicheros.TAM_REGISTRO
                + " bytes = " + Files.size(binario) + " bytes");
        System.out.println("  Leídos de nuevo: " + acceso.leerBinario(binario).size());

        System.out.println("\n-- Binario aleatorio: ir directo al registro 2 (Carlos Mendoza) --");
        System.out.println("  Antes:   " + acceso.leerRegistro(binario, 2));
        acceso.actualizarHorasFaltas(binario, 2, 14);
        System.out.println("  Después: " + acceso.leerRegistro(binario, 2));
    }

    // ------------------------------------------------------------------ c (Adaptado a 2º DAM)
    private static void demoLecturaXml() throws Exception {
        Path xmlClase = DATOS.resolve("clase_2dam.xml");
        LectorXml lector = new LectorXml();

        System.out.println("-- XPath 1 (atributo): Fecha del listado de 2º DAM -> " + lector.leerFechaClase(xmlClase));

        List<AlumnoDam> dom = lector.leerAlumnosConDom(xmlClase);
        List<AlumnoDam> sax = lector.leerAlumnosConSax(xmlClase);
        List<AlumnoDam> stax = lector.leerAlumnosConStax(xmlClase);

        System.out.println("DOM: " + dom.size() + " alumnos | SAX: " + sax.size() + " | StAX: " + stax.size());
        System.out.println("¿Los tres métodos devuelven exactamente lo mismo? " + (dom.equals(sax) && sax.equals(stax)));
        dom.forEach(a -> System.out.println("  " + a));

        System.out.println("\n-- XPath 2 (condición numérica con variable $min > 8.0) --");
        lector.filtrarPorNotaMinima(xmlClase, new BigDecimal("8.0"))
                .forEach(a -> System.out.println("  " + a.nombre() + " (" + a.modulo() + " -> Nota: " + a.notaMedia() + ")"));
    }

    // ------------------------------------------------------------------ d (Adaptado a 2º DAM)
    private static void demoEscrituraXml() throws Exception {
        Files.createDirectories(SALIDA);
        Path xmlOrigen = DATOS.resolve("clase_2dam.xml");
        LectorXml lector = new LectorXml();
        EscritorXml escritor = new EscritorXml();

        List<AlumnoDam> alumnos = lector.leerAlumnosConDom(xmlOrigen);

        Path conDom = SALIDA.resolve("clase_2dam-dom.xml");
        Path conStax = SALIDA.resolve("clase_2dam-stax.xml");

        // 1. Escribir con DOM y con StAX y verificar que se leen igual
        escritor.escribirAlumnosConDom(conDom, LocalDate.of(2026, 11, 19), alumnos);
        escritor.escribirAlumnosConStax(conStax, LocalDate.of(2026, 11, 19), alumnos);
        System.out.println("Escritos: " + conDom.getFileName() + " y " + conStax.getFileName());
        System.out.println("¿Ambos se leen igual? "
                + lector.leerAlumnosConDom(conDom).equals(lector.leerAlumnosConStax(conStax)));

        System.out.println("\n--- CONTENIDO ANTES DE MODIFICAR (clase_2dam-dom.xml) ---");
        System.out.println(Files.readString(conDom));

        // 2. Añadir alumno con caracteres especiales (&, <, "), modificar faltas y eliminar
        AlumnoDam nuevoConEspeciales = new AlumnoDam(
                7,
                "Sofía \"La Pro\" & Cía <2DAM>",
                "Acceso a Datos & XML",
                new BigDecimal("9.85"),
                0);
        escritor.anadirAlumno(conDom, nuevoConEspeciales);
        escritor.actualizarFaltasAlumno(conDom, 3, 15); // Carlos Mendoza pasa de 8 a 15 faltas
        escritor.eliminarAlumno(conDom, 2);             // Eliminamos a Lucía Gómez (id=2)

        // Comprobar gestión de errores (duplicado e inexistente)
        try {
            escritor.anadirAlumno(conDom, nuevoConEspeciales);
        } catch (IllegalArgumentException e) {
            System.out.println("[Control duplicado] " + e.getMessage());
        }
        System.out.println("[Control inexistente] ¿Modificado id 999? " + escritor.actualizarFaltasAlumno(conDom, 999, 5));

        System.out.println("\n--- CONTENIDO DESPUÉS DE MODIFICAR (añadido id=7, faltas id=3 -> 15, eliminado id=2) ---");
        System.out.println(Files.readString(conDom));

        // Verificar que se lee de vuelta sin errores y decodifica los caracteres especiales
        List<AlumnoDam> trasModificar = lector.leerAlumnosConDom(conDom);
        System.out.println("Leído de vuelta correctamente (" + trasModificar.size() + " alumnos).");
        System.out.println("Último alumno recuperado: " + trasModificar.getLast());
    }

    // ------------------------------------------------------------------ e (Adaptado a 2º DAM)
    private static void demoConversion() throws Exception {
        Files.createDirectories(SALIDA);
        Path xmlClase = DATOS.resolve("clase_2dam.xml");
        ConversorXml conversor = new ConversorXml();

        Path salidaHtml = SALIDA.resolve("clase_2dam.html");
        Path salidaCsv = SALIDA.resolve("clase_2dam.csv");
        Path salidaJson = SALIDA.resolve("clase_2dam.json");

        conversor.transformar(xmlClase, RAIZ.resolve(Path.of("xslt", "clase_2dam-html.xsl")), salidaHtml);
        conversor.transformar(xmlClase, RAIZ.resolve(Path.of("xslt", "clase_2dam-csv.xsl")), salidaCsv);
        conversor.alumnosAJson(xmlClase, salidaJson);

        System.out.println("Generados en salida/: clase_2dam.html, clase_2dam.csv y clase_2dam.json\n");
        System.out.println("-- clase_2dam.csv --");
        System.out.println(Files.readString(salidaCsv));
        System.out.println("-- clase_2dam.json --");
        System.out.println(Files.readString(salidaJson));
    }

    // ------------------------------------------------------------------ f (Adaptado a 2º DAM)
    private static void demoExcepciones() {
        Path xmlValido = DATOS.resolve("clase_2dam.xml");

        probarDam("1. Fichero que no existe",
                () -> servicioDam.cargar(DATOS.resolve("no-existe-2dam.xml"), ServicioClaseDam.Metodo.DOM));

        probarDam("2. Fichero que ya existe (copia sin sobrescribir)", () -> {
            Path copia = SALIDA.resolve("duplicado_prueba.xml");
            Files.createDirectories(SALIDA);
            Files.writeString(copia, "<clase/>");
            try {
                gestor.copiar(xmlValido, copia, false);
            } catch (FileAlreadyExistsException e) {
                throw new ClaseDamException("El fichero «" + copia.getFileName() + "» ya existe y no se ha sobrescrito.", e);
            } finally {
                Files.deleteIfExists(copia);
            }
        });

        probarDam("3. XML mal formado con línea y columna (DOM)",
                () -> servicioDam.cargar(DATOS.resolve("clase_2dam-malformado.xml"), ServicioClaseDam.Metodo.DOM));

        probarDam("4. XML mal formado (StAX)",
                () -> servicioDam.cargar(DATOS.resolve("clase_2dam-malformado.xml"), ServicioClaseDam.Metodo.STAX));

        probarDam("5. XML con DOCTYPE / ataque XXE (SAX)",
                () -> servicioDam.cargar(DATOS.resolve("clase_2dam-peligroso.xml"), ServicioClaseDam.Metodo.SAX));

        probarDam("6. Nota media con formato numérico incorrecto",
                () -> servicioDam.cargar(DATOS.resolve("clase_2dam-nota-erronea.xml"), ServicioClaseDam.Metodo.DOM));

        probarDam("7. Alumno duplicado (id=1 ya existe)", () -> {
            servicioDam.anadirAlumno(xmlValido, new AlumnoDam(1, "Repetido", "PSP", new BigDecimal("7.00"), 0));
        });

        probarDam("8. Datos no válidos en el modelo (nota negativa)", () -> {
            new AlumnoDam(99, "Alumno Error", "AD", new BigDecimal("-2.50"), 0);
        });

        System.out.println("\nTodos los casos de error se han capturado y registrado SIN detener el programa.");
    }

    @FunctionalInterface
    private interface Caso {
        void ejecutar() throws Exception;
    }

    private static void probarDam(String descripcion, Caso caso) {
        System.out.println("\n>> " + descripcion);
        try {
            caso.ejecutar();
            System.out.println("   (no ha fallado)");
        } catch (ClaseDamException e) {
            System.out.println("   Mensaje al usuario: " + e.getMessage());
            if (e.getCause() != null) {
                System.out.println("   Causa técnica encadenada: " + e.getCause().getClass().getSimpleName());
            }
        } catch (Exception e) {
            System.out.println("   Excepción capturada: " + e.getClass().getSimpleName() + " -> " + e.getMessage());
        }
    }
}