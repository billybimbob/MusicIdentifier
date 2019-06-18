package audio;

import javax.sound.sampled.*;
import java.io.*;
import fourier.*;
import audio.storage.*;

/*
 * input - used to read audio file or listen to microphone
 * songs - store info about songs
 */

public class AudioParse {

    private static final int CHUNK_SIZE = 4096;
    private static final int[] BOUNDS = {40, 80, 120, 180, 300};
    private AudioInput input;
    private SongMatches songs;

    public AudioParse() {
        this.input = new AudioInput();
    }
    public AudioParse(SongMatches songs) {
        this.input = new AudioInput();
        this.songs = songs;
    }


    public SongMatches getSongs() { //make immutable
        return new SongMatches(songs);
    }


    /*
     * private helper functions
     */
    
    private byte[] toBytes(AudioInputStream line) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[(int)1024];
            boolean running = true;
            int loops = 0;
            while(running && loops < 1000) { //can change condition, like for mic
                int count = line.read(buffer);
                if (count > 0) {
                    out.write(buffer, 0, count);
                    loops++;
                } else
                    running = false;
            }
            System.out.println("Loops "+ loops);
            return out.toByteArray();

        }
        //return line.readAllBytes();
    }

    private Complex[][] freqTrans(final byte[] data) { //trans time domain to freq
        final int numChunks = data.length/CHUNK_SIZE;
        Complex [][] freqs = new Complex[numChunks][];
        
        for(int chunk = 0; chunk < numChunks; chunk++) {
            Complex[] set = new Complex[CHUNK_SIZE];
            for(int offset = 0; offset < CHUNK_SIZE; offset++)
                set[offset] = new Complex(data[chunk*CHUNK_SIZE + offset], 0);
            
            freqs[chunk] = FourierTrans.fft(set);
        }

        return freqs;
    }

    private int getRange(final int freq) { //could change to binary search
        int idx = 0;
        while(idx < BOUNDS.length && BOUNDS[idx] > freq)
            idx++;
        
        return idx;
    }

    //writes to input writer file
    private DataPoint[] keyPoints(final Complex[][] freqs) {
        final int LOWER_LIMIT = BOUNDS[0];
        final int UPPER_LIMIT = BOUNDS[BOUNDS.length-1];
        DataPoint[] points = new DataPoint[freqs.length];

        for (int i = 0; i < freqs.length; i++) {
            double[] highMags = new double[BOUNDS.length];
            double[] highFreqs = new double[BOUNDS.length];
            final int time = i;

            for (int freq = LOWER_LIMIT; freq < UPPER_LIMIT; freq++) {
                int range = getRange(freq);
                double mag = Math.log(freqs[i][freq].abs() + 1);

                if (mag > highMags[range]) {
                    highMags[range] = mag;
                    highFreqs[range] = freq;
                }
            }
            points[i] = new DataPoint(time, highFreqs);
        }
        return points;
    }

    private DataPoint[] parseAudio (AudioInputStream stream) throws IOException {
        byte[] audio = toBytes(stream);
        System.out.println("bytes " + audio.length);

        Complex[][] freqs = freqTrans(audio);
        System.out.println("length " + freqs.length);
        
        return keyPoints(freqs);
    }


    /*
     * methods to find and add songs
     */

    public int findSong () throws IOException, NoSuchFieldException {
        try (AudioInputStream stream = input.listen()) {
            DataPoint[] pts = parseAudio(stream);
            return songs.findMatch(pts);
        } catch (LineUnavailableException e) {
            throw new IOException(e);
        }
    }
    public void addSong (File file) throws IOException {
        try (AudioInputStream stream = input.read(file)) {
            final int songID = songs.getNextId();
            String fileName = file.getName();
            
            DataPoint[] pts = parseAudio(stream);
            for(DataPoint pt: pts)
                pt.setID(songID);

            songs.addPoints(pts, fileName);
        } catch (UnsupportedAudioFileException e) {
            throw new IOException(e);
        }
    }

}