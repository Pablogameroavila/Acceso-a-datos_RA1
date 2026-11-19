package ad.ra1;

import ad.ra1.ficheros.AccesoFicheros;
import ad.ra1.modelo.AlumnoDam;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del criterio b (Temática: 2º DAM): texto, binario secuencial y binario de acceso aleatorio. */
class AccesoFicherosTest {

    private final AccesoFicheros acceso = new AccesoFicheros();

    private List<AlumnoDam> muestra() {
        return List.of(
                new AlumnoDam(1, "Pablo Navarro", "Acceso a Datos", new BigDecimal("8.75"), 2),
                new AlumnoDam(2, "Lucia Gomez", "PSP", new BigDecimal("9.10"), 0),
                new AlumnoDam(3, "Carlos Mendoza", "PMDM", new BigDecimal("6.50"), 8));
    }

    @Test
    void escribirLineasSustituyeOAnade(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("registro_asistencia.txt");
        acceso.escribirLineas(f, List.of("uno"), false);
        acceso.escribirLineas(f, List.of("dos"), true);
        assertEquals(List.of("uno", "dos"), acceso.leerLineasEnMemoria(f));

        acceso.escribirLineas(f, List.of("solo esta"), false);
        assertEquals(List.of("solo esta"), acceso.leerLineasBuffered(f));
        assertEquals(1L, acceso.contarLineas(f));
    }

    @Test
    void leerCsvConvierteLasLineasEnAlumnos(@TempDir Path tmp) throws IOException {
        Path csv = tmp.resolve("alumnos_2dam.csv");
        Files.write(csv, List.of(
                "id;nombre;modulo;notaMedia;horasFaltas",
                "1;Pablo Navarro;Acceso a Datos;8.75;2",
                "2;Lucia Gomez;PSP;9.10;0",
                "3;Carlos Mendoza;PMDM;6.50;8"));

        List<AlumnoDam> alumnos = acceso.leerCsv(csv);
        assertEquals(3, alumnos.size());
        assertEquals("Pablo Navarro", alumnos.get(0).nombre());
        assertEquals(new BigDecimal("6.50"), alumnos.get(2).notaMedia());
    }

    @Test
    void leerCsvIndicaLaLineaDelError(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("roto.csv");
        Files.write(f, List.of("id;nombre;modulo;notaMedia;horasFaltas", "1;Solo;tres;columnas"));
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> acceso.leerCsv(f));
        assertTrue(e.getMessage().contains("Línea 2"));
    }

    @Test
    void binarioSecuencialGuardaYRecuperaTodo(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("alumnos.dat");
        acceso.escribirBinario(f, muestra());

        assertEquals(3L * AccesoFicheros.TAM_REGISTRO, Files.size(f)); // registros de longitud fija (96 bytes)
        assertEquals(muestra(), acceso.leerBinario(f));
    }

    @Test
    void binarioRecortaLosTextosLargos(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("alumnos.dat");
        String largo = "Un nombre larguísimo que no cabe en veinte caracteres";
        acceso.escribirBinario(f, List.of(new AlumnoDam(9, largo, "AD", new BigDecimal("7.50"), 1)));
        assertEquals(largo.substring(0, AccesoFicheros.CARACTERES_TEXTO), acceso.leerBinario(f).get(0).nombre());
    }

    @Test
    void accesoAleatorioLeeUnRegistroConcreto(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("alumnos.dat");
        acceso.escribirBinario(f, muestra());
        assertEquals("Carlos Mendoza", acceso.leerRegistro(f, 2).nombre());
        assertEquals(1, acceso.leerRegistro(f, 0).id());
        assertEquals(3L, acceso.contarRegistros(f));
    }

    @Test
    void accesoAleatorioModificaSoloLasFaltas(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("alumnos.dat");
        acceso.escribirBinario(f, muestra());
        long tamanoAntes = Files.size(f);

        acceso.actualizarHorasFaltas(f, 1, 15);

        assertEquals(15, acceso.leerRegistro(f, 1).horasFaltas());
        assertEquals("Lucia Gomez", acceso.leerRegistro(f, 1).nombre()); // el resto intacto
        assertEquals(2, acceso.leerRegistro(f, 0).horasFaltas());       // los vecinos intactos
        assertEquals(tamanoAntes, Files.size(f));
    }

    @Test
    void indiceFueraDeRangoSeRechaza(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("alumnos.dat");
        acceso.escribirBinario(f, muestra());
        assertThrows(IllegalArgumentException.class, () -> acceso.leerRegistro(f, 3));
        assertThrows(IllegalArgumentException.class, () -> acceso.leerRegistro(f, -1));
        assertThrows(IllegalArgumentException.class, () -> acceso.actualizarHorasFaltas(f, 10, 1));
    }

    @Test
    void ficheroBinarioDanadoSeDetecta(@TempDir Path tmp) throws IOException {
        Path f = tmp.resolve("danado.dat");
        Files.write(f, new byte[AccesoFicheros.TAM_REGISTRO + 5]); // un registro y 5 bytes sobrantes
        assertThrows(IOException.class, () -> acceso.leerBinario(f));
    }
}