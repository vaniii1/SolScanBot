package ivan.solscanbot.verifier;

import ivan.solscanbot.dto.internal.MonitoredAddress;
import ivan.solscanbot.exception.AddressAlreadyExistsException;
import ivan.solscanbot.exception.AddressNotMonitoredException;
import ivan.solscanbot.exception.ExceedsAmountOfAddressesException;
import ivan.solscanbot.exception.InvalidAddressException;
import ivan.solscanbot.exception.UserNotHaveAnyMonitoredAddressesException;
import ivan.solscanbot.repository.MonitoredAddressRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SolanaAddressVerifierImpl implements SolanaAddressVerifier {
    private static final String SOLANA_ADDRESS_PATTERN = "[1-9A-HJ-NP-Za-km-z]{32,44}";

    private final MonitoredAddressRepository addressRepository;

    @Override
    public void verifyAmountOfAddresses(int amount, String[] addresses) {
        if (addresses.length > amount) {
            throw new ExceedsAmountOfAddressesException(
                    "You can only manage up to three addresses in a single request.");
        }
    }

    @Override
    public void verifyAddressIsProvided(String message) {
        if (message.trim().length() <= 1) {
            throw new InvalidAddressException("No addresses provided.");
        }
    }

    @Override
    public void verifyValidSolanaAddress(String address) {
        if (address == null || !address.matches(SOLANA_ADDRESS_PATTERN)) {
            throw new InvalidAddressException("Invalid Solana Address - " + address);
        }
    }

    @Override
    public void verifyUserHasCertainAddress(long chatId, String address) {
        if (!addressRepository.existsByAddressAndChatId(address, chatId)) {
            throw new AddressNotMonitoredException(
                    "You need to add this address to monitored address first "
                            + "'/add address1 address2' ...'. Address: " + address);
        }
    }

    @Override
    public void verifyAddressAlreadyAdded(long chatId, String address) {
        if (addressRepository.existsByAddressAndChatId(address, chatId)) {
            throw new AddressAlreadyExistsException(
                    "You already have this address in your list.\n\nAddress: " + address);
        }
    }

    @Override
    public void verifyValidSolanaAddresses(String[] addresses) {
        Arrays.stream(addresses).forEach(this::verifyValidSolanaAddress);
    }

    @Override
    public void verifyUserHasCertainAddresses(long chatId, Set<String> addresses) {
        addresses.forEach(address -> verifyUserHasCertainAddress(chatId, address));
    }

    @Override
    public void verifyAddressesAlreadyAdded(long chatId, Set<String> addresses) {
        addresses.forEach(address -> verifyAddressAlreadyAdded(chatId, address));
    }

    @Override
    public void verifyUserHasAddresses(List<MonitoredAddress> addresses) {
        if (addresses.isEmpty()) {
            throw new UserNotHaveAnyMonitoredAddressesException("You have no monitored addresses");
        }
    }
}
