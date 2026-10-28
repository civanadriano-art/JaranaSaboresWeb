package pe.edu.utp.jarana.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EstadoPedidoConverter implements AttributeConverter<EstadoPedido, String> {
    @Override
    public String convertToDatabaseColumn(EstadoPedido attribute) {
        return attribute == null ? null : attribute.getEtiqueta();
    }

    @Override
    public EstadoPedido convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EstadoPedido.desdeEtiqueta(dbData);
    }
}
