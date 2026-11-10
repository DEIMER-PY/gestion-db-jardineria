package com.jardineria.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Identificador compuesto para la entidad DetallePedido.
 */
public class DetallePedidoId implements Serializable {
    private Integer codigoPedido;
    private String codigoProducto;

    public DetallePedidoId() {
    }

    public DetallePedidoId(Integer codigoPedido, String codigoProducto) {
        this.codigoPedido = codigoPedido;
        this.codigoProducto = codigoProducto;
    }

    public Integer getCodigoPedido() {
        return codigoPedido;
    }

    public void setCodigoPedido(Integer codigoPedido) {
        this.codigoPedido = codigoPedido;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetallePedidoId that = (DetallePedidoId) o;
        return Objects.equals(codigoPedido, that.codigoPedido) &&
                Objects.equals(codigoProducto, that.codigoProducto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoPedido, codigoProducto);
    }

    @Override
    public String toString() {
        return "DetallePedidoId{" +
                "codigoPedido=" + codigoPedido +
                ", codigoProducto='" + codigoProducto + '\'' +
                '}';
    }
}
