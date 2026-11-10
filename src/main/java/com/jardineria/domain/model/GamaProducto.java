package com.jardineria.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Entidad de dominio que representa una Gama o Familia de Productos.
 */
public class GamaProducto implements Serializable {
    private String gama;
    private String descripcionTexto;
    private String descripcionHtml;
    private String imagen;

    public GamaProducto() {
    }

    public GamaProducto(String gama, String descripcionTexto, String descripcionHtml, String imagen) {
        this.gama = gama;
        this.descripcionTexto = descripcionTexto;
        this.descripcionHtml = descripcionHtml;
        this.imagen = imagen;
    }

    public String getGama() {
        return gama;
    }

    public void setGama(String gama) {
        this.gama = gama;
    }

    public String getDescripcionTexto() {
        return descripcionTexto;
    }

    public void setDescripcionTexto(String descripcionTexto) {
        this.descripcionTexto = descripcionTexto;
    }

    public String getDescripcionHtml() {
        return descripcionHtml;
    }

    public void setDescripcionHtml(String descripcionHtml) {
        this.descripcionHtml = descripcionHtml;
    }

    public String getImagen() {
        return imagen;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GamaProducto that = (GamaProducto) o;
        return Objects.equals(gama, that.gama);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gama);
    }

    @Override
    public String toString() {
        return "GamaProducto{" +
                "gama='" + gama + '\'' +
                ", descripcionTexto='" + (descripcionTexto != null && descripcionTexto.length() > 30 ? descripcionTexto.substring(0, 30) + "..." : descripcionTexto) + '\'' +
                '}';
    }
}
