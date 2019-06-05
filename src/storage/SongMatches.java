package storage;
import java.util.*;

public class SongMatches {
    private static Map<Integer, List<DataPoint>> points = new HashMap<>();
    private static Map<Integer, String> names = new HashMap<>();
    private Map<Integer, Map<Integer, Integer>> matches;
    
    public SongMatches () {
        this.matches = new HashMap<>();
    }

    //checks against points
    public int findMatch (DataPoint check) throws NoSuchFieldException {
        //id of check not used
        List<DataPoint> possPts = null;
        if ((possPts=points.get(check.hashCode())) == null)
            throw new NoSuchFieldException();

        this.matches = new HashMap<>();
        for (DataPoint point: possPts) {
            Map<Integer, Integer> counts;
            final int id = point.getID();
            final int offset = Math.abs(point.getTime()-check.getTime());

            if ((counts=matches.get(id)) != null) {
                int val = counts.get(offset).intValue() + 1;
                counts.put(offset, Integer.valueOf(val));
            } else {
                counts = new HashMap<>();
                counts.put(offset, 1);
                matches.put(id, counts);
            }
        }
        return this.bestMatch();
    }
    
    private int bestMatch () { //finds max count in counts
        int max = 0, songID = -1;
        for (Map.Entry<Integer, Map<Integer, Integer>> match: this.matches.entrySet()) {
            for (Map.Entry<Integer, Integer> count: match.getValue().entrySet())
                if (count.getValue() > max) {
                    max = count.getValue();
                    songID = match.getKey();
                }
        }
        return songID;
    }


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

}