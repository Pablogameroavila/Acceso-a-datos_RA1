package ad.ra1.servicio;

import ad.ra1.modelo.AlumnoDam;
import ad.ra1.xml.ConversorXml;
import ad.ra1.xml.EscritorXml;
import ad.ra1.xml.LectorXml;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactoryConfigurationException;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Capa de servicio para 2º de DAM (criterio f): centraliza todos los bloques catch,
 * registra el detalle técnico con Logger y traduce los errores en {@link ClaseDamException}.
 */
public class ServicioClaseDam {

    private static final Logger LOG = Logger.getLogger(ServicioClaseDam.class.getName());

    public enum Metodo { DOM, SAX, STAX }

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar() throws Exception;
    }

    private final LectorXml lector = new LectorXml();
    private final EscritorXml escritor = new EscritorXml();
    private final ConversorXml conversor = new ConversorXml();

    // ================================================================ operaciones

    public List<AlumnoDam> cargar(Path xml, Metodo metodo) throws ClaseDamException {
        return ejecutar(xml, () -> switch (metodo) {
            case DOM -> lector.leerAlumnosConDom(xml);
            case SAX -> lector.leerAlumnosConSax(xml);
            case STAX -> lector.leerAlumnosConStax(xml);
        });
    }

    public void guardar(Path destino, LocalDate fecha, List<AlumnoDam> alumnos) throws ClaseDamException {
        ejecutar(destino, () -> {
            escritor.escribirAlumnosConStax(destino, fecha, alumnos);
            return null;
        });
    }

    public void anadirAlumno(Path xml, AlumnoDam alumno) throws ClaseDamException {
        ejecutar(xml, () -> {
            escritor.anadirAlumno(xml, alumno);
            return null;
        });
    }

    public void exportar(Path xml, Path hoja, Path destino) throws ClaseDamException {
        ejecutar(xml, () -> {
            conversor.transformar(xml, hoja, destino);
            return null;
        });
    }

    public void exportarJson(Path xml, Path destino) throws ClaseDamException {
        ejecutar(xml, () -> {
            conversor.alumnosAJson(xml, destino);
            return null;
        });
    }

    // ================================================================ gestión centralizada de excepciones

    private <T> T ejecutar(Path fichero, Operacion<T> operacion) throws ClaseDamException {
        String nombre = fichero.getFileName().toString(); // nunca exponemos rutas completas al usuario
        try {
            return operacion.ejecutar();

            // 1. Excepciones de ficheros: de las subclases concretas a la padre IOException
        } catch (NoSuchFileException e) {
            throw error("No existe el fichero «" + nombre + "».", e);
        } catch (AccessDeniedException e) {
            throw error("No tienes permisos suficientes para acceder a «" + nombre + "».", e);
        } catch (FileAlreadyExistsException e) {
            throw error("El fichero «" + nombre + "» ya existe y no se ha sobrescrito.", e);
        } catch (IOException e) {
            throw error("Error de entrada/salida al procesar «" + nombre + "».", e);

            // 2. Excepciones de XML: SAXParseException (subclase con línea y columna) antes que SAXException
        } catch (SAXParseException e) {
            throw error("El XML de «" + nombre + "» está mal formado o no está permitido "
                    + "(línea " + e.getLineNumber() + ", columna " + e.getColumnNumber() + ").", e);
        } catch (SAXException | XMLStreamException e) {
            throw error("El contenido XML de «" + nombre + "» no es válido o contiene elementos prohibidos.", e);
        } catch (TransformerException | ParserConfigurationException | XPathExpressionException
                 | XPathFactoryConfigurationException e) {
            throw error("Error interno de configuración al analizar «" + nombre + "».", e);

            // 3. Excepciones de datos (RuntimeException): NumberFormatException antes que su padre IllegalArgumentException
        } catch (NumberFormatException e) {
            throw error("Hay un valor numérico con formato incorrecto en «" + nombre + "».", e);
        } catch (IllegalArgumentException e) {
            throw error("Datos no válidos en «" + nombre + "»: " + e.getMessage(), e);

            // 4. Red de seguridad final: cualquier otra excepción no prevista
        } catch (Exception e) {
            throw error("Error inesperado al trabajar con «" + nombre + "».", e);
        }
    }

    /** Registra la causa técnica en el Logger y devuelve la excepción propia de la aplicación. */
    private ClaseDamException error(String mensajeUsuario, Exception causa) {
        LOG.log(Level.WARNING, "{0} | Causa técnica: {1}", new Object[]{mensajeUsuario, causa});
        LOG.log(Level.FINE, "Traza completa del error", causa);
        return new ClaseDamException(mensajeUsuario, causa);
    }
}