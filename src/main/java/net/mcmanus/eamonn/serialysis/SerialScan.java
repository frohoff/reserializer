/*
 *                ,
 * Copyright 2007 Eamonn McManus.
 *
 *  This file is part of the Serialysis library.
 *
 *  Serialysis is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  Serialysis is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Serialysis; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA 02110-1301  USA.
 *
 *  Linking this library statically or dynamically with other modules is
 *  making a combined work based on this library.  Thus, the terms and
 *  conditions of the GNU General Public License cover the whole
 *  combination.
 *
 *  As a special exception, the copyright holder of this library gives you
 *  permission to link this library with independent modules to produce an
 *  executable, regardless of the license terms of these independent
 *  modules, and to copy and distribute the resulting executable under
 *  terms of your choice, provided that you also meet, for each linked
 *  independent module, the terms and conditions of the license of that
 *  module.  An independent module is a module which is not derived from
 *  or based on this library.  If you modify this library, you may extend
 *  this exception to your version of the library, but you are not
 *  obligated to do so.  If you do not wish to do so, delete this
 *  exception statement from your version.
 */

package net.mcmanus.eamonn.serialysis;

import static java.io.ObjectStreamConstants.SC_BLOCK_DATA;
import static java.io.ObjectStreamConstants.SC_EXTERNALIZABLE;
import static java.io.ObjectStreamConstants.SC_SERIALIZABLE;
import static java.io.ObjectStreamConstants.SC_WRITE_METHOD;
import static java.io.ObjectStreamConstants.STREAM_MAGIC;
import static java.io.ObjectStreamConstants.STREAM_VERSION;
import static java.io.ObjectStreamConstants.TC_ARRAY;
import static java.io.ObjectStreamConstants.TC_BLOCKDATA;
import static java.io.ObjectStreamConstants.TC_BLOCKDATALONG;
import static java.io.ObjectStreamConstants.TC_CLASS;
import static java.io.ObjectStreamConstants.TC_CLASSDESC;
import static java.io.ObjectStreamConstants.TC_ENDBLOCKDATA;
import static java.io.ObjectStreamConstants.TC_ENUM;
import static java.io.ObjectStreamConstants.TC_EXCEPTION;
import static java.io.ObjectStreamConstants.TC_LONGSTRING;
import static java.io.ObjectStreamConstants.TC_NULL;
import static java.io.ObjectStreamConstants.TC_OBJECT;
import static java.io.ObjectStreamConstants.TC_PROXYCLASSDESC;
import static java.io.ObjectStreamConstants.TC_REFERENCE;
import static java.io.ObjectStreamConstants.TC_RESET;
import static java.io.ObjectStreamConstants.TC_STRING;
import static java.io.ObjectStreamConstants.baseWireHandle;
import static net.mcmanus.eamonn.serialysis.Trace.enter;
import static net.mcmanus.eamonn.serialysis.Trace.exit;
import static net.mcmanus.eamonn.serialysis.Trace.show;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.SequenceInputStream;
import java.io.StreamCorruptedException;
import java.io.WriteAbortedException;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
// for javadoc

/**
 * Scan a serial stream to produce a representation of each object
 * in the stream.
 */
public class SerialScan {
	
	private Map<String,ClassDesc> classDescriptions = new HashMap<String, ClassDesc>();
    public Map<String, ClassDesc> getClassDescriptions() { return classDescriptions; }

	/**
     * Scan the given {@code InputStream} as a serial stream.
     * Unlike an {@link ObjectInputStream}, each call to
     * {@link #readObject()} produces an {@code SEntity}, which can
     * be examined to determine the object contents as serialized
     * in the serial stream.
     *
     * @param in the serial stream to be analyzed.
     * @exception IOException if there is a problem reading the
     * {@code InputStream}, for example if it does not begin with
     * the correct sequence of bytes.
     */
    public SerialScan(InputStream in, boolean expectingHeader) throws IOException {
        this.in = in;
        this.din = new DataInputStream(in);
        if (expectingHeader) {
            if (din.readShort() != STREAM_MAGIC
                    
            		|| din.readShort() != STREAM_VERSION)
                throw new StreamCorruptedException("Bad stream header");        	
        }
    }
    
