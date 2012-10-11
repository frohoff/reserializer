package org.frohoff.ysoserial;

import static java.io.ObjectStreamConstants.SC_EXTERNALIZABLE;
import static java.io.ObjectStreamConstants.SC_SERIALIZABLE;
import static privilegedaccessor.StrictPA.getValue;

import java.io.Externalizable;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.NotFoundException;
import javassist.SerialVersionUID;
import net.mcmanus.eamonn.serialysis.SerialScan.FieldDesc;
import net.mcmanus.eamonn.serialysis.SerialScan.ObjectClassDesc;

public class ClassSynthesizer {

	static Class<?> synthesize(ObjectStreamClass desc) throws NoSuchFieldException, NotFoundException, CannotCompileException {
		final ObjectStreamField[] fields = desc.getFields();
		
		final ClassPool pool = ClassPool.getDefault();
		final CtClass cc = pool.makeClass(desc.getName());
		
		cc.addInterface(pool.getCtClass(Doppelganger.class.getName()));
		
		if ((Boolean) getValue(desc, "serializable"))
			cc.addInterface(pool.getCtClass(Serializable.class.getName()));
		if ((Boolean) getValue(desc, "externalizable")) {
			cc.addInterface(pool.getCtClass(Externalizable.class.getName()));
			// FIXME: implement this
			new UnsupportedOperationException("need to add some methods to the class");
		}
		
		// add serialVersionUID
		cc.addField(new CtField(CtClass.longType,"serialVersionUID", cc){{
			setModifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);
		}}, CtField.Initializer.constant(desc.getSerialVersionUID()));
		
		
		// add fields in desc
		for (final ObjectStreamField field : fields) {
			cc.addField(new CtField(pool.getCtClass(field.getType().getName()), field.getName(), cc));
		}
		
		
		
		return cc.toClass();
	}

	public static Class<?> synthesize(String name) throws CannotCompileException {
		final ClassPool pool = ClassPool.getDefault();
		final CtClass cc = pool.makeClass(name);
		
		return cc.toClass();
	}

	public static Class<?> synthesize(ObjectClassDesc desc) throws NoSuchFieldException, NotFoundException, CannotCompileException  {
		try {
			return Class.forName(desc.getName()); // try to load normally one more time
		} catch (Exception e) {}
		
		final ObjectClassDesc superClassDesc = desc.getSuperClassDesc();		
		final FieldDesc[] fields = desc.getFields();
		
		final ClassPool pool = ClassPool.getDefault();
		final CtClass cc = pool.makeClass(desc.getName(), superClassDesc != null ? pool.getCtClass(superClassDesc.getName()) : null);
		
		cc.addInterface(pool.getCtClass(Doppelganger.class.getName()));
		
		//if ((Boolean) getValue(desc, "serializable")) 
		if ((desc.getFlags() & SC_SERIALIZABLE) != 0) 
			cc.addInterface(pool.getCtClass(Serializable.class.getName()));
		//if ((Boolean) getValue(desc, "externalizable")) {
		if ((desc.getFlags() & SC_EXTERNALIZABLE) != 0) {
			cc.addInterface(pool.getCtClass(Externalizable.class.getName()));
			
			cc.addMethod(CtMethod.make("public void readExternal(java.io.ObjectInput in) {}", cc));
			cc.addMethod(CtMethod.make("public void writeExternal(java.io.ObjectOutput in) {}", cc));
			// FIXME: implement this
			//new UnsupportedOperationException("need to add some methods to the class");
		}
		
		// add fields in desc
		for (final FieldDesc field : fields) {
			cc.addField(new CtField(pool.getCtClass(field.getTypeName()), field.getName(), cc));
		}		
		
		// add serialVersionUID
		if (desc.getSerialVersionUID() != null) {			
			cc.addField(new CtField(CtClass.longType,"serialVersionUID", cc){{
				setModifiers(Modifier.PRIVATE | Modifier.STATIC | Modifier.FINAL);
			}}, CtField.Initializer.constant(desc.getSerialVersionUID()));			
		} else {
			SerialVersionUID.setSerialVersionUID(cc);
		}		
		
		if (superClassDesc != null && superClassDesc.getName().equals("java.lang.Enum")) {
			String name = cc.getName();
			CtMethod method = CtMethod.make("public static " + name + "[] values() { return new " + name + "[0]; }" , cc);
			cc.addMethod(method);
			
			cc.setModifiers(cc.getModifiers() | Modifier.ENUM);
		}
		
		Class<?> clazz = cc.toClass(); 
		
		if (Enum.class.isAssignableFrom(clazz)) {
			addEnumValues((Class<? extends Enum>) clazz, desc.getEnumValues().toArray(new String[0]));
		}
		
		return clazz;
	}
	
	public static <T extends Enum> void addEnumValues(Class<T> clazz, String ... values ) {
		try {
			Constructor con = clazz.getDeclaredConstructors()[0];
			Method[] methods = con.getClass().getDeclaredMethods();
			for (Method m : methods) {
				if (m.getName().equals("acquireConstructorAccessor")) {
					m.setAccessible(true);
					m.invoke(con, new Object[0]);
				}
			}
			Field[] fields = con.getClass().getDeclaredFields();
			Object ca = null;
			for (Field f : fields) {
				if (f.getName().equals("constructorAccessor")) {
					f.setAccessible(true);
					ca = f.get(con);
				}
			}
			Method newInstanceMethod = ca.getClass().getMethod("newInstance", new Class[] { Object[].class });
			newInstanceMethod.setAccessible(true);
			Method enumConstantDictionaryMethod = Class.class.getDeclaredMethod("enumConstantDirectory", new Class<?>[0]);
			enumConstantDictionaryMethod.setAccessible(true);
			
			Map<String,T> dictionary = (Map<String, T>) enumConstantDictionaryMethod.invoke(clazz, new Object[0]);
			
			for (String value : values) {
				T instance = (T) newInstanceMethod.invoke(ca, new Object[] { new Object[] { value, Integer.MAX_VALUE } });
				dictionary.put(instance.name(), instance);			
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
