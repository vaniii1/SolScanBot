package ivan.solscanbot.exception;

public class AddressNotMonitoredException extends RuntimeException {

    public AddressNotMonitoredException(String message) {
        super(message);
    }

    public AddressNotMonitoredException(String message, Throwable cause) {
        super(message, cause);
    }
}
