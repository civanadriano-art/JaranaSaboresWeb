package pe.edu.utp.jarana.estructuras;

import java.util.ArrayList;
import java.util.List;

public class ArbolAVL<K extends Comparable<K>, V> {
    private NodoAVL<K, V> raiz;
    private int cantidad;

    public boolean estaVacio() { return raiz == null; }
    public int getCantidad() { return cantidad; }

    public boolean insertar(K clave, V valor) {
        int anterior = cantidad;
        raiz = insertar(raiz, clave, valor);
        return cantidad > anterior;
    }

    private NodoAVL<K, V> insertar(NodoAVL<K, V> nodo, K clave, V valor) {
        if (nodo == null) {
            cantidad++;
            return new NodoAVL<>(clave, valor);
        }
        int c = clave.compareTo(nodo.getClave());
        if (c < 0) nodo.setIzquierdo(insertar(nodo.getIzquierdo(), clave, valor));
        else if (c > 0) nodo.setDerecho(insertar(nodo.getDerecho(), clave, valor));
        else return nodo;
        return balancear(actualizarAltura(nodo));
    }

    public V buscar(K clave) {
        NodoAVL<K, V> actual = raiz;
        while (actual != null) {
            int c = clave.compareTo(actual.getClave());
            if (c == 0) return actual.getValor();
            actual = c < 0 ? actual.getIzquierdo() : actual.getDerecho();
        }
        return null;
    }

    public boolean contiene(K clave) { return buscar(clave) != null; }

    public V eliminar(K clave) {
        V encontrado = buscar(clave);
        if (encontrado == null) return null;
        raiz = eliminarNodo(raiz, clave);
        cantidad--;
        return encontrado;
    }

    private NodoAVL<K, V> eliminarNodo(NodoAVL<K, V> nodo, K clave) {
        if (nodo == null) return null;
        int c = clave.compareTo(nodo.getClave());
        if (c < 0) nodo.setIzquierdo(eliminarNodo(nodo.getIzquierdo(), clave));
        else if (c > 0) nodo.setDerecho(eliminarNodo(nodo.getDerecho(), clave));
        else {
            if (nodo.getIzquierdo() == null) return nodo.getDerecho();
            if (nodo.getDerecho() == null) return nodo.getIzquierdo();
            NodoAVL<K, V> sucesor = menorNodo(nodo.getDerecho());
            nodo.setClave(sucesor.getClave());
            nodo.setValor(sucesor.getValor());
            nodo.setDerecho(eliminarNodo(nodo.getDerecho(), sucesor.getClave()));
        }
        return balancear(actualizarAltura(nodo));
    }

    public V obtenerMenor() { return raiz == null ? null : menorNodo(raiz).getValor(); }
    public V obtenerMayor() { return raiz == null ? null : mayorNodo(raiz).getValor(); }

    public List<V> recorridoEnOrden() {
        List<V> valores = new ArrayList<>();
        recorrido(raiz, valores);
        return valores;
    }

    private void recorrido(NodoAVL<K, V> nodo, List<V> valores) {
        if (nodo == null) return;
        recorrido(nodo.getIzquierdo(), valores);
        valores.add(nodo.getValor());
        recorrido(nodo.getDerecho(), valores);
    }

    private NodoAVL<K, V> menorNodo(NodoAVL<K, V> nodo) {
        NodoAVL<K, V> actual = nodo;
        while (actual.getIzquierdo() != null) actual = actual.getIzquierdo();
        return actual;
    }

    private NodoAVL<K, V> mayorNodo(NodoAVL<K, V> nodo) {
        NodoAVL<K, V> actual = nodo;
        while (actual.getDerecho() != null) actual = actual.getDerecho();
        return actual;
    }

    private int altura(NodoAVL<K, V> nodo) { return nodo == null ? 0 : nodo.getAltura(); }

    private NodoAVL<K, V> actualizarAltura(NodoAVL<K, V> nodo) {
        if (nodo != null) nodo.setAltura(1 + Math.max(altura(nodo.getIzquierdo()), altura(nodo.getDerecho())));
        return nodo;
    }

    private int factor(NodoAVL<K, V> nodo) {
        return nodo == null ? 0 : altura(nodo.getIzquierdo()) - altura(nodo.getDerecho());
    }

    private NodoAVL<K, V> balancear(NodoAVL<K, V> nodo) {
        if (nodo == null) return null;
        int f = factor(nodo);
        if (f > 1) {
            if (factor(nodo.getIzquierdo()) < 0) nodo.setIzquierdo(rotarIzquierda(nodo.getIzquierdo()));
            return rotarDerecha(nodo);
        }
        if (f < -1) {
            if (factor(nodo.getDerecho()) > 0) nodo.setDerecho(rotarDerecha(nodo.getDerecho()));
            return rotarIzquierda(nodo);
        }
        return nodo;
    }

    private NodoAVL<K, V> rotarDerecha(NodoAVL<K, V> y) {
        NodoAVL<K, V> x = y.getIzquierdo();
        NodoAVL<K, V> t2 = x.getDerecho();
        x.setDerecho(y);
        y.setIzquierdo(t2);
        actualizarAltura(y);
        actualizarAltura(x);
        return x;
    }

    private NodoAVL<K, V> rotarIzquierda(NodoAVL<K, V> x) {
        NodoAVL<K, V> y = x.getDerecho();
        NodoAVL<K, V> t2 = y.getIzquierdo();
        y.setIzquierdo(x);
        x.setDerecho(t2);
        actualizarAltura(x);
        actualizarAltura(y);
        return y;
    }
}
