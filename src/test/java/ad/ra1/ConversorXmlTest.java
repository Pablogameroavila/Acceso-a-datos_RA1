package ad.ra1;

import ad.ra1.modelo.AlumnoDam;
import ad.ra1.xml.ConversorXml;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del criterio e (Temática: 2º DAM): conversión a HTML, CSV y JSON. */
class ConversorXmlTest {

    private static final Path CLASE_XML = Path.of("datos", "clase_2dam.xml");
    private final ConversorXml conversor = new ConversorXml();

    @Test
    void xmlAHtmlGeneraUnaTablaConFormatoCondicional(@TempDir Path tmp) throws Exception {
        Path html = tmp.resolve("clase_2dam.html");
        conversor.transformar(CLASE_XML, Path.of("xslt", "clase_2dam-html.xsl"), html);
        String contenido = Files.readString(html);
        assertTrue(contenido.contains("Grupo 2DAM"));
        assertTrue(contenido.contains("Marta Castillo"));
        assertEquals(7, contenido.split("<tr>", -1).length - 1); // 1 cabecera + 6 alumnos
        assertTrue(contenido.contains("class=\"alerta\"")); // nota baja o faltas altas resaltadas
    }

    @Test
    void xmlACsvGeneraCabeceraYSeisLineas(@TempDir Path tmp) throws Exception {
        Path csv = tmp.resolve("clase_2dam.csv");
        conversor.transformar(CLASE_XML, Path.of("xslt", "clase_2dam-csv.xsl"), csv);
        List<String> lineas = Files.readAllLines(csv);
        assertEquals(7, lineas.size());
        assertEquals("id;nombre;modulo;notaMedia;horasFaltas", lineas.get(0));
        assertEquals("1;Pablo Navarro;Acceso a Datos;8.75;2", lineas.get(1));
        assertEquals("4;Elena Torres;Sistemas & Gestión Empresarial;7.80;4", lineas.get(4));
    }

    @Test
    void xmlAJsonGeneraUnObjetoPorAlumno(@TempDir Path tmp) throws Exception {
        Path json = tmp.resolve("clase_2dam.json");
        conversor.alumnosAJson(CLASE_XML, json);
        String contenido = Files.readString(json);
        assertTrue(contenido.startsWith("{\n  \"grupo\": \"2DAM\",\n  \"alumnos\": ["));
        assertEquals(6, contenido.split("\"id\":", -1).length - 1);
        assertTrue(contenido.contains("\"notaMedia\": 8.75"));
    }

    @Test
    void escaparJsonProtegeLosCaracteresEspeciales() {
        assertEquals("Hola", ConversorXml.escaparJson("Hola"));
        assertEquals("dice \\\"hola\\\"", ConversorXml.escaparJson("dice \"hola\""));
        assertEquals("a\\\\b", ConversorXml.escaparJson("a\\b"));
        assertEquals("l1\\nl2\\tfin", ConversorXml.escaparJson("l1\nl2\tfin"));
        assertEquals("\\u0001", ConversorXml.escaparJson("\u0001"));
    }

    @Test
    void construirJsonAlumnosEscapaLosTextos() {
        String json = ConversorXml.construirJsonAlumnos(
                List.of(new AlumnoDam(1, "Alumno \"Pro\"", "AD\\XML", new BigDecimal("8.50"), 0)));
        assertTrue(json.contains("\"nombre\": \"Alumno \\\"Pro\\\"\""));
        assertTrue(json.contains("\"modulo\": \"AD\\\\XML\""));
    }

    @Test
    void unaHojaInexistenteFalla(@TempDir Path tmp) {
        assertThrows(Exception.class,
                () -> conversor.transformar(CLASE_XML, Path.of("xslt", "no-existe.xsl"), tmp.resolve("x.html")));
    }
}