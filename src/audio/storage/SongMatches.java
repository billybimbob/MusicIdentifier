package audio.storage;
import java.util.*;
import java.io.*;

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
    public SongMatches (SongMatches copy) { //copy constructor
        this.points = new HashMap<>(copy.points);
        this.infos = new ArrayList<>(copy.infos);
    }
    public SongMatches (File file) throws FileNotFoundException, IOException {
        this();
        this.addInfo(this.readFile(file));
    }

    
    /*
     * private helper methods
     */

    private List<SongInfo> readFile(File file) //parses file
        throws FileNotFoundException, IOException {

        try (BufferedReader reading = new BufferedReader(new FileReader(file))) {
                
            List<SongInfo> info = new ArrayList<>();
            String line;
            while((line=reading.readLine()) != null) {
                String[] tok;

                final int id = info.size()-1;
                if (line.charAt(0) == '\t') { //song point
                    tok = line.split(",\\s+");
                    int time, hash;
                    for (String pt: tok) {
                        String[] ptTok = pt.trim().split(":\\s+");
                        time = Integer.parseInt(ptTok[0]);
                        hash = Integer.parseInt(ptTok[1]);
                        info.get(id).addFreq(new DataPoint(id, time, hash));
                    }
                } else { //name
                    tok = line.split(":\\s+");
                    String name = tok[1];
                    if (id >= 0 && !name.equals(info.get(id).getName()) || id < 0) //coalesce with previous entry; maybe index by name?
                        info.add(new SongInfo(name));
                }
            }
            return info;
        }
    }
    private void addInfo(List<SongInfo> start) { //add starting vals to infos and points; songs split up
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

    private int bestMatch (Map<Integer, Map<Integer, Integer>> matches) { //finds max count in matches
        int max = 0, songID = -1;
        for (Map.Entry<Integer, Map<Integer, Integer>> match: matches.entrySet()) {
            int ptCount = 0;
            for (Map.Entry<Integer, Integer> count: match.getValue().entrySet())
                ptCount += count.getValue();
            
            if (ptCount > max) {
                max = ptCount;
                songID = match.getKey();
            }
        }
        return songID;
    }


    /*
     * public methods
     */
    
    public int getNextId() {
        return infos.size();
    }
    public String getName(int id) {
        return infos.get(id).getName();
    }

    public int findMatch (DataPoint[] checks) throws NoSuchFieldException { //check against points
        //id of checks not used
        Map<Integer, Map<Integer, Integer>> matches = new HashMap<>(); //counts for each point
        List<DataPoint> possPts = null;

        for (DataPoint check: checks) {
            if ((possPts=points.get(check.hashCode())) == null) //not sure
                throw new NoSuchFieldException();
            
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
        }
        return this.bestMatch(matches);
    }

    public void addPoints (DataPoint[] pts, String name) { //adds values to points
        for (DataPoint pt: pts) {
            List<DataPoint> possPts;
            boolean dup = false;
            int key = pt.hashCode();
            if((possPts=points.get(key)) == null) {
                possPts = new ArrayList<>();
                possPts.add(pt);
                points.put(key, possPts);
            } else {
                for(int i = 0; !dup && i < possPts.size(); i++) //look for dup point
                    if (possPts.get(i).equals(pt))
                        dup = true;
            }

            if (!dup) { //double check dup check, songs could have similar points at same time
                possPts.add(pt);

                SongInfo match;
                final int id = pt.getID();
                if (id >= infos.size()) { //in bounds of infos
                    match = new SongInfo(name);
                    match.addFreq(pt);
                    infos.add(match);
                } else {
                    match = infos.get(id);
                    match.addFreq(pt);
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder accum = new StringBuilder();
        for (int id = 0; id < infos.size(); id++) {
            SongInfo info = infos.get(id);
            
            accum.append(id + ": " + info.getName() + "\n\t");
            for (DataPoint pt: info.getFreqs()) {
                accum.append(pt.toString() + ", ");
            }
            accum.append("\n");
        }
        return accum.toString();
    }

    

}