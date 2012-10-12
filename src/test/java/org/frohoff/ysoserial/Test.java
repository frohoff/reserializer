package org.frohoff.ysoserial;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;

import org.apache.commons.lang3.builder.EqualsBuilder;

public class Test {
	private static final String TMPDIR = System.getProperty("java.io.tmpdir");
	private static final File FILE = new File(TMPDIR + File.separator + "test.ser");
	
	@org.junit.Test
	public void test() throws FileNotFoundException, IOException, ClassNotFoundException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		File serDir = new File("/Users/chris/Downloads/sers");
		File[] sers = serDir.listFiles();
		for (File ser : sers) {
			try {
				if (ser.length() < 100000 && ser.getName().endsWith(".ser")) {
					testFile(ser);
					System.out.println("success on " + ser);
				}
					
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("failed in " + ser + " with " + e);
			}
		}
		//testFile(FILE);
	}

	private void testFile(File file) throws IOException, FileNotFoundException, ClassNotFoundException {
		File reserFile = new File(file.getAbsolutePath() + ".reser");
		
		System.out.println("testing " + file);
		
		ObjectInputStream in = new YsoserialObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
		Object obj = in.readObject();
		
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(reserFile));
		//out.writeObject(obj);
		
		ObjectInputStream in2 = new YsoserialObjectInputStream(new BufferedInputStream(new FileInputStream(reserFile)));
		Object obj2 = in2.readObject();

		//System.out.println(obj2);		
		System.out.println("matches: " + new EqualsBuilder().reflectionEquals(obj, obj2, true));
		
	}
}
