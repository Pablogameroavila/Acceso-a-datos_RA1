package ad.ra1.xml;

import ad.ra1.modelo.AlumnoDam;
import ad.ra1.modelo.Producto;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Recuperación de información de un fichero XML (criterio c) con DOM, SAX, StAX y XPath.
 */
public class LectorXml {

    // ================================================================ DOM (2º DAM)

    /** Lee la lista de alumnos de 2º DAM con DOM. */
    public List<AlumnoDam> leerAlumnosConDom(Path fichero) throws IOException, SAXException, ParserConfigurationException {
        Document doc = XmlSeguro.parsearDom(fichero);
        NodeList nodos = doc.getElementsByTagName("alumno");
        List<AlumnoDam> alumnos = new ArrayList<>();
        for (int i = 0; i < nodos.getLength(); i++) {
            alumnos.add(aAlumno((Element) nodos.item(i)));
        }
        return alumnos;
    }

    static AlumnoDam aAlumno(Element e) {
        return new AlumnoDam(
                Integer.parseInt(e.getAttribute("id").trim()),
                texto(e, "nombre"),
                texto(e, "modulo"),
                new BigDecimal(texto(e, "notaMedia")),
                Integer.parseInt(texto(e, "horasFaltas")));
    }

    // ================================================================ SAX (2º DAM)

    /** Lee la lista de alumnos de 2º DAM con SAX (eventos). */
    public List<AlumnoDam> leerAlumnosConSax(Path fichero) throws IOException, SAXException, ParserConfigurationException {
        SAXParser analizador = XmlSeguro.fabricaSax().newSAXParser();
        ManejadorAlumnos manejador = new ManejadorAlumnos();
        try (InputStream entrada = Files.newInputStream(fichero)) {
            analizador.parse(entrada, manejador);
        }
        return manejador.alumnos;
    }

    private static final class ManejadorAlumnos extends DefaultHandler {
        private final List<AlumnoDam> alumnos = new ArrayList<>();
        private final StringBuilder texto = new StringBuilder();
        private int id;
        private String nombre;
        private String modulo;
        private BigDecimal notaMedia;
        private int horasFaltas;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atributos) {
            texto.setLength(0);
            if (qName.equals("alumno")) {
                id = Integer.parseInt(atributos.getValue("id").trim());
                nombre = null;
                modulo = null;
                notaMedia = null;
                horasFaltas = 0;
            }
        }

