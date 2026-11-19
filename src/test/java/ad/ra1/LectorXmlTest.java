package ad.ra1;

import ad.ra1.modelo.AlumnoDam;
import ad.ra1.xml.LectorXml;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import java.math.BigDecimal;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del criterio c (Temática: 2º DAM): DOM, SAX, StAX y XPath. */
class LectorXmlTest {

    private static final Path CLASE_XML = Path.of("datos", "clase_2dam.xml");
    private final LectorXml lector = new LectorXml();

    @Test
    void domLeeLosSeisAlumnos() throws Exception {
        List<AlumnoDam> alumnos = lector.leerAlumnosConDom(CLASE_XML);
        assertEquals(6, alumnos.size());
        assertEquals(new AlumnoDam(1, "Pablo Navarro", "Acceso a Datos", new BigDecimal("8.75"), 2), alumnos.get(0));
    }

    @Test
    void domSaxYStaxDevuelvenExactamenteLoMismo() throws Exception {
        List<AlumnoDam> dom = lector.leerAlumnosConDom(CLASE_XML);
        assertEquals(dom, lector.leerAlumnosConSax(CLASE_XML));
        assertEquals(dom, lector.leerAlumnosConStax(CLASE_XML));
    }

    @Test
    void lasEntidadesXmlSeInterpretan() throws Exception {
        // En el XML aparece «Sistemas &amp; Gestión Empresarial»
        assertEquals("Sistemas & Gestión Empresarial", lector.leerAlumnosConDom(CLASE_XML).get(3).modulo());
        assertEquals("Sistemas & Gestión Empresarial", lector.leerAlumnosConSax(CLASE_XML).get(3).modulo());
        assertEquals("Sistemas & Gestión Empresarial", lector.leerAlumnosConStax(CLASE_XML).get(3).modulo());
    }

    @Test
    void xpathFiltraPorNotaMinima() throws Exception {
        List<AlumnoDam> notables = lector.filtrarPorNotaMinima(CLASE_XML, new BigDecimal("8.0"));
        assertEquals(3, notables.size());
        assertEquals(List.of("Pablo Navarro", "Lucía Gómez", "Marta Castillo"),
                notables.stream().map(AlumnoDam::nombre).toList());
        assertEquals(0, lector.filtrarPorNotaMinima(CLASE_XML, new BigDecimal("10.0")).size());
    }

    @Test
    void xpathLeeAtributoFecha() throws Exception {
        assertEquals("2026-11-19", lector.leerFechaClase(CLASE_XML));
    }

    @Test
    void unXmlMalFormadoLanzaExcepcion() {
        Path roto = Path.of("datos", "clase_2dam-malformado.xml");
        assertThrows(SAXException.class, () -> lector.leerAlumnosConDom(roto));
        assertThrows(SAXException.class, () -> lector.leerAlumnosConSax(roto));
        assertThrows(XMLStreamException.class, () -> lector.leerAlumnosConStax(roto));
    }

    @Test
    void unDoctypeSeRechazaEnLosTresAnalizadores() {
        Path peligroso = Path.of("datos", "clase_2dam-peligroso.xml");
        assertThrows(SAXException.class, () -> lector.leerAlumnosConDom(peligroso));
        assertThrows(SAXException.class, () -> lector.leerAlumnosConSax(peligroso));
        assertThrows(Exception.class, () -> lector.leerAlumnosConStax(peligroso));
    }

    @Test
    void unaNotaIlegibleLanzaNumberFormatException() {
        Path f = Path.of("datos", "clase_2dam-nota-erronea.xml");
        assertThrows(NumberFormatException.class, () -> lector.leerAlumnosConDom(f));
        assertThrows(NumberFormatException.class, () -> lector.leerAlumnosConSax(f));
        assertThrows(NumberFormatException.class, () -> lector.leerAlumnosConStax(f));
    }

    @Test
    void unFicheroInexistenteLanzaNoSuchFile() {
        assertThrows(NoSuchFileException.class, () -> lector.leerAlumnosConDom(Path.of("datos", "no-existe.xml")));
    }
}