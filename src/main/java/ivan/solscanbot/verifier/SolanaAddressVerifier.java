package ivan.solscanbot.verifier;

import ivan.solscanbot.dto.internal.MonitoredAddress;
import java.util.List;
import java.util.Set;

public interface SolanaAddressVerifier {
    void verifyAmountOfAddresses(int amount, String[] addresses);

    void verifyAddressIsProvided(String message);

    void verifyValidSolanaAddress(String address);

    void verifyUserHasCertainAddress(long chatId, String address);

    void verifyAddressAlreadyAdded(long chatId, String address);

    void verifyValidSolanaAddresses(String[] addresses);

    void verifyUserHasCertainAddresses(long chatId, Set<String> addresses);

    void verifyAddressesAlreadyAdded(long chatId, Set<String> addresses);

    void verifyUserHasAddresses(List<MonitoredAddress> addresses);
}
