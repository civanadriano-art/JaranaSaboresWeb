package pe.edu.utp.jarana.estructuras;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ArbolAVLTest {
    @Test
    void mantieneOrdenYPermiteMenorMayor() {
        ArbolAVL<Integer,String> a = new ArbolAVL<>();
        a.insertar(30,"C"); a.insertar(10,"A"); a.insertar(20,"B"); a.insertar(40,"D");
        assertEquals("A", a.obtenerMenor());
        assertEquals("D", a.obtenerMayor());
        assertEquals(4, a.getCantidad());
        assertEquals("B", a.buscar(20));
        assertEquals("B", a.eliminar(20));
        assertNull(a.buscar(20));
    }
}
