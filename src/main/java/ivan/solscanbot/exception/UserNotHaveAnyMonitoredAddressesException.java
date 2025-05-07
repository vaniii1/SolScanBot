package ivan.solscanbot.exception;

public class UserNotHaveAnyMonitoredAddressesException extends RuntimeException {
    public UserNotHaveAnyMonitoredAddressesException(String message) {
        super(message);
    }

    public UserNotHaveAnyMonitoredAddressesException(String message, Throwable cause) {
        super(message, cause);
    }
}
