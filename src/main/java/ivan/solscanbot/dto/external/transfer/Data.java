package ivan.solscanbot.dto.external.transfer;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@lombok.Data
public class Data {
    @JsonProperty("data")
    private List<TransferData> data;
}
