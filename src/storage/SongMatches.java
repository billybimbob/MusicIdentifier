package storage;
import java.util.*;

public class SongMatches {
    private static HashMap<Integer, List<DataPoint>> points = new HashMap<>();
    private static HashMap<Integer, String> names = new HashMap<>();

    public static void addPoint (DataPoint pt) { //adds values to points
        List<DataPoint> possPts;
        if((possPts=points.get(pt.hashCode())) == null) {
            possPts = new ArrayList<>();
            possPts.add(pt);
        } else
            possPts.add(pt);

        //if (names.get(pt.getID()) == null)
            //add name somehow
    }
    
    //add method that checks against points

}