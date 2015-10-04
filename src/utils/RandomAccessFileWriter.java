package utils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by nadiachepurko on 10/3/15.
 */
public class RandomAccessFileWriter {
    private RandomAccessFile file;

    public RandomAccessFileWriter(File outFile) throws FileNotFoundException {
        file = new RandomAccessFile(outFile, "rw");
    }

    /**
     * Write all bytes of data array to file starting from offset in file
     * @param offset file bytes offset
     * @param data data to write to file
     * @throws IOException
     */
    public void write(int offset, byte[] data) throws IOException {
        file.seek(offset);
        file.write(data);
    }

    public void close() throws IOException {
        if (file != null) {
            file.close();
        }
    }
}
