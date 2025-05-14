package ivan.solscanbot.dto.external.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import lombok.Data;

@Data
public class TransferData {
    @JsonProperty("time")
    private Date time;
}
