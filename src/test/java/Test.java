import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import org.frohoff.ysoserial.Doppelganger;
import org.frohoff.ysoserial.YsoserialObjectInputStream;

public class Test {
	private static final String TMPDIR = System.getProperty("java.io.tmpdir");
	private static final File file = new File(TMPDIR + File.separator + "test.ser");
	
	@org.junit.Test
	public void test() throws FileNotFoundException, IOException, ClassNotFoundException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		ObjectInputStream in = (ObjectInputStream) new Enhancer() {{
			setSuperclass(YsoserialObjectInputStream.class);
			setCallback(new MethodInterceptor() {
				public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
					System.out.println("invoking " + method);
					return proxy.invokeSuper(obj, args);
				}
			});
		}}.create(new Class[]{InputStream.class}, new Object[]{ new FileInputStream(file) });
//		ObjectInputStream in = new YsoserialObjectInputStream(new FileInputStream(file));
		Object obj = in.readObject();
		System.out.println(obj + ": " + Doppelganger.Utils.getFieldValues(obj));
	}
}