    public SerialScan(InputStream in) throws IOException {
    	this(in, false);
    }

    /**
     * Examine the given object by serializing it and examining
     * the resultant serial stream.
     *
     * @param x the object to be examined.
     * @return the {@code SEntity} representing {@code x}.
     * @exception IllegalArgumentException if the object cannot be serialized or
     * the resultant serial stream is incorrectly formatted.
     */
    public static SEntity examine(Object x) {
        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bout);
            oos.writeObject(x);
            oos.close();
            ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
            SerialScan ss = new SerialScan(bin);
            return ss.readObject();
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Return the representation of the next object read from the serial stream.
     *
     * @return the representation of the object read from the serial stream.
     */
    public SEntity readObject() throws IOException {
        enter("readObject");
        Object x = readObjectOrEnd();
        if (x == END)
            throw new StreamCorruptedException("Unexpected end-block-data");
        //if (x instanceof ClassDesc) {
         //   throw new StreamCorruptedException("Unexpected classdesc");
            // These are internal serialization objects which shouldn't
            // escape to this level.
        //}
        exit(x);
		for (int i=0; i<handles.size(); i++ ) {
			System.out.print(i);
			System.out.print(": ");
			System.out.println(handles.get(i));
		}
		try {
			System.out.println(handles.get(39));
		} catch (Exception ex) {
			
		}
        return (SEntity) x;
    }

    private SString readString() throws IOException {
        enter("readString");
        SEntity so = readObject();
        return (SString) so;
    }

    private SEntity readObjectOrEnd() throws IOException {
        while (true) {
            int code = din.readByte();
            switch (code) {
                case TC_OBJECT:
                    return newObject();
                case TC_CLASS:
                    return newClass();
                case TC_ARRAY:
                    return newArray();
                case TC_STRING:
                    return newString();
                case TC_LONGSTRING:
                    return newLongString();
                case TC_ENUM:
                    return newEnum();
                case TC_CLASSDESC:
                case TC_PROXYCLASSDESC:
                    classDesc(code); break;
                case TC_REFERENCE:
                    return prevObject();
                case TC_NULL:
                    return null;
                case TC_EXCEPTION:
                    exception(); break;
                case TC_RESET:
                    reset(); break;
                case TC_BLOCKDATA:
                    return blockDataShort();
                case TC_BLOCKDATALONG:
                    return blockDataLong();
                case TC_ENDBLOCKDATA:
                    return END;
                default:
                    throw new StreamCorruptedException("Bad type code: " + code);
            }
        }
    }

    private SEntity newObject() throws IOException {
        enter("newObject");
        ObjectClassDesc desc = classDesc();
        SObject t = new SObject(desc.getType());
        newHandle(t);
        for (ObjectClassDesc cd : desc.getHierarchy())
            classData(t, cd);
        exit(t);
        return t;
    }

    private void classData(SObject t, ObjectClassDesc cd) throws IOException {
        int flags = cd.getFlags();
        if ((flags & SC_SERIALIZABLE) != 0) {
            // wrclass or nowrclass, both start with values:
            for (FieldDesc fieldDesc : cd.getFields()) {
                SEntity x = fieldDesc.read();
                t.setField(fieldDesc.getName(), x);
            }
            if ((flags & SC_WRITE_METHOD) != 0) {
                // wrclass has objectAnnotation, currently ignored
                objectAnnotation(t);
            }
        } else if ((flags & SC_EXTERNALIZABLE) != 0) {
            if ((flags & SC_BLOCK_DATA) == 0)
                throw new IOException("Can't handle externalContents");
            objectAnnotation(t);
        }
    }

