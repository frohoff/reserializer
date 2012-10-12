package org.frohoff.ysoserial;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OptionalDataException;
import java.util.LinkedList;
import java.util.List;

public class ObjectContent {	
	final Object[] contents;
	public ObjectContent(Object[] contents) {
		this.contents = contents;
	}

	public static ObjectContent read(ObjectInput in) {
		List<Object> contents = new LinkedList<Object>();
		
		boolean readBytes = true;
		boolean readObject = true;
		
		while (readBytes || readObject) {
			readBytes = readBytes(in, contents);
			readObject = readObject(in, contents);
		}
		
		return new ObjectContent(contents.toArray());		
	}
	
	public static void write(ObjectContent content, ObjectOutput out) {
		try {
			for (Object obj : content.contents) {
				if (obj instanceof byte[]) {
					out.write((byte[]) obj);				
				} else {
					out.writeObject(obj);
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static boolean readBytes(ObjectInput in, List<Object> contents) {		
		boolean read = false;
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();		
		try {
			bytes.write(new byte[] { in.readByte() });
			read = true;
			while (true) {
				bytes.write(new byte[] { in.readByte() });
			}
		} catch (EOFException e) {
			// expected
		} catch (IOException e) {
			throw new RuntimeException(e);
		}		
		if (read)
			contents.add(bytes.toByteArray());
		return read;
	}
	
	private static boolean readObject(ObjectInput in, List<Object> contents) {
		boolean read = false;
		try {			
			contents.add(in.readObject());
			read = true;
			while (true) {
				contents.add(in.readObject());
			}						
		} catch (OptionalDataException e) {
			// expected
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
		return read;
	}
}
