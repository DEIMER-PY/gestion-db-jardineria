package com.jardineria.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad de dominio que representa a un Cliente.
 */
public class Cliente implements Serializable {
    private Integer codigoCliente;
    private String nombreCliente;
    private String nombreContacto;
    private String apellidoContacto;
    private String telefono;
    private String fax;
    private String lineaDireccion1;
    private String lineaDireccion2;
    private String ciudad;
    private String region;
    private String pais;
    private String codigoPostal;
    private Integer codigoEmpleadoRepVentas;
    private BigDecimal limiteCredito;

    public Cliente() {
    }

    public Cliente(Integer codigoCliente, String nombreCliente, String nombreContacto, String apellidoContacto,
                   String telefono, String fax, String lineaDireccion1, String lineaDireccion2,
                   String ciudad, String region, String pais, String codigoPostal,
                   Integer codigoEmpleadoRepVentas, BigDecimal limiteCredito) {
        this.codigoCliente = codigoCliente;
        this.nombreCliente = nombreCliente;
        this.nombreContacto = nombreContacto;
        this.apellidoContacto = apellidoContacto;
        this.telefono = telefono;
        this.fax = fax;
        this.lineaDireccion1 = lineaDireccion1;
        this.lineaDireccion2 = lineaDireccion2;
        this.ciudad = ciudad;
        this.region = region;
        this.pais = pais;
        this.codigoPostal = codigoPostal;
        this.codigoEmpleadoRepVentas = codigoEmpleadoRepVentas;
        this.limiteCredito = limiteCredito;
    }

    public Integer getCodigoCliente() {
        return codigoCliente;
    }

    public void setCodigoCliente(Integer codigoCliente) {
        this.codigoCliente = codigoCliente;
    }

    public String getNombreCliente() {
        return nombreCliente;
    }

    public void setNombreCliente(String nombreCliente) {
        this.nombreCliente = nombreCliente;
    }

    public String getNombreContacto() {
        return nombreContacto;
    }

    public void setNombreContacto(String nombreContacto) {
        this.nombreContacto = nombreContacto;
    }

    public String getApellidoContacto() {
        return apellidoContacto;
    }

    public void setApellidoContacto(String apellidoContacto) {
        this.apellidoContacto = apellidoContacto;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getFax() {
        return fax;
    }

    public void setFax(String fax) {
        this.fax = fax;
    }

    public String getLineaDireccion1() {
        return lineaDireccion1;
    }

    public void setLineaDireccion1(String lineaDireccion1) {
        this.lineaDireccion1 = lineaDireccion1;
    }

    public String getLineaDireccion2() {
        return lineaDireccion2;
    }

    public void setLineaDireccion2(String lineaDireccion2) {
        this.lineaDireccion2 = lineaDireccion2;
    }

    public String getCiudad() {
        return ciudad;
    }

    public void setCiudad(String ciudad) {
        this.ciudad = ciudad;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getPais() {
        return pais;
    }

    public void setPais(String pais) {
        this.pais = pais;
    }

    public String getCodigoPostal() {
        return codigoPostal;
    }

    public void setCodigoPostal(String codigoPostal) {
        this.codigoPostal = codigoPostal;
    }

    public Integer getCodigoEmpleadoRepVentas() {
        return codigoEmpleadoRepVentas;
    }

    public void setCodigoEmpleadoRepVentas(Integer codigoEmpleadoRepVentas) {
        this.codigoEmpleadoRepVentas = codigoEmpleadoRepVentas;
    }

    public BigDecimal getLimiteCredito() {
        return limiteCredito;
    }

    public void setLimiteCredito(BigDecimal limiteCredito) {
        this.limiteCredito = limiteCredito;
    }

    public String getContactoCompleto() {
        if (nombreContacto == null && apellidoContacto == null) return "";
        return ((nombreContacto != null ? nombreContacto : "") + " " + (apellidoContacto != null ? apellidoContacto : "")).trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cliente cliente = (Cliente) o;
        return Objects.equals(codigoCliente, cliente.codigoCliente);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoCliente);
    }

    @Override
    public String toString() {
        return "Cliente{" +
                "codigoCliente=" + codigoCliente +
                ", nombreCliente='" + nombreCliente + '\'' +
                ", telefono='" + telefono + '\'' +
                ", ciudad='" + ciudad + '\'' +
                ", pais='" + pais + '\'' +
                '}';
    }
}
