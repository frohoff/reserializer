package org.frohoff.ysoserial;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;

import net.mcmanus.eamonn.serialysis.SerialScan;
import net.mcmanus.eamonn.serialysis.SerialScan.ArrayClassDesc;
import net.mcmanus.eamonn.serialysis.SerialScan.ClassDesc;
import net.mcmanus.eamonn.serialysis.SerialScan.ObjectClassDesc;

public class YsoserialObjectInputStream extends ObjectInputStream {

	private final SerialScan scan;
	
	public YsoserialObjectInputStream(InputStream in) throws IOException {
		super(in);		
		if (!in.markSupported())
			throw new IllegalArgumentException("stream needs to support mark()/reset()");
		in.mark(Integer.MAX_VALUE);
		scan = new SerialScan(in, false);
		System.out.println(scan.readObject());		
		in.reset();		
	}
	
	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		try {
			return super.resolveClass(desc);
		} catch (Exception e1) {
			ClassDesc d = scan.getClassDescriptions().get(desc.getName());
			return resolveClass(d);
		}
	}
	
	protected Class<?> resolveClass(ClassDesc classDesc) {
		try {
			if (classDesc instanceof ArrayClassDesc) {
				ArrayClassDesc arrayClassDesc = (ArrayClassDesc) classDesc;
				ClassDesc componentClassDesc = arrayClassDesc.getComponentClassDesc();
				resolveClass(componentClassDesc); // recurse
				return Class.forName(arrayClassDesc.getName());
			} else if (classDesc instanceof ObjectClassDesc) {
				ObjectClassDesc objClassDesc = (ObjectClassDesc) classDesc;
				ObjectClassDesc superClassDesc = objClassDesc.getSuperClassDesc();
				if (superClassDesc != null)
					resolveClass(superClassDesc); // recurse
				return ClassSynthesizer.synthesize(objClassDesc);
			}
			throw new IllegalStateException("I'm lost"); 
		} catch (Exception e) {
			throw new RuntimeException(e);
		}		
	}
}