    private void objectAnnotation(SObject t) throws IOException {
        SEntity x;
        while ((x = readObjectOrEnd()) != END)
            t.addAnnotation(x);
    }

    private ClassDesc newClass() throws IOException {
        ClassDesc desc = classDesc();
        newHandle(desc);
        return desc;
    }

    private ObjectClassDesc classDesc() throws IOException {
        int code = din.readByte();
        return classDesc(code);
    }

    private ObjectClassDesc classDesc(int code) throws IOException {
        enter("classDesc", code);
        ObjectClassDesc classDesc = classDesc0(code);
        exit(classDesc);
        return classDesc;
    }

    private ObjectClassDesc classDesc0(int code) throws IOException {
        switch (code) {
            case TC_CLASSDESC:
                return newPlainClassDesc();
            case TC_PROXYCLASSDESC:
                return newProxyClassDesc();
            case TC_NULL:
                return null;
            case TC_REFERENCE:
                return (ObjectClassDesc) prevObject();
            default:
                throw new StreamCorruptedException("Bad class descriptor");
        }
    }

    private ObjectClassDesc newPlainClassDesc() throws IOException {
        enter("newPlainClassDesc");
        String className = din.readUTF();
        show("className", className);
        long serialVersionUID = din.readLong();
        show("serialVersionUID", serialVersionUID);
        // classDescInfo:
        int flags = din.readByte();
        show("flags", flags);
        ObjectClassDesc desc;
        if (className.startsWith("["))
            desc = getArrayClassDesc(className, flags, serialVersionUID);
        else
            desc = getObjectClassDesc(className, flags, serialVersionUID);
        newHandle(desc);
        // fields:
        int nfields = din.readShort();
        show("nfields", nfields);
        FieldDesc[] fields = new FieldDesc[nfields];
        for (int i = 0; i < nfields; i++)
            fields[i] = fieldDesc();
        desc.setFields(fields);
        classAnnotation(desc);
        // superClassDesc:
        ObjectClassDesc superDesc = classDesc();
        show("superDesc", superDesc);
        desc.setSuperClassDesc(superDesc);
        exit(desc);
        return desc;
    }

	private ObjectClassDesc getObjectClassDesc(String className, int flags, Long serialVersionUID) {
		ObjectClassDesc desc = (ObjectClassDesc) classDescriptions.get(className);
		if (desc != null) {
			if (desc.getSerialVersionUID() == null)
				desc.setSerialVersionUID(serialVersionUID);			
			if (!desc.getName().equals(className) || desc.getFlags() != flags)
				throw new IllegalStateException("name/flags/serialVersionUID don't match");
			return desc;
		} else {
			desc = new ObjectClassDesc(className, flags, serialVersionUID);
        	classDescriptions.put(desc.getName(), desc);
        	return desc;
		}
	}

	private ArrayClassDesc getArrayClassDesc(String className, int flags, Long serialVersionUID) throws IOException {
		ArrayClassDesc desc = (ArrayClassDesc) classDescriptions.get(className);
		if (desc != null) {
			if (desc.getSerialVersionUID() == null)
				desc.setSerialVersionUID(serialVersionUID);
			if (!desc.getName().equals(className) || desc.getFlags() != flags)
				throw new IllegalStateException("name/flags/serialVersionUID don't match");
			return desc;
		} else {
			desc = new ArrayClassDesc(className, flags, serialVersionUID);
        	classDescriptions.put(desc.getName(), desc);
        	return desc;
		}	}

    private ObjectClassDesc newProxyClassDesc() throws IOException {
        ObjectClassDesc desc = new ObjectClassDesc("<Proxy>", SC_SERIALIZABLE, 1L);
        // SC_SERIALIZABLE but not SC_WRITE_METHOD
        newHandle(desc);
        int count = din.readInt();
        String[] interfaces = new String[count];
        for (int i = 0; i < count; i++)
            interfaces[i] = din.readUTF();
        // we don't do anything with this array for now
        classAnnotation(desc);
        ObjectClassDesc superDesc = classDesc();
        desc.setSuperClassDesc(superDesc);
        return desc;
    }

