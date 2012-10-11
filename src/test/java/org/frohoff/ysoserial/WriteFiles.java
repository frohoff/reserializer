package org.frohoff.ysoserial;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;

import privilegedaccessor.StrictPA;


public class WriteFiles {
	private static final String TMPDIR = System.getProperty("java.io.tmpdir");
	private static final File file = new File(TMPDIR + File.separator + "test.ser");

	public static void main(String[] args) throws FileNotFoundException, IOException {
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file)){
			@Override
			protected void writeClassDescriptor(ObjectStreamClass desc) throws IOException {
				try {
					if (desc.getName().contains("org.frohoff.ysoserial"))						
						StrictPA.setValue(desc, "name", desc.getName().replaceAll("org\\.frohoff\\.ysoserial", "org.frohoff.ysoserial.mangled"));
					for (ObjectStreamField field : desc.getFields())
						StrictPA.setValue(field, "signature", ((String) StrictPA.getValue(field,"signature")).replaceAll("org/frohoff/ysoserial", "org/frohoff/ysoserial/mangled"));						
				} catch (Exception e){
					throw new RuntimeException(e);
				}
				super.writeClassDescriptor(desc);
			}
			
			
		};
		out.writeObject(new Simple[] {  });		
		out.close();
	}
}
