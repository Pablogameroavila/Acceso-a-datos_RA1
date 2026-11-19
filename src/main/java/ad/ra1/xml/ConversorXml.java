package ad.ra1.xml;

import ad.ra1.ficheros.GestorFicheros;
import ad.ra1.modelo.AlumnoDam;
import ad.ra1.modelo.Producto;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Conversión de un fichero XML a HTML, CSV y JSON (criterio e - Temática: 2º DAM).
 */
public class ConversorXml {

    private final GestorFicheros gestor = new GestorFicheros();

    /**
     * Aplica una hoja de estilo XSLT a un XML y guarda el resultado de forma atómica (HTML o CSV).
     */
    public void transformar(Path xml, Path hoja, Path destino)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Document origen = XmlSeguro.parsearDom(xml);
        Transformer transformador = XmlSeguro.fabricaTransformer().newTransformer(new StreamSource(hoja.toFile()));

        Path temporal = gestor.crearTemporalJuntoA(destino);
        try {
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                transformador.transform(new DOMSource(origen), new StreamResult(salida));
            }
            gestor.reemplazar(temporal, destino);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    /** Convierte el XML de la clase de 2º DAM a un fichero JSON escapando correctamente los textos. */
    public void alumnosAJson(Path xml, Path destino) throws IOException, XMLStreamException {
        List<AlumnoDam> alumnos = new LectorXml().leerAlumnosConStax(xml);
        String json = construirJsonAlumnos(alumnos);

        Path temporal = gestor.crearTemporalJuntoA(destino);
        try {
            Files.writeString(temporal, json, StandardCharsets.UTF_8);
            gestor.reemplazar(temporal, destino);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    /** Construye el texto JSON de la lista de alumnos de 2º DAM. */
    public static String construirJsonAlumnos(List<AlumnoDam> alumnos) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"grupo\": \"2DAM\",\n  \"alumnos\": [\n");
        for (int i = 0; i < alumnos.size(); i++) {
            AlumnoDam a = alumnos.get(i);
            sb.append("    {")
                    .append("\"id\": ").append(a.id()).append(", ")
                    .append("\"nombre\": \"").append(escaparJson(a.nombre())).append("\", ")
                    .append("\"modulo\": \"").append(escaparJson(a.modulo())).append("\", ")
                    .append("\"notaMedia\": ").append(a.notaMedia().toPlainString()).append(", ")
                    .append("\"horasFaltas\": ").append(a.horasFaltas())
                    .append("}");
            sb.append(i < alumnos.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    // ================================================================ Compatibilidad (Producto)

    public void aJson(Path xml, Path destino) throws IOException, XMLStreamException {
        List<Producto> productos = new LectorXml().leerConStax(xml);
        String json = construirJson(productos);

        Path temporal = gestor.crearTemporalJuntoA(destino);
        try {
            Files.writeString(temporal, json, StandardCharsets.UTF_8);
            gestor.reemplazar(temporal, destino);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    public static String construirJson(List<Producto> productos) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"productos\": [\n");
        for (int i = 0; i < productos.size(); i++) {
            Producto p = productos.get(i);
            sb.append("    {")
                    .append("\"id\": ").append(p.id()).append(", ")
                    .append("\"nombre\": \"").append(escaparJson(p.nombre())).append("\", ")
                    .append("\"categoria\": \"").append(escaparJson(p.categoria())).append("\", ")
                    .append("\"precio\": ").append(p.precio().toPlainString()).append(", ")
                    .append("\"stock\": ").append(p.stock())
                    .append("}");
            sb.append(i < productos.size() - 1 ? ",\n" : "\n");
        }
        sb.append("  ]\n}\n");
        return sb.toString();
    }

    /** Escapa un texto para meterlo entre comillas en JSON. */
    public static String escaparJson(String texto) {
        StringBuilder sb = new StringBuilder(texto.length() + 8);
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }
}