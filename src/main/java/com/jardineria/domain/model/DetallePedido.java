package com.jardineria.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad de dominio que representa una línea o detalle dentro de un Pedido.
 */
public class DetallePedido implements Serializable {
    private Integer codigoPedido;
    private String codigoProducto;
    private Integer cantidad;
    private BigDecimal precioUnidad;
    private Short numeroLinea;

    public DetallePedido() {
    }

    public DetallePedido(Integer codigoPedido, String codigoProducto, Integer cantidad,
                         BigDecimal precioUnidad, Short numeroLinea) {
        this.codigoPedido = codigoPedido;
        this.codigoProducto = codigoProducto;
        this.cantidad = cantidad;
        this.precioUnidad = precioUnidad;
        this.numeroLinea = numeroLinea;
    }

    public DetallePedidoId getId() {
        return new DetallePedidoId(codigoPedido, codigoProducto);
    }

    public void setId(DetallePedidoId id) {
        if (id != null) {
            this.codigoPedido = id.getCodigoPedido();
            this.codigoProducto = id.getCodigoProducto();
        }
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

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnidad() {
        return precioUnidad;
    }

    public void setPrecioUnidad(BigDecimal precioUnidad) {
        this.precioUnidad = precioUnidad;
    }

    public Short getNumeroLinea() {
        return numeroLinea;
    }

    public void setNumeroLinea(Short numeroLinea) {
        this.numeroLinea = numeroLinea;
    }

    public BigDecimal getSubtotal() {
        if (precioUnidad == null || cantidad == null) {
            return BigDecimal.ZERO;
        }
        return precioUnidad.multiply(BigDecimal.valueOf(cantidad));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DetallePedido that = (DetallePedido) o;
        return Objects.equals(codigoPedido, that.codigoPedido) &&
                Objects.equals(codigoProducto, that.codigoProducto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoPedido, codigoProducto);
    }

    @Override
    public String toString() {
        return "DetallePedido{" +
                "codigoPedido=" + codigoPedido +
                ", codigoProducto='" + codigoProducto + '\'' +
                ", cantidad=" + cantidad +
                ", precioUnidad=" + precioUnidad +
                ", numeroLinea=" + numeroLinea +
                ", subtotal=" + getSubtotal() +
                '}';
    }
}
