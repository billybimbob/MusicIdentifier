package storage;
import java.util.*;

class SongInfo { //only used in SongMatches
    private String name;
    private List<DataPoint> freqs;
    
    public SongInfo (String name) {
        this.name = name;
        this.freqs = new ArrayList<>();
    }
    public String getName() { 
        return this.name;
    }
    public List<DataPoint> getFreqs() {
        return new ArrayList<>(this.freqs); //copies
    }

    public void addFreq(DataPoint pt) {
        freqs.add(pt);
    }
}
