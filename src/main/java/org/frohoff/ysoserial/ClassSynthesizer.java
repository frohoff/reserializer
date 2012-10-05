package org.frohoff.ysoserial;

import static privilegedaccessor.StrictPA.getValue;

import java.io.Externalizable;
import java.io.ObjectStreamClass;
import java.io.ObjectStreamField;
import java.io.Serializable;
import java.lang.reflect.Modifier;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.NotFoundException;

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

}
