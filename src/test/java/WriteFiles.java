import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;


public class WriteFiles {
	private static final String TMPDIR = System.getProperty("java.io.tmpdir");
	private static final File file = new File(TMPDIR + File.separator + "test.ser");

	public static void main(String[] args) throws FileNotFoundException, IOException {
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
		out.writeObject(new Simple());		
		out.close();
	}
}
