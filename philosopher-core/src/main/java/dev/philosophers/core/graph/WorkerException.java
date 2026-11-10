package dev.philosophers.core.graph;

/** Un Worker n'a pas pu traiter une tâche (panne, timeout, réponse invalide). */
public class WorkerException extends Exception {

    private static final long serialVersionUID = 1L;

    public WorkerException(String message) {
        super(message);
    }

    public WorkerException(String message, Throwable cause) {
        super(message, cause);
    }
}
