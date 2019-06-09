package storage;
import java.util.*;

/*
 * points - hashed sounds as key and all mapped datapoints; for lookup
 * infos - songId to name and associated datapoints, songId determined by list idx
 */

public class SongMatches {
    private Map<Integer, List<DataPoint>> points;
    private List<SongInfo> infos;

    public SongMatches () {
        this.points = new HashMap<>();
        this.infos = new ArrayList<>();
    }
    public SongMatches (List<SongInfo> start) { //add starting vals to infos and points
        this.points = new HashMap<>();
        this.infos = new ArrayList<>();
        
        for (SongInfo info: start) {
            infos.add(info);
            for (DataPoint pt: info.getFreqs()) {
                List<DataPoint> pts;
                int key = pt.hashCode();
                if((pts=points.get(key)) == null) {
                    pts = new ArrayList<>();
                    pts.add(pt);
                    points.put(key, pts);
                } else
                    pts.add(pt);
            }
        }
    }

    public int getNextId() {
        return infos.size();
    }

    public int findMatch (DataPoint check) throws NoSuchFieldException { //check against points
        //id of check not used
        Map<Integer, Map<Integer, Integer>> matches = null; //counts for each point
        List<DataPoint> possPts = null;
        if ((possPts=points.get(check.hashCode())) == null)
            throw new NoSuchFieldException();

        matches = new HashMap<>();
        for (DataPoint point: possPts) {
            Map<Integer, Integer> counts;
            final int id = point.getID();
            final int offset = Math.abs(point.getTime()-check.getTime());

            if ((counts=matches.get(id)) == null) {
                counts = new HashMap<>();
                counts.put(offset, 1);
                matches.put(id, counts);
            } else {
                int val = counts.get(offset).intValue() + 1;
                counts.put(offset, Integer.valueOf(val));
            }
        }
        return this.bestMatch(matches);
    }
    
    private int bestMatch (Map<Integer, Map<Integer, Integer>> matches) { //finds max count in matches
        int max = 0, songID = -1;
        for (Map.Entry<Integer, Map<Integer, Integer>> match: matches.entrySet()) {
            for (Map.Entry<Integer, Integer> count: match.getValue().entrySet())
                if (count.getValue() > max) {
                    max = count.getValue();
                    songID = match.getKey();
                }
        }
        return songID;
    }

    public void addPoint (DataPoint pt, String name) { //adds values to points
        List<DataPoint> possPts;
        int key = pt.hashCode();
        if((possPts=points.get(key)) == null) {
            possPts = new ArrayList<>();
            possPts.add(pt);
            points.put(key, possPts);
        } else
            possPts.add(pt);

        SongInfo match;
        final int id = pt.getID();
        if (id > infos.size()-1) { //in bounds of infos
            match = new SongInfo(name);
            match.addFreq(pt);
            infos.add(match);
        } else {
            match = infos.get(id);
            match.addFreq(pt);
        }
    }

    @Override
    public String toString() {
        StringBuilder accum = new StringBuilder();
        for (int id = 0; id < infos.size(); id++) {
            SongInfo info = infos.get(id);
            
            accum.append(id + ": " + info.getName() + "\n");
            for (DataPoint pt: info.getFreqs()) {
                accum.append("\t" + pt.getTime() + " " + pt.hashCode() + "\n");
            }
        }
        return accum.toString();
    }

}