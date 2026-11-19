package ad.ra1;

import ad.ra1.modelo.AlumnoDam;
import ad.ra1.servicio.ClaseDamException;
import ad.ra1.servicio.ServicioClaseDam;
import ad.ra1.servicio.ServicioClaseDam.Metodo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del criterio f (Temática: 2º DAM): cada error técnico se traduce y conserva la causa. */
class ServicioClaseDamTest {

    private final ServicioClaseDam servicio = new ServicioClaseDam();

    @Test
    void cargaNormalEnLosTresMetodos() throws ClaseDamException {
        for (Metodo m : Metodo.values()) {
            assertEquals(6, servicio.cargar(Path.of("datos", "clase_2dam.xml"), m).size());
        }
    }

    @Test
    void ficheroInexistenteDaUnMensajeClaroYConservaLaCausa() {
        ClaseDamException e = assertThrows(ClaseDamException.class,
                () -> servicio.cargar(Path.of("datos", "no-existe-2dam.xml"), Metodo.DOM));
        assertTrue(e.getMessage().contains("No existe el fichero «no-existe-2dam.xml»"));
        assertInstanceOf(NoSuchFileException.class, e.getCause());
    }

    @Test
    void xmlMalFormadoIndicaLineaYColumna() {
        ClaseDamException e = assertThrows(ClaseDamException.class,
                () -> servicio.cargar(Path.of("datos", "clase_2dam-malformado.xml"), Metodo.DOM));
        assertTrue(e.getMessage().contains("línea"));
        assertTrue(e.getMessage().contains("columna"));
    }

    @Test
    void elMensajeAlUsuarioNoRevelaLaRutaCompleta() {
        ClaseDamException e = assertThrows(ClaseDamException.class,
                () -> servicio.cargar(Path.of("datos", "no-existe-2dam.xml"), Metodo.DOM));
        assertFalse(e.getMessage().contains("datos/"));
        assertFalse(e.getMessage().contains("datos\\"));
    }

    @Test
    void unDoctypeSeRechazaConMensajeClaro() {
        ClaseDamException e = assertThrows(ClaseDamException.class,
                () -> servicio.cargar(Path.of("datos", "clase_2dam-peligroso.xml"), Metodo.SAX));
        assertTrue(e.getMessage().contains("no está permitido"));
    }

    @Test
    void unaNotaMalEscritaSeTraduce() {
        ClaseDamException e = assertThrows(ClaseDamException.class,
                () -> servicio.cargar(Path.of("datos", "clase_2dam-nota-erronea.xml"), Metodo.DOM));
        assertTrue(e.getMessage().contains("formato incorrecto"));
        assertInstanceOf(NumberFormatException.class, e.getCause());
    }

    @Test
    void anadirDuplicadoSeTraduceYNoCorrompeElFichero(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve("clase.xml");
        Files.copy(Path.of("datos", "clase_2dam.xml"), f);
        assertThrows(ClaseDamException.class,
                () -> servicio.anadirAlumno(f, new AlumnoDam(1, "Repetido", "PSP", BigDecimal.ONE, 0)));
        assertEquals(6, servicio.cargar(f, Metodo.DOM).size());
    }

    @Test
    void guardarYVolverACargar(@TempDir Path tmp) throws Exception {
        List<AlumnoDam> alumnos = servicio.cargar(Path.of("datos", "clase_2dam.xml"), Metodo.STAX);
        Path destino = tmp.resolve("copia_clase.xml");
        servicio.guardar(destino, java.time.LocalDate.of(2026, 11, 19), alumnos);
        assertEquals(alumnos, servicio.cargar(destino, Metodo.DOM));
    }

    @Test
    void alumnoConDatosImposiblesNoSePuedeCrear() {
        assertThrows(IllegalArgumentException.class, () -> new AlumnoDam(1, " ", "AD", BigDecimal.TEN, 0));
        assertThrows(IllegalArgumentException.class, () -> new AlumnoDam(1, "ok", "AD", new BigDecimal("-1"), 0));
        assertThrows(IllegalArgumentException.class, () -> new AlumnoDam(1, "ok", "AD", new BigDecimal("11.0"), 0));
        assertThrows(IllegalArgumentException.class, () -> new AlumnoDam(1, "ok", "AD", BigDecimal.TEN, -3));
    }
}