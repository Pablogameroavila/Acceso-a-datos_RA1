package ad.ra1.xml;

import ad.ra1.ficheros.GestorFicheros;
import ad.ra1.modelo.AlumnoDam;
import ad.ra1.modelo.Producto;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Almacenamiento y modificación segura de ficheros XML (criterio d - Temática: 2º DAM).
 */
public class EscritorXml {

    private final GestorFicheros gestor = new GestorFicheros();

    // ================================================================ DOM (2º DAM)

    /** Crea un fichero XML de la clase de 2º DAM construyendo el árbol DOM y volcándolo con temporal. */
    public void escribirAlumnosConDom(Path destino, LocalDate fecha, List<AlumnoDam> alumnos)
            throws IOException, ParserConfigurationException, TransformerException {
        Document doc = XmlSeguro.fabricaDom().newDocumentBuilder().newDocument();
        Element raiz = doc.createElement("clase");
        raiz.setAttribute("fecha", fecha.toString());
        raiz.setAttribute("grupo", "2DAM");
        doc.appendChild(raiz);
        for (AlumnoDam a : alumnos) {
            raiz.appendChild(crearElementoAlumno(doc, a));
        }
        guardar(doc, destino);
    }

    /** Añade un alumno a un XML ya existente. Lanza IllegalArgumentException si el id está repetido. */
    public void anadirAlumno(Path fichero, AlumnoDam nuevo)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Document doc = XmlSeguro.parsearDom(fichero);
        if (buscarAlumno(doc, nuevo.id()) != null) {
            throw new IllegalArgumentException("Ya existe un alumno en 2º DAM con id " + nuevo.id());
        }
        doc.getDocumentElement().appendChild(crearElementoAlumno(doc, nuevo));
        guardar(doc, fichero);
    }

    /** Modifica las horas de falta de un alumno existente. Devuelve false si el id no existe. */
    public boolean actualizarFaltasAlumno(Path fichero, int id, int nuevasFaltas)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        if (nuevasFaltas < 0) {
            throw new IllegalArgumentException("Las horas de falta no pueden ser negativas");
        }
        Document doc = XmlSeguro.parsearDom(fichero);
        Element alumno = buscarAlumno(doc, id);
        if (alumno == null) {
            return false;
        }
        alumno.getElementsByTagName("horasFaltas").item(0).setTextContent(Integer.toString(nuevasFaltas));
        guardar(doc, fichero);
        return true;
    }

    /** Elimina un alumno del XML por su id. Devuelve false si no existe. */
    public boolean eliminarAlumno(Path fichero, int id)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Document doc = XmlSeguro.parsearDom(fichero);
        Element alumno = buscarAlumno(doc, id);
        if (alumno == null) {
            return false;
        }
        alumno.getParentNode().removeChild(alumno);
        guardar(doc, fichero);
        return true;
    }

    private Element buscarAlumno(Document doc, int id) {
        NodeList nodos = doc.getElementsByTagName("alumno");
        for (int i = 0; i < nodos.getLength(); i++) {
            Element e = (Element) nodos.item(i);
            if (e.getAttribute("id").trim().equals(Integer.toString(id))) {
                return e;
            }
        }
        return null;
    }

    private Element crearElementoAlumno(Document doc, AlumnoDam a) {
        Element e = doc.createElement("alumno");
        e.setAttribute("id", Integer.toString(a.id()));
        e.appendChild(campo(doc, "nombre", a.nombre()));
        e.appendChild(campo(doc, "modulo", a.modulo()));
        Element nota = campo(doc, "notaMedia", a.notaMedia().toPlainString());
        nota.setAttribute("escala", "10");
        e.appendChild(nota);
        e.appendChild(campo(doc, "horasFaltas", Integer.toString(a.horasFaltas())));
        return e;
    }

    // ================================================================ StAX (2º DAM)

    /** Crea el XML de 2º DAM «al vuelo» con StAX usando fichero temporal y reemplazo atómico. */
    public void escribirAlumnosConStax(Path destino, LocalDate fecha, List<AlumnoDam> alumnos)
            throws IOException, XMLStreamException {
        Path temporal = gestor.crearTemporalJuntoA(destino);
        try {
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                XMLStreamWriter w = XMLOutputFactory.newFactory().createXMLStreamWriter(salida, "UTF-8");
                try {
                    w.writeStartDocument("UTF-8", "1.0");
                    w.writeCharacters("\n");
                    w.writeStartElement("clase");
                    w.writeAttribute("fecha", fecha.toString());
                    w.writeAttribute("grupo", "2DAM");
                    for (AlumnoDam a : alumnos) {
                        w.writeCharacters("\n    ");
                        w.writeStartElement("alumno");
                        w.writeAttribute("id", Integer.toString(a.id()));
                        campo(w, "nombre", a.nombre());
                        campo(w, "modulo", a.modulo());
                        w.writeCharacters("\n        ");
                        w.writeStartElement("notaMedia");
                        w.writeAttribute("escala", "10");
                        w.writeCharacters(a.notaMedia().toPlainString());
                        w.writeEndElement();
                        campo(w, "horasFaltas", Integer.toString(a.horasFaltas()));
                        w.writeCharacters("\n    ");
                        w.writeEndElement(); // alumno
                    }
                    w.writeCharacters("\n");
                    w.writeEndElement(); // clase
                    w.writeEndDocument();
                    w.flush();
                } finally {
                    w.close();
                }
            }
            gestor.reemplazar(temporal, destino);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    // ================================================================ Auxiliares y compatibilidad (Producto)

    public void escribirConDom(Path destino, LocalDate fecha, List<Producto> productos)
            throws IOException, ParserConfigurationException, TransformerException {
        Document doc = XmlSeguro.fabricaDom().newDocumentBuilder().newDocument();
        Element raiz = doc.createElement("inventario");
        raiz.setAttribute("fecha", fecha.toString());
        doc.appendChild(raiz);
        for (Producto p : productos) {
            raiz.appendChild(crearElementoProducto(doc, p));
        }
        guardar(doc, destino);
    }

    public void anadirProducto(Path fichero, Producto nuevo)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Document doc = XmlSeguro.parsearDom(fichero);
        if (buscarProducto(doc, nuevo.id()) != null) {
            throw new IllegalArgumentException("Ya existe un producto con id " + nuevo.id());
        }
        doc.getDocumentElement().appendChild(crearElementoProducto(doc, nuevo));
        guardar(doc, fichero);
    }

    public boolean actualizarStock(Path fichero, int id, int nuevoStock)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        if (nuevoStock < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
        Document doc = XmlSeguro.parsearDom(fichero);
        Element producto = buscarProducto(doc, id);
        if (producto == null) {
            return false;
        }
        producto.getElementsByTagName("stock").item(0).setTextContent(Integer.toString(nuevoStock));
        guardar(doc, fichero);
        return true;
    }

    public boolean eliminarProducto(Path fichero, int id)
            throws IOException, SAXException, ParserConfigurationException, TransformerException {
        Document doc = XmlSeguro.parsearDom(fichero);
        Element producto = buscarProducto(doc, id);
        if (producto == null) {
            return false;
        }
        producto.getParentNode().removeChild(producto);
        guardar(doc, fichero);
        return true;
    }

    private Element buscarProducto(Document doc, int id) {
        NodeList nodos = doc.getElementsByTagName("producto");
        for (int i = 0; i < nodos.getLength(); i++) {
            Element e = (Element) nodos.item(i);
            if (e.getAttribute("id").trim().equals(Integer.toString(id))) {
                return e;
            }
        }
        return null;
    }

    private Element crearElementoProducto(Document doc, Producto p) {
        Element e = doc.createElement("producto");
        e.setAttribute("id", Integer.toString(p.id()));
        e.appendChild(campo(doc, "nombre", p.nombre()));
        e.appendChild(campo(doc, "categoria", p.categoria()));
        Element precio = campo(doc, "precio", p.precio().toPlainString());
        precio.setAttribute("moneda", "EUR");
        e.appendChild(precio);
        e.appendChild(campo(doc, "stock", Integer.toString(p.stock())));
        return e;
    }

    private Element campo(Document doc, String nombre, String valor) {
        Element e = doc.createElement(nombre);
        e.setTextContent(valor);
        return e;
    }

    private void guardar(Document doc, Path destino) throws IOException, TransformerException {
        quitarEspaciosVacios(doc.getDocumentElement());

        Transformer t = XmlSeguro.fabricaTransformer().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

        Path temporal = gestor.crearTemporalJuntoA(destino);
        try {
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                t.transform(new DOMSource(doc), new StreamResult(salida));
            }
            gestor.reemplazar(temporal, destino);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private void quitarEspaciosVacios(Node nodo) {
        NodeList hijos = nodo.getChildNodes();
        for (int i = hijos.getLength() - 1; i >= 0; i--) {
            Node hijo = hijos.item(i);
            if (hijo.getNodeType() == Node.TEXT_NODE && hijo.getTextContent().isBlank()) {
                nodo.removeChild(hijo);
            } else if (hijo.getNodeType() == Node.ELEMENT_NODE) {
                quitarEspaciosVacios(hijo);
            }
        }
    }

    public void escribirConStax(Path destino, LocalDate fecha, List<Producto> productos)
            throws IOException, XMLStreamException {
        Path temporal = gestor.crearTemporalJuntoA(destino);
        try {
            try (OutputStream salida = Files.newOutputStream(temporal)) {
                XMLStreamWriter w = XMLOutputFactory.newFactory().createXMLStreamWriter(salida, "UTF-8");
                try {
                    w.writeStartDocument("UTF-8", "1.0");
                    w.writeCharacters("\n");
                    w.writeStartElement("inventario");
                    w.writeAttribute("fecha", fecha.toString());
                    for (Producto p : productos) {
                        w.writeCharacters("\n    ");
                        w.writeStartElement("producto");
                        w.writeAttribute("id", Integer.toString(p.id()));
                        campo(w, "nombre", p.nombre());
                        campo(w, "categoria", p.categoria());
                        w.writeCharacters("\n        ");
                        w.writeStartElement("precio");
                        w.writeAttribute("moneda", "EUR");
                        w.writeCharacters(p.precio().toPlainString());
                        w.writeEndElement();
                        campo(w, "stock", Integer.toString(p.stock()));
                        w.writeCharacters("\n    ");
                        w.writeEndElement();
                    }
                    w.writeCharacters("\n");
                    w.writeEndElement();
                    w.writeEndDocument();
                    w.flush();
                } finally {
                    w.close();
                }
            }
            gestor.reemplazar(temporal, destino);
        } finally {
            Files.deleteIfExists(temporal);
        }
    }

    private void campo(XMLStreamWriter w, String nombre, String valor) throws XMLStreamException {
        w.writeCharacters("\n        ");
        w.writeStartElement(nombre);
        w.writeCharacters(valor);
        w.writeEndElement();
    }
}