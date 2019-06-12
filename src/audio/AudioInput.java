package audio;
import java.io.*;
import javax.sound.sampled.*;
import javax.sound.sampled.AudioFormat.*;

class AudioInput {

    private static AudioFormat defaultFormat() {
        float sampleRate = 8000.0f;
        int sampleSize = 16;
        int channels = 1;
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate, sampleSize, channels, signed, bigEndian);
    }
    private static AudioFormat decodeFormat(AudioFormat copy) {
        Encoding encode = Encoding.PCM_SIGNED;
        float sampleRate = copy.getSampleRate();
        int sampleSize = 16;
        int channels = copy.getChannels();
        int frameSize = copy.getChannels()*2;
        float frameRate = copy.getSampleRate();
        boolean bigEndian = false;
        return new AudioFormat(encode, sampleRate, sampleSize, channels, frameSize, frameRate, bigEndian);
    }

    public AudioInputStream listen() throws LineUnavailableException {
        final AudioFormat format = defaultFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        final TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();
        return new AudioInputStream(line);
    }

    public AudioInputStream read(File file) 
        throws UnsupportedAudioFileException, IOException {

        System.out.println("File ob " + file.getName() + " " + file.getPath());
        AudioInputStream in = AudioSystem.getAudioInputStream(file);
        AudioFormat decodeForm = decodeFormat(in.getFormat());
        AudioInputStream decodeIn = AudioSystem.getAudioInputStream(decodeForm, in);
        return decodeIn;
        //PCMtoPCMCodec convert = new PCMtoPCMCodec(); //not sure if needed
        //return convert.getAudioInputStream(defaultFormat(), decodeIn);
    }
}