    private void classAnnotation(ClassDesc desc) throws IOException {
        // we currently throw away the annotation
        Object x;
        while ((x = readObjectOrEnd()) != END)
            ;
    }

    private FieldDesc fieldDesc() throws IOException {
        enter("fieldDesc");
        char c = (char) din.readByte();
        final boolean primitive;
        switch (c) {
            case 'B':
            case 'C':
            case 'D':
            case 'F':
            case 'I':
            case 'J':
            case 'S':
            case 'Z':
                primitive = true;
                break;
            case 'L': case '[':
                primitive = false;
                break;
            default:
                throw new StreamCorruptedException("Bad field type " + (int) c);
        }
        String name = din.readUTF();
        FieldDesc desc;
        if (primitive)
            desc = new PrimitiveFieldDesc(name, c);
        else {
            String className = readString().getValue();
            if (className.startsWith("L")) {
            	className = className.substring(1, className.length() - 1);
            }
        	className = className.replaceAll("/", ".");
            desc = new ReferenceFieldDesc(name, className);
        }
        exit(desc);
        return desc;
    }

    private SArray newArray() throws IOException {
        enter("newArray");
        ArrayClassDesc classDesc = (ArrayClassDesc) classDesc();
        show("classDesc", classDesc);
        int size = din.readInt();
        show("size", size);
        SArray array = new SArray(classDesc.getType(), size);
        show("array class", array.getClass().getName());
        newHandle(array);
        ClassDesc componentClassDesc = classDesc.getComponentClassDesc();
        for (int i = 0; i < size; i++)
            array.set(i, componentClassDesc.read());
        exit(array);
        return array;
    }

    private SString newString() throws IOException {
        SString s = new SString(din.readUTF());
        newHandle(s);
        return s;
    }

    private SString newLongString() throws IOException {
        /*
         * The Java libraries don't offer a way to read a UTF8-encoded
         * string without reading an initial two-byte length.  Serialized
         * long strings have an eight-byte length.  So we fabricate a
         * sequence of substreams where each but the last has a length
         * of 65535 and the last has whatever bytes are left over.
         */
        long len = din.readLong();
        StringBuilder sb = new StringBuilder();
        while (len > 0) {
            int slice = (int) Math.min(len, 65535);
            byte[] blen = {(byte) (slice >> 8), (byte) slice};
            InputStream lenis = new ByteArrayInputStream(blen);
            InputStream seqis = new SequenceInputStream(lenis, din);
            DataInputStream ddin = new DataInputStream(seqis);
            String s = ddin.readUTF();
            assert s.length() == slice;
            sb.append(s);
            len -= slice;
        }
        return new SString(sb.toString());
    }

    private SObject newEnum() throws IOException {
        ObjectClassDesc classDesc = classDesc();
        SObject enumConst = new SObject(classDesc.getType());
        newHandle(enumConst);
        SString constName = readString();
        classDesc.getEnumValues().add(constName.getValue());
        enumConst.setField("<name>", constName);
        return enumConst;
    }

    private void exception() throws IOException {
        reset();
        IOException exc = new IOException(readObject().toString());
        reset();
        throw new WriteAbortedException("Writing aborted", exc);
    }

    private SBlockData blockDataShort() throws IOException {
        int len = din.readUnsignedByte();
        return blockData(len);
    }

    private SBlockData blockDataLong() throws IOException {
        int len = din.readInt();
        return blockData(len);
    }

    private SBlockData blockData(int len) throws IOException {
        byte[] data = new byte[len];
        din.readFully(data);
        return new SBlockData(data);
    }

    private void newHandle(SEntity o) {
        handles.add(o);
    }

