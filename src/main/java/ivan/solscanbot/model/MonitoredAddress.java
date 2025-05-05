package ivan.solscanbot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class MonitoredAddress {
    @Id
    public Long id;
    public String address;
    public String chatId;
}
