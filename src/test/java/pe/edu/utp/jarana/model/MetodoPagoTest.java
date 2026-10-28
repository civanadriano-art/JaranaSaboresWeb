package pe.edu.utp.jarana.model;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MetodoPagoTest {
    @Test
    void soloPermiteEfectivoYapeYPlin() {
        assertArrayEquals(new String[]{"EFECTIVO","YAPE","PLIN"},
                Arrays.stream(MetodoPago.values()).map(Enum::name).toArray(String[]::new));
    }
}