    private SEntity prevObject() throws IOException {
        int h = din.readInt();
        int i = h - baseWireHandle;
        System.out.println(i);
        if (i < 0 || i > handles.size())
            throw new StreamCorruptedException("Bad handle: " + h);
        return handles.get(i);
    }

    private void reset() {
        handles.clear();
    }

    public abstract class ClassDesc extends SEntity {
        ClassDesc(String name) {
            super(name);
            this.name = name;
        }

        /* Making ClassDesc extend SEntity is not very clean but is a
         * consequence of not having a clean separation in the representation
         * between publicly visible parts of the serial stream such as
         * strings and arrays, and internal parts such as class descriptions.
         */
        String kind() {
            throw new UnsupportedOperationException();
        }
        String contents() {
            throw new UnsupportedOperationException();
        }

        abstract SEntity read() throws IOException;
        abstract Class<?> arrayComponentClass();
        public abstract String toString();

        private final String name;

		public String getName() {
			return name;
		}        
    }

    public class ObjectClassDesc extends ClassDesc {
    	private Long serialVersionUID = null;
    	
        ObjectClassDesc(String name, int flags, Long serialVersionUID) {
            super(name);
            this.flags = flags;
            this.serialVersionUID = serialVersionUID;
        }

		public Long getSerialVersionUID() {
			return serialVersionUID;
		}		
		
		public void setSerialVersionUID(Long serialVersionUID) {
			this.serialVersionUID = serialVersionUID;
		}

		SEntity read() throws IOException {
            return readObject();
        }

        Class<?> arrayComponentClass() {
            if (getType().equals("java.lang.String"))
                return String.class;
            else
                return SEntity.class;
        }

        public void setFields(FieldDesc[] fields) {
            this.fields = fields;
        }

        public FieldDesc[] getFields() {
            return fields;
        }

        public int getFlags() {
            return flags;
        }

        public void setSuperClassDesc(ObjectClassDesc superClassDesc) {
            this.superClassDesc = superClassDesc;
        }        
        
        public ObjectClassDesc getSuperClassDesc() {
			return superClassDesc;
		}

		public String toString() {
            // Don't include getFields() because could provoke recursion
            return getType();
        }

        public List<ObjectClassDesc> getHierarchy() {
            if (hierarchy.isEmpty()) {
                if (superClassDesc != null)
                    hierarchy.addAll(superClassDesc.getHierarchy());
                hierarchy.add(this);
            }
            return hierarchy;
        }

        public Set<String> getEnumValues() {
			return enumValues;
		}

		private final int flags;
        private FieldDesc[] fields = new FieldDesc[0];
        private ObjectClassDesc superClassDesc;
        private final List<ObjectClassDesc> hierarchy =
                new ArrayList<ObjectClassDesc>();
        
        private final Set<String> enumValues = new HashSet<String>();        
    }

    public class ArrayClassDesc extends ObjectClassDesc {
        ArrayClassDesc(String name, int flags, long serialVersionUID) throws IOException {
            super(name, flags, serialVersionUID);
            String componentName = name.substring(1);
            if (componentName.startsWith("["))
                componentClassDesc = getArrayClassDesc(componentName, flags, null);
            else if (componentName.startsWith("L")) {
                componentName =
                        componentName.substring(1, componentName.length() - 1);
                // "Ljava.lang.Integer;" -> "java.lang.Integer"
                componentClassDesc = getObjectClassDesc(componentName, flags, null);
            } else {
                if (componentName.length() > 1)
                    throw new StreamCorruptedException("Bad array type " + name);
                char typeCode = componentName.charAt(0);
                componentClassDesc =
                        primitiveClassDescFactory.forTypeCode(typeCode);
            }
            componentClass = componentClassDesc.arrayComponentClass();
            arrayClass = Array.newInstance(componentClass, 0).getClass();
        }

        Class<?> arrayComponentClass() {
            return arrayClass;
        }

        public ClassDesc getComponentClassDesc() {
            return componentClassDesc;
        }

