package gr.uaegean.palaemondbproxy.model.TO;

import lombok.*;

import java.util.ArrayList;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PassengerVisualizationTO {
    private String xLoc;
    private String yLoc;
    private String geofence;
    private String assignedMS;
    private ArrayList<String> mobility;
    private String type; //Crew of Passenger
    private boolean inAssignedMS;
    private String deck;
}