        @Override
        public void characters(char[] ch, int inicio, int longitud) {
            texto.append(ch, inicio, longitud);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String contenido = texto.toString().trim();
            switch (qName) {
                case "nombre" -> nombre = contenido;
                case "modulo" -> modulo = contenido;
                case "notaMedia" -> notaMedia = new BigDecimal(contenido);
                case "horasFaltas" -> horasFaltas = Integer.parseInt(contenido);
                case "alumno" -> alumnos.add(new AlumnoDam(id, nombre, modulo, notaMedia, horasFaltas));
                default -> { /* otros elementos (clase): se ignoran */ }
            }
        }
    }

    // ================================================================ StAX (2º DAM)

    /** Lee la lista de alumnos de 2º DAM con StAX. */
    public List<AlumnoDam> leerAlumnosConStax(Path fichero) throws IOException, XMLStreamException {
        List<AlumnoDam> alumnos = new ArrayList<>();
        try (InputStream entrada = Files.newInputStream(fichero)) {
            XMLStreamReader lector = XmlSeguro.fabricaStax().createXMLStreamReader(entrada);
            try {
                int id = 0;
                String nombre = null;
                String modulo = null;
                BigDecimal notaMedia = null;
                int horasFaltas = 0;
                while (lector.hasNext()) {
                    int evento = lector.next();
                    if (evento == XMLStreamConstants.START_ELEMENT) {
                        switch (lector.getLocalName()) {
                            case "alumno" -> {
                                id = Integer.parseInt(lector.getAttributeValue(null, "id").trim());
                                nombre = null;
                                modulo = null;
                                notaMedia = null;
                                horasFaltas = 0;
                            }
                            case "nombre" -> nombre = lector.getElementText().trim();
                            case "modulo" -> modulo = lector.getElementText().trim();
                            case "notaMedia" -> notaMedia = new BigDecimal(lector.getElementText().trim());
                            case "horasFaltas" -> horasFaltas = Integer.parseInt(lector.getElementText().trim());
                            default -> { /* clase */ }
                        }
                    } else if (evento == XMLStreamConstants.END_ELEMENT && lector.getLocalName().equals("alumno")) {
                        alumnos.add(new AlumnoDam(id, nombre, modulo, notaMedia, horasFaltas));
                    }
                }
            } finally {
                lector.close();
            }
        }
        return alumnos;
    }

    // ================================================================ XPath (2º DAM)

    /** Consulta XPath 1 (por atributo): devuelve la fecha del acta de clase (/clase/@fecha). */
    public String leerFechaClase(Path fichero)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException,
            XPathFactoryConfigurationException {
        Document doc = XmlSeguro.parsearDom(fichero);
        return XmlSeguro.fabricaXPath().newXPath().evaluate("/clase/@fecha", doc);
    }

    /** Consulta XPath 2 (condición numérica con variable $min): alumnos con notaMedia mayor que el mínimo. */
    public List<AlumnoDam> filtrarPorNotaMinima(Path fichero, BigDecimal minimo)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException,
            XPathFactoryConfigurationException {
        Document doc = XmlSeguro.parsearDom(fichero);
        XPath xpath = XmlSeguro.fabricaXPath().newXPath();
        xpath.setXPathVariableResolver(nombre -> "min".equals(nombre.getLocalPart()) ? minimo.doubleValue() : null);

        NodeList nodos = (NodeList) xpath.evaluate("/clase/alumno[number(notaMedia) > $min]", doc, XPathConstants.NODESET);
        List<AlumnoDam> alumnos = new ArrayList<>();
        for (int i = 0; i < nodos.getLength(); i++) {
            alumnos.add(aAlumno((Element) nodos.item(i)));
        }
        return alumnos;
    }

    // ================================================================ Métodos base de compatibilidad (Producto)

    public List<Producto> leerConDom(Path fichero) throws IOException, SAXException, ParserConfigurationException {
        Document doc = XmlSeguro.parsearDom(fichero);
        NodeList nodos = doc.getElementsByTagName("producto");
        List<Producto> productos = new ArrayList<>();
        for (int i = 0; i < nodos.getLength(); i++) {
            productos.add(aProducto((Element) nodos.item(i)));
        }
        return productos;
    }

    static Producto aProducto(Element e) {
        return new Producto(
                Integer.parseInt(e.getAttribute("id").trim()),
                texto(e, "nombre"),
                texto(e, "categoria"),
                new BigDecimal(texto(e, "precio")),
                Integer.parseInt(texto(e, "stock")));
    }

    private static String texto(Element padre, String hijo) {
        NodeList lista = padre.getElementsByTagName(hijo);
        if (lista.getLength() == 0) {
            throw new IllegalArgumentException("Falta el elemento <" + hijo + ">");
        }
        Node nodo = lista.item(0);
        return nodo.getTextContent().trim();
    }

    public List<Producto> leerConSax(Path fichero) throws IOException, SAXException, ParserConfigurationException {
        SAXParser analizador = XmlSeguro.fabricaSax().newSAXParser();
        ManejadorProductos manejador = new ManejadorProductos();
        try (InputStream entrada = Files.newInputStream(fichero)) {
            analizador.parse(entrada, manejador);
        }
        return manejador.productos;
    }

    private static final class ManejadorProductos extends DefaultHandler {
        private final List<Producto> productos = new ArrayList<>();
        private final StringBuilder texto = new StringBuilder();
        private int id;
        private String nombre;
        private String categoria;
        private BigDecimal precio;
        private int stock;

        @Override
        public void startElement(String uri, String localName, String qName, Attributes atributos) {
            texto.setLength(0);
            if (qName.equals("producto")) {
                id = Integer.parseInt(atributos.getValue("id").trim());
                nombre = null;
                categoria = null;
                precio = null;
                stock = 0;
            }
        }

        @Override
        public void characters(char[] ch, int inicio, int longitud) {
            texto.append(ch, inicio, longitud);
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            String contenido = texto.toString().trim();
            switch (qName) {
                case "nombre" -> nombre = contenido;
                case "categoria" -> categoria = contenido;
                case "precio" -> precio = new BigDecimal(contenido);
                case "stock" -> stock = Integer.parseInt(contenido);
                case "producto" -> productos.add(new Producto(id, nombre, categoria, precio, stock));
                default -> { }
            }
        }
    }

    public List<Producto> leerConStax(Path fichero) throws IOException, XMLStreamException {
        List<Producto> productos = new ArrayList<>();
        try (InputStream entrada = Files.newInputStream(fichero)) {
            XMLStreamReader lector = XmlSeguro.fabricaStax().createXMLStreamReader(entrada);
            try {
                int id = 0;
                String nombre = null;
                String categoria = null;
                BigDecimal precio = null;
                int stock = 0;
                while (lector.hasNext()) {
                    int evento = lector.next();
                    if (evento == XMLStreamConstants.START_ELEMENT) {
                        switch (lector.getLocalName()) {
                            case "producto" -> {
                                id = Integer.parseInt(lector.getAttributeValue(null, "id").trim());
                                nombre = null;
                                categoria = null;
                                precio = null;
                                stock = 0;
                            }
                            case "nombre" -> nombre = lector.getElementText().trim();
                            case "categoria" -> categoria = lector.getElementText().trim();
                            case "precio" -> precio = new BigDecimal(lector.getElementText().trim());
                            case "stock" -> stock = Integer.parseInt(lector.getElementText().trim());
                            default -> { }
                        }
                    } else if (evento == XMLStreamConstants.END_ELEMENT && lector.getLocalName().equals("producto")) {
                        productos.add(new Producto(id, nombre, categoria, precio, stock));
                    }
                }
            } finally {
                lector.close();
            }
        }
        return productos;
    }

    public List<Producto> filtrarPorPrecioMinimo(Path fichero, BigDecimal minimo)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException,
            XPathFactoryConfigurationException {
        Document doc = XmlSeguro.parsearDom(fichero);
        XPath xpath = XmlSeguro.fabricaXPath().newXPath();
        xpath.setXPathVariableResolver(nombre -> "min".equals(nombre.getLocalPart()) ? minimo.doubleValue() : null);

        NodeList nodos = (NodeList) xpath.evaluate("/inventario/producto[number(precio) > $min]", doc, XPathConstants.NODESET);
        List<Producto> productos = new ArrayList<>();
        for (int i = 0; i < nodos.getLength(); i++) {
            productos.add(aProducto((Element) nodos.item(i)));
        }
        return productos;
    }

    public String leerFecha(Path fichero)
            throws IOException, SAXException, ParserConfigurationException, XPathExpressionException,
            XPathFactoryConfigurationException {
        Document doc = XmlSeguro.parsearDom(fichero);
        return XmlSeguro.fabricaXPath().newXPath().evaluate("/inventario/@fecha", doc);
    }
}