package com.jardineria.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Identificador compuesto para la entidad Pago.
 */
public class PagoId implements Serializable {
    private Integer codigoCliente;
    private String idTransaccion;

    public PagoId() {
    }

    public PagoId(Integer codigoCliente, String idTransaccion) {
        this.codigoCliente = codigoCliente;
        this.idTransaccion = idTransaccion;
    }

    public Integer getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(Integer codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public String getIdTransaccion() {
        return idTransaccion;
    }

    public void setIdTransaccion(String idTransaccion) {
        this.idTransaccion = idTransaccion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PagoId pagoId = (PagoId) o;
        return Objects.equals(codigoCliente, pagoId.codigoCliente) &&
                Objects.equals(idTransaccion, pagoId.idTransaccion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoCliente, idTransaccion);
    }

    @Override
    public String toString() {
        return "PagoId{" +
                "codigoCliente=" + codigoCliente +
                ", idTransaccion='" + idTransaccion + '\'' +
                '}';
    }
}
