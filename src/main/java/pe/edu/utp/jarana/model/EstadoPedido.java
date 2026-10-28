package pe.edu.utp.jarana.model;

public enum EstadoPedido {
    PENDIENTE("Pendiente"),
    EN_PREPARACION("En Preparación"),
    LISTO("Listo"),
    DESPACHADO("Despachado"),
    CANCELADO("Cancelado");

    private final String etiqueta;

    EstadoPedido(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String getEtiqueta() {
        return etiqueta;
    }

    public static EstadoPedido desdeEtiqueta(String valor) {
        for (EstadoPedido estado : values()) {
            if (estado.etiqueta.equalsIgnoreCase(valor) || estado.name().equalsIgnoreCase(valor)) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado no válido: " + valor);
    }
}
