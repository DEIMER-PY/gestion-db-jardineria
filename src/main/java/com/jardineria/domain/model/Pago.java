package com.jardineria.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Entidad de dominio que representa un Pago efectuado por un Cliente.
 */
public class Pago implements Serializable {
    private Integer codigoCliente;
    private String formaPago;
    private String idTransaccion;
    private LocalDate fechaPago;
    private BigDecimal total;

    public Pago() {
    }

    public Pago(Integer codigoCliente, String formaPago, String idTransaccion,
                LocalDate fechaPago, BigDecimal total) {
        this.codigoCliente = codigoCliente;
        this.formaPago = formaPago;
        this.idTransaccion = idTransaccion;
        this.fechaPago = fechaPago;
        this.total = total;
    }

    public PagoId getId() {
        return new PagoId(codigoCliente, idTransaccion);
    }

    public void setId(PagoId id) {
        if (id != null) {
            this.codigoCliente = id.getCodigoCliente();
            this.idTransaccion = id.getIdTransaccion();
        }
    }

    public Integer getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(Integer codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public String getFormaPago() {
        return formaPago;
    }

    public void setFormaPago(String formaPago) {
        this.formaPago = formaPago;
    }

    public String getIdTransaccion() {
        return idTransaccion;
    }

    public void setIdTransaccion(String idTransaccion) {
        this.idTransaccion = idTransaccion;
    }

    public LocalDate getFechaPago() {
        return fechaPago;
    }

    public void setFechaPago(LocalDate fechaPago) {
        this.fechaPago = fechaPago;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Pago pago = (Pago) o;
        return Objects.equals(codigoCliente, pago.codigoCliente) &&
                Objects.equals(idTransaccion, pago.idTransaccion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoCliente, idTransaccion);
    }

    @Override
    public String toString() {
        return "Pago{" +
                "codigoCliente=" + codigoCliente +
                ", formaPago='" + formaPago + '\'' +
                ", idTransaccion='" + idTransaccion + '\'' +
                ", fechaPago=" + fechaPago +
                ", total=" + total +
                '}';
    }
}
