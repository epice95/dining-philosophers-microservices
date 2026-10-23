package dev.philosophers.monitor;

/** Deux événements différents prétendent avoir la même horloge pour le même philosophe. */
public class ConflictingEventException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictingEventException(String message) {
        super(message);
    }
}
