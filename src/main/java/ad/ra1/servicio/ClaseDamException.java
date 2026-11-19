package ad.ra1.servicio;

/**
 * Excepción propia de la aplicación de gestión de 2º de DAM (criterio f).
 * Contiene un mensaje claro para el usuario y conserva la causa técnica original encadenada.
 */
public class ClaseDamException extends Exception {

    private static final long serialVersionUID = 1L;

    public ClaseDamException(String mensaje) {
        super(mensaje);
    }

    public ClaseDamException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}