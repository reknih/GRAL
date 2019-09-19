package de.haug.gral;

/**
 * Exceptions thrown by the Epoch class during its operations.
 */
class EpochException extends RuntimeException {
    /**
     * Constructs a new EpochException with a error message.
     * @param s The error message
     */
    EpochException(String s) {
        super(s);
    }
}
