package ransomware.sandbox;

/**
 * Thrown when a file-system operation violates the Lab_sandbox boundary.
 * All sandbox components throw this (unchecked) instead of IOException
 * so callers can catch it separately from legitimate I/O errors.
 */
public class SandboxException extends RuntimeException {

    public SandboxException(String message) {
        super(message);
    }

    public SandboxException(String message, Throwable cause) {
        super(message, cause);
    }
}
