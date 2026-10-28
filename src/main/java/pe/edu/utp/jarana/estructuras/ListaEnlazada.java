package pe.edu.utp.jarana.estructuras;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ListaEnlazada<T> {
    private Nodo<T> inicio;
    private Nodo<T> fin;
    private int cantidad;

    public void agregar(T dato) {
        Nodo<T> nuevo = new Nodo<>(dato);
        if (inicio == null) inicio = fin = nuevo;
        else {
            fin.setSiguiente(nuevo);
            fin = nuevo;
        }
        cantidad++;
    }

    public T eliminarUltimo() {
        if (inicio == null) return null;
        if (inicio == fin) {
            T dato = inicio.getDato();
            inicio = fin = null;
            cantidad = 0;
            return dato;
        }
        Nodo<T> actual = inicio;
        while (actual.getSiguiente() != fin) actual = actual.getSiguiente();
        T dato = fin.getDato();
        fin = actual;
        fin.setSiguiente(null);
        cantidad--;
        return dato;
    }

    public void paraCada(Consumer<T> accion) {
        Nodo<T> actual = inicio;
        while (actual != null) {
            accion.accept(actual.getDato());
            actual = actual.getSiguiente();
        }
    }

    public List<T> aLista() {
        List<T> salida = new ArrayList<>();
        paraCada(salida::add);
        return salida;
    }

    public boolean estaVacia() { return inicio == null; }
    public int getCantidad() { return cantidad; }
    public Nodo<T> getInicio() { return inicio; }
}
