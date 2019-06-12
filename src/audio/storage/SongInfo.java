package audio.storage;
import java.util.*;

class SongInfo {
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

    @Override
    public boolean equals(Object other) {
        if (other == null)
            return false;
        else if (this.getClass() != other.getClass())
            return false;
        else {
            SongInfo comp = (SongInfo)other;
            return this.name.equals(comp.name) && this.freqs.equals(comp.freqs);
        }
    }
}
