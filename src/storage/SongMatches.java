package storage;
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
     * constructor helper methods
     */
    private List<SongInfo> readFile(File file) //parses file
        throws FileNotFoundException, IOException {

        List<SongInfo> info = new ArrayList<>();
        BufferedReader reading = new BufferedReader(new FileReader(file));
        String line;
        while((line=reading.readLine()) != null) {
            String[] tok = line.trim().split("\\s+");
            if (line.charAt(0) == '\t') { //song point
                final int id = info.size()-1;

                List<Double> kPts = new ArrayList<>();
                Scanner lst = new Scanner(tok[1]);
                lst.useDelimiter("\\[|,|\\]");
                while(lst.hasNextDouble())
                    kPts.add(lst.nextDouble());
                lst.close();

                double[] dArr = new double[kPts.size()];
                for (int i = 0; i < kPts.size(); i++)
                    dArr[i] = kPts.get(i);

                int time = Integer.parseInt(tok[0].replaceAll("\\D", ""));
                info.get(id).addFreq(new DataPoint(id, time, dArr));
            } else { //name
                info.add(new SongInfo(tok[1]));
            }
        }
        reading.close();
        return info;
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




    public int getNextId() {
        return infos.size();
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
            
            accum.append(id + ": " + info.getName() + "\n");
            for (DataPoint pt: info.getFreqs()) {
                accum.append("\t" + pt.toString() + "\n");
            }
        }
        return accum.toString();
    }

    

}