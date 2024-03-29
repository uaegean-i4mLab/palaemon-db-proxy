package gr.uaegean.palaemondbproxy.model.TO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EvacuationCoordinatorEventTO {

/*
{
'originator': 'evacuation-coordinator',
'evacuation-status': 0,
'timestamp': '2022-03-03T22:26:13.393523'
}

 */

    private String originator;
    @JsonProperty("evacuation-status")
    private int evacuationStatus;
    private String timestamp;
    private  int current;

}
