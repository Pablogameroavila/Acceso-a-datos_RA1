package ad.ra1.modelo;

import java.math.BigDecimal;

/**
 * Representa un alumno de 2º de DAM.
 *
 * @param id             identificador numérico del alumno
 * @param nombre         nombre completo del alumno (obligatorio)
 * @param modulo         módulo de 2º DAM evaluado
 * @param notaMedia      nota media del módulo (0.00 a 10.00)
 * @param horasFaltas    horas de ausencia acumuladas (nunca negativas)
 */
public record AlumnoDam(int id, String nombre, String modulo, BigDecimal notaMedia, int horasFaltas) {

    public AlumnoDam {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El alumno " + id + " no tiene nombre");
        }
        if (modulo == null) {
            modulo = "";
        }
        if (notaMedia == null || notaMedia.signum() < 0 || notaMedia.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException("El alumno " + id + " tiene una nota media no válida (0-10)");
        }
        if (horasFaltas < 0) {
            throw new IllegalArgumentException("El alumno " + id + " tiene horas de falta negativas");
        }
    }
}