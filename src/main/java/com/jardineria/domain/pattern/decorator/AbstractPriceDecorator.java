package com.jardineria.domain.pattern.decorator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Decorador Abstracto (GoF Decorator).
 * Mantiene una referencia al componente envuelto y delega las operaciones básicas.
 */
public abstract class AbstractPriceDecorator implements PriceCalculator {
    protected final PriceCalculator wrappee;
    protected final List<String> desgloseLocal = new ArrayList<>();

    public AbstractPriceDecorator(PriceCalculator wrappee) {
        this.wrappee = wrappee;
    }

    @Override
    public List<String> getDesglose() {
        List<String> combined = new ArrayList<>(wrappee.getDesglose());
        combined.addAll(desgloseLocal);
        return combined;
    }
}
