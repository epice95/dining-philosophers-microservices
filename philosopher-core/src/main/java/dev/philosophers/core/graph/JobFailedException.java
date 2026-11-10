package dev.philosophers.core.graph;

/** Une tâche n'a pu être traitée par aucun Worker : le calcul est abandonné. */
public class JobFailedException extends Exception {

    private static final long serialVersionUID = 1L;

    public JobFailedException(String message) {
        super(message);
    }

    public JobFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