        private final ClassDesc componentClassDesc;
        private final Class<?> componentClass;
        private final Class<?> arrayClass;
    }

    class PrimitiveClassDescFactory {
        private final Map<Character, PrimitiveClassDesc> descMap =
                new HashMap<Character, PrimitiveClassDesc>();

        {
            String primitives =
                    "BByte CChar DDouble FFloat IInt JLong SShort ZBoolean";
            for (String prim : primitives.split(" ")) {
                char typeCode = prim.charAt(0);
                String readWhat = prim.substring(1);
                Method readMethod;
                try {
                    readMethod =
                            DataInputStream.class.getMethod("read" + readWhat);
                } catch (Exception e) {
                    throw new RuntimeException("No read method for " + readWhat,
                            e);
                }
                Class<?> componentClass;
                try {
                    Class<?> arrayClass = Class.forName("[" + typeCode);
                    componentClass = arrayClass.getComponentType();
                } catch (Exception e) {
                    throw new RuntimeException("No array class for " + typeCode,
                            e);
                }
                PrimitiveClassDesc desc =
                        new PrimitiveClassDesc(typeCode, readMethod, componentClass);
                descMap.put(typeCode, desc);
            }
        }

                PrimitiveClassDesc forTypeCode(char c) throws IOException {
                    PrimitiveClassDesc desc = descMap.get(c);
                    if (desc == null)
                        throw new StreamCorruptedException("Bad type code " + (int) c);
                    return desc;
                }
    }
    private final PrimitiveClassDescFactory
            primitiveClassDescFactory = new PrimitiveClassDescFactory();

   public class PrimitiveClassDesc extends ClassDesc {
        PrimitiveClassDesc(char typeCode, Method readMethod,
                Class<?> componentClass) {
            super(String.valueOf(typeCode));
            this.readMethod = readMethod;
            this.componentClass = componentClass;
        }

        SEntity read() throws IOException {
            try {
                Object wrapped = readMethod.invoke(din);
                return new SPrim(wrapped);
            } catch (IllegalAccessException e) {
                throw new RuntimeException("Read method invoke failed", e);
            } catch (InvocationTargetException e) {
                Throwable t = e.getTargetException();
                if (t instanceof IOException)
                    throw (IOException) t;
                else if (t instanceof RuntimeException)
                    throw (RuntimeException) t;
                else if (t instanceof Error)
                    throw (Error) t;
                else
                    throw new RuntimeException(t.toString(), t);
            }
        }

        Class<?> arrayComponentClass() {
            return componentClass;
        }

        public String toString() {
            return componentClass.getName();
        }

        private final Method readMethod;
        private final Class<?> componentClass;
    }

    public abstract class FieldDesc {
        FieldDesc(String name) {
            this.name = name;
        }

        abstract SEntity read() throws IOException;
        public abstract String toString();

        public String getName() {
            return this.name;
        }

        private final String name;
        
        public abstract String getTypeName();
    }

   public class ReferenceFieldDesc extends FieldDesc {
        ReferenceFieldDesc(String name, String className) {
            super(name);
            this.className = className;
        }

        SEntity read() throws IOException {
            return readObject();
        }

        public String toString() {
            return className + " " + getName();
        }

        private final String className;

		@Override
		public String getTypeName() {
			return className;
		}
    }

   public class PrimitiveFieldDesc extends FieldDesc {
        PrimitiveFieldDesc(String name, char type) throws IOException {
            super(name);
            classDesc = primitiveClassDescFactory.forTypeCode(type);
        }

        SEntity read() throws IOException {
            return classDesc.read();
        }

        public String toString() {
            return classDesc + " " + getName();
        }

        private final PrimitiveClassDesc classDesc;

		@Override
		public String getTypeName() {
			return classDesc.toString();
		}
    }

    private static final SEntity END = new SString("END");
    private final InputStream in;
    private final DataInputStream din;
    private final List<SEntity> handles = new ArrayList<SEntity>();
}
