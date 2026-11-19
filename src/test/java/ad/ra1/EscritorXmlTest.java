package ad.ra1;

import ad.ra1.modelo.AlumnoDam;
import ad.ra1.xml.EscritorXml;
import ad.ra1.xml.LectorXml;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del criterio d (Temática: 2º DAM): escribir y modificar XML. */
class EscritorXmlTest {

    private final EscritorXml escritor = new EscritorXml();
    private final LectorXml lector = new LectorXml();
    private static final LocalDate FECHA = LocalDate.of(2026, 11, 19);

    private final List<AlumnoDam> muestra = List.of(
            new AlumnoDam(1, "Pablo Navarro", "Acceso a Datos", new BigDecimal("8.75"), 2),
            new AlumnoDam(2, "Sofía \"La Pro\" & Cía <2DAM>", "Sistemas & XML", new BigDecimal("9.85"), 0));

    @Test
    void escribirConDomYLeerDevuelveLoMismo(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("dom.xml");
        escritor.escribirAlumnosConDom(f, FECHA, muestra);
        assertEquals(muestra, lector.leerAlumnosConDom(f));
        assertEquals("2026-11-19", lector.leerFechaClase(f));
    }

    @Test
    void escribirConStaxYLeerDevuelveLoMismo(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("stax.xml");
        escritor.escribirAlumnosConStax(f, FECHA, muestra);
        assertEquals(muestra, lector.leerAlumnosConStax(f));
        assertEquals(muestra, lector.leerAlumnosConSax(f));
    }

    @Test
    void losCaracteresEspecialesSeEscapanYElXmlQuedaBienFormado(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("escape.xml");
        escritor.escribirAlumnosConDom(f, FECHA, muestra);
        String contenido = Files.readString(f);
        assertTrue(contenido.contains("&amp;"));
        assertTrue(contenido.contains("&lt;2DAM&gt;"));
        assertFalse(contenido.contains("<2DAM>"));
    }

    @Test
    void noQuedanFicherosTemporalesTrasEscribir(@TempDir Path tmp) throws Exception {
        escritor.escribirAlumnosConDom(tmp.resolve("a.xml"), FECHA, muestra);
        escritor.escribirAlumnosConStax(tmp.resolve("b.xml"), FECHA, muestra);
        try (var flujo = Files.list(tmp)) {
            assertEquals(2L, flujo.count());
        }
    }

    @Test
    void anadirAlumnoLoAgregaAlFinal(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("clase.xml");
        Files.copy(Path.of("datos", "clase_2dam.xml"), f);
        escritor.anadirAlumno(f, new AlumnoDam(7, "Laura Rivas", "PSP", new BigDecimal("8.20"), 1));
        List<AlumnoDam> alumnos = lector.leerAlumnosConDom(f);
        assertEquals(7, alumnos.size());
        assertEquals("Laura Rivas", alumnos.get(6).nombre());
    }

    @Test
    void anadirUnIdRepetidoSeRechaza(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("clase.xml");
        Files.copy(Path.of("datos", "clase_2dam.xml"), f);
        assertThrows(IllegalArgumentException.class,
                () -> escritor.anadirAlumno(f, new AlumnoDam(1, "Repetido", "PSP", BigDecimal.TEN, 0)));
        assertEquals(6, lector.leerAlumnosConDom(f).size());
    }

    @Test
    void actualizarFaltasCambiaSoloEseAlumno(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("clase.xml");
        Files.copy(Path.of("datos", "clase_2dam.xml"), f);
        assertTrue(escritor.actualizarFaltasAlumno(f, 3, 15));
        assertEquals(15, lector.leerAlumnosConDom(f).get(2).horasFaltas());
        assertEquals(4, lector.leerAlumnosConDom(f).get(3).horasFaltas());
        assertFalse(escritor.actualizarFaltasAlumno(f, 99, 5));
        assertThrows(IllegalArgumentException.class, () -> escritor.actualizarFaltasAlumno(f, 3, -1));
    }

    @Test
    void eliminarAlumnoLoQuita(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("clase.xml");
        Files.copy(Path.of("datos", "clase_2dam.xml"), f);
        assertTrue(escritor.eliminarAlumno(f, 2));
        assertEquals(5, lector.leerAlumnosConDom(f).size());
        assertFalse(escritor.eliminarAlumno(f, 2));
    }
}