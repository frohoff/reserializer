package org.frohoff.ysoserial;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;


import static privilegedaccessor.StrictPA.*;

public class YsoserialObjectInputStream extends ObjectInputStream {

	public YsoserialObjectInputStream(InputStream in) throws IOException {
		super(in);
	}
	
	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		System.out.println("resolving " + desc);
		try {
			return super.resolveClass(desc);
		} catch (Exception e1) {
			try {
				ObjectStreamClass parent = (ObjectStreamClass) getValue(desc, "superDesc");
				if (parent != null)
					resolveClass(parent);
				
				return ClassSynthesizer.synthesize(desc);
			} catch (Exception e2) {
				throw new RuntimeException(e2);
			}
		}
	}

}
