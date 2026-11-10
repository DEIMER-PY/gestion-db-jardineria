package com.jardineria.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entidad de dominio que representa a un Empleado.
 */
public class Empleado implements Serializable {
    private Integer codigoEmpleado;
    private String nombre;
    private String apellido1;
    private String apellido2;
    private String extension;
    private String email;
    private String codigoOficina;
    private Integer codigoJefe;
    private String puesto;

    public Empleado() {
    }

    public Empleado(Integer codigoEmpleado, String nombre, String apellido1, String apellido2,
                    String extension, String email, String codigoOficina, Integer codigoJefe, String puesto) {
        this.codigoEmpleado = codigoEmpleado;
        this.nombre = nombre;
        this.apellido1 = apellido1;
        this.apellido2 = apellido2;
        this.extension = extension;
        this.email = email;
        this.codigoOficina = codigoOficina;
        this.codigoJefe = codigoJefe;
        this.puesto = puesto;
    }

    public Integer getCodigoEmpleado() {
        return codigoEmpleado;
    }

    public void setCodigoEmpleado(Integer codigoEmpleado) {
        this.codigoEmpleado = codigoEmpleado;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido1() {
        return apellido1;
    }

    public void setApellido1(String apellido1) {
        this.apellido1 = apellido1;
    }

    public String getApellido2() {
        return apellido2;
    }

    public void setApellido2(String apellido2) {
        this.apellido2 = apellido2;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCodigoOficina() {
        return codigoOficina;
    }

    public void setCodigoOficina(String codigoOficina) {
        this.codigoOficina = codigoOficina;
    }

    public Integer getCodigoJefe() {
        return codigoJefe;
    }

    public void setCodigoJefe(Integer codigoJefe) {
        this.codigoJefe = codigoJefe;
    }

    public String getPuesto() {
        return puesto;
    }

    public void setPuesto(String puesto) {
        this.puesto = puesto;
    }

    public String getNombreCompleto() {
        StringBuilder sb = new StringBuilder();
        if (nombre != null) sb.append(nombre).append(" ");
        if (apellido1 != null) sb.append(apellido1);
        if (apellido2 != null && !apellido2.isBlank()) sb.append(" ").append(apellido2);
        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Empleado empleado = (Empleado) o;
        return Objects.equals(codigoEmpleado, empleado.codigoEmpleado);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoEmpleado);
    }

    @Override
    public String toString() {
        return "Empleado{" +
                "codigoEmpleado=" + codigoEmpleado +
                ", nombreCompleto='" + getNombreCompleto() + '\'' +
                ", email='" + email + '\'' +
                ", puesto='" + puesto + '\'' +
                ", codigoOficina='" + codigoOficina + '\'' +
                '}';
    }
}
