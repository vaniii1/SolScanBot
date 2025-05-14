package ivan.solscanbot.exception;

public class ExceedsAmountOfAddressesException extends RuntimeException {
    public ExceedsAmountOfAddressesException(String message) {
        super(message);
    }

    public ExceedsAmountOfAddressesException(String message, Throwable cause) {
        super(message, cause);
    }
}
