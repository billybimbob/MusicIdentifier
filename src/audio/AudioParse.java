package audio;

import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.*;
import java.io.*;
import fourier.*;
import storage.*;

public class AudioParse {
    private static final int CHUNK_SIZE = 4096;
    private static final int[] BOUNDS = {40, 80, 120, 180, 300};

    private AudioFormat defaultFormat() {
        float sampleRate = 8000.0f;
        int sampleSize = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
    }
    private AudioFormat decodeFormat(AudioFormat copy) {
        Encoding encode = Encoding.PCM_SIGNED;
        float sampleRate = copy.getSampleRate();
        int sampleSize = copy.getSampleSizeInBits();
        int channels = copy.getChannels()*2;
        int frameSize = copy.getFrameSize();
        float frameRate = copy.getFrameRate();
        boolean bigEndian = false;
        return new AudioFormat(encode, sampleRate, sampleSize, channels, frameSize, frameRate, bigEndian);
    }

    private AudioInputStream listen() throws LineUnavailableException {
        final AudioFormat format = defaultFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        return new AudioInputStream(line);
    }

    private AudioInputStream read(File file) 
        throws UnsupportedAudioFileException, IOException {
        
        AudioInputStream in = AudioSystem.getAudioInputStream(file);
        AudioFormat decodeForm = decodeFormat(in.getFormat());
        AudioInputStream decodeIn = AudioSystem.getAudioInputStream(decodeForm, in);
        return decodeIn;
        //PCMtoPCMCodec convert = new PCMtoPCMCodec(); //not sure if needed
        //return convert.getAudioInputStream(defaultFormat(), decodeIn);
    }

    private byte[] toBytes (AudioInputStream line) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        byte[] buffer = new byte[(int)16];
        boolean running = true;
        int loops = 0;
        while(running && loops < 10) {
            int count = line.read(buffer, 0, buffer.length);
            if (count > 0) {
                out.write(buffer, 0, count);
                loops++;
            } else
                running = false;
        }
        out.close();
        return out.toByteArray();
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
    private DataPoint[] keyPoints(final Complex[][] freqs, int songID, BufferedWriter writing) 
        throws IOException {
        final int LOWER_LIMIT = BOUNDS[0];
        final int UPPER_LIMIT = BOUNDS[BOUNDS.length-1];
        DataPoint[] points = new DataPoint[freqs.length];

        for (int i = 0; i < freqs.length; i++) {
            double[] highMags = new double[BOUNDS.length];
            double[] highFreqs = new double[BOUNDS.length];

            for (int freq = LOWER_LIMIT; freq < UPPER_LIMIT; freq++) {
                int range = getRange(freq);
                double mag = Math.log(freqs[i][freq].abs() + 1);

                if (mag > highMags[range]) {
                    highMags[range] = mag;
                    highFreqs[range] = freq;
                }
            }
            System.out.println("got here");
            for (double highFreq: highFreqs)
                writing.write(highFreq + "\t");
            writing.write("\n");

            points[i] = new DataPoint(songID, i, highFreqs);
        }
        return points;
    }

    public static void parseSong (int songID, boolean adding) {
        AudioParse audParse = new AudioParse();

        try {
            AudioInputStream stream = null;
            
            boolean someCon = true;
            if (someCon)
                stream = audParse.listen();
            else
                stream = audParse.read(new File("song" + ".mp3"));

            byte[] audio = audParse.toBytes(stream);
            for (byte val: audio)
                System.out.println(val);

            Complex[][] freqs = audParse.freqTrans(audio);
            System.out.println("length " + freqs.length);
            
            BufferedWriter bw = new BufferedWriter(new FileWriter("data.txt"));
            DataPoint[] pts = audParse.keyPoints(freqs, songID, bw);
            bw.close();

            for (DataPoint pt: pts) {
                if (adding) {
                    SongMatches.addPoint(pt);
                    System.out.println("Successfully added data");
                } else {
                    SongMatches matching = new SongMatches();
                    int match = matching.findMatch(pt);
                    System.out.println("Best match is:" + match);
                }
            }

        } catch (IOException e) {
            System.err.println("I/O problem: " + e);
            System.exit(-1);
        } catch (LineUnavailableException e) {
            System.err.println("No audio: " + e);
            System.exit(-1);
        } catch (NoSuchFieldException e) {
            System.err.println("No match found");
        } catch (UnsupportedAudioFileException e) {
            System.err.println("Error reading audio file" + e);
        }
    }

    public static void main(String[] args) {
        //determine what to parse
        parseSong(0, false);
    }
}