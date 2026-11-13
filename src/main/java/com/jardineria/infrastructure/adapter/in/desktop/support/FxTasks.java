package com.jardineria.infrastructure.adapter.in.desktop.support;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Ejecuta trabajo bloqueante (JDBC) fuera del hilo de JavaFX usando hilos virtuales de Java 21
 * y entrega el resultado de vuelta en el FX Application Thread.
 * <p>
 * Es la pieza que evita que la interfaz se congele mientras MySQL responde.
 */
public final class FxTasks {
    private static final Logger log = LoggerFactory.getLogger(FxTasks.class);
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private FxTasks() {
    }

    /**
     * Ejecuta {@code work} en segundo plano.
     *
     * @param work      trabajo bloqueante (consultas a puertos de entrada)
     * @param onSuccess callback en el hilo de JavaFX con el resultado
     * @param onError   callback en el hilo de JavaFX con la causa real del error
     */
    public static <T> CompletableFuture<T> run(Callable<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(() -> {
            try {
                return work.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, EXECUTOR);
        future.whenComplete((result, error) -> runOnFx(() -> {
            if (error == null) {
                onSuccess.accept(result);
            } else {
                Throwable cause = unwrap(error);
                log.warn("Tarea en segundo plano fallida: {}", cause.getMessage());
                onError.accept(cause);
            }
        }));
        return future;
    }

    /** Variante para operaciones sin resultado (insertar, actualizar, eliminar). */
    public static CompletableFuture<Void> runVoid(ThrowingRunnable work, Runnable onSuccess, Consumer<Throwable> onError) {
        return run(() -> {
            work.run();
            return null;
        }, ignored -> onSuccess.run(), onError);
    }

    /** Ejecuta en el hilo de JavaFX (inmediatamente si ya estamos en él). */
    public static void runOnFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }

    /** Obtiene la causa raíz significativa de un error asíncrono. */
    public static Throwable unwrap(Throwable error) {
        Throwable t = error;
        while ((t instanceof CompletionException || t instanceof ExecutionException) && t.getCause() != null) {
            t = t.getCause();
        }
        return t;
    }

    /** Indica si el error se debe a que MySQL no es alcanzable. */
    public static boolean isConnectionError(Throwable error) {
        Throwable t = error;
        while (t != null) {
            String name = t.getClass().getName();
            String msg = t.getMessage() == null ? "" : t.getMessage();
            if (name.contains("SQLTransientConnectionException") || name.contains("CommunicationsException")
                    || name.contains("ConnectException") || name.contains("PoolInitializationException")
                    || msg.contains("Communications link failure") || msg.contains("Connection is not available")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    /** Mensaje legible para el usuario a partir de un error. */
    public static String friendlyMessage(Throwable error) {
        if (isConnectionError(error)) {
            return "No hay conexión con MySQL. Verifica que el contenedor Docker esté en ejecución.";
        }
        String msg = error.getMessage();
        if (msg == null || msg.isBlank()) {
            return "Se produjo un error inesperado (" + error.getClass().getSimpleName() + ").";
        }
        // Los adaptadores JDBC envuelven el SQLException: mostramos solo la parte útil
        return msg.replaceFirst("^Error al [^:]+: ", "");
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
