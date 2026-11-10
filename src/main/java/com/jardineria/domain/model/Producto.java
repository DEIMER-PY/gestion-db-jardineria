package com.jardineria.domain.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Entidad de dominio que representa un Producto comercializado.
 */
public class Producto implements Serializable {
    private String codigoProducto;
    private String nombre;
    private String gama;
    private String dimensiones;
    private String proveedor;
    private String descripcion;
    private Integer cantidadEnStock;
    private BigDecimal precioVenta;
    private BigDecimal precioProveedor;

    public Producto() {
    }

    public Producto(String codigoProducto, String nombre, String gama, String dimensiones,
                    String proveedor, String descripcion, Integer cantidadEnStock,
                    BigDecimal precioVenta, BigDecimal precioProveedor) {
        this.codigoProducto = codigoProducto;
        this.nombre = nombre;
        this.gama = gama;
        this.dimensiones = dimensiones;
        this.proveedor = proveedor;
        this.descripcion = descripcion;
        this.cantidadEnStock = cantidadEnStock;
        this.precioVenta = precioVenta;
        this.precioProveedor = precioProveedor;
    }

    public String getCodigoProducto() {
        return codigoProducto;
    }

    public void setCodigoProducto(String codigoProducto) {
        this.codigoProducto = codigoProducto;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getGama() {
        return gama;
    }

    public void setGama(String gama) {
        this.gama = gama;
    }

    public String getDimensiones() {
        return dimensiones;
    }

    public void setDimensiones(String dimensiones) {
        this.dimensiones = dimensiones;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Integer getCantidadEnStock() {
        return cantidadEnStock;
    }

    public void setCantidadEnStock(Integer cantidadEnStock) {
        this.cantidadEnStock = cantidadEnStock;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public BigDecimal getPrecioProveedor() {
        return precioProveedor;
    }

    public void setPrecioProveedor(BigDecimal precioProveedor) {
        this.precioProveedor = precioProveedor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Producto producto = (Producto) o;
        return Objects.equals(codigoProducto, producto.codigoProducto);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigoProducto);
    }

    @Override
    public String toString() {
        return "Producto{" +
                "codigoProducto='" + codigoProducto + '\'' +
                ", nombre='" + nombre + '\'' +
                ", gama='" + gama + '\'' +
                ", cantidadEnStock=" + cantidadEnStock +
                ", precioVenta=" + precioVenta +
                '}';
    }
}
