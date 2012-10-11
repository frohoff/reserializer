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

public class Test {
	private static final String TMPDIR = System.getProperty("java.io.tmpdir");
	private static final File file = new File(TMPDIR + File.separator + "test.ser");
	
	@org.junit.Test
	public void test() throws FileNotFoundException, IOException, ClassNotFoundException, IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
//		ObjectInputStream in = (ObjectInputStream) new Enhancer() {{
//			setSuperclass(YsoserialObjectInputStream.class);
//			setCallback(new MethodInterceptor() {
//				public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
//					System.out.println("invoking " + method);
//					return proxy.invokeSuper(obj, args);
//				}
//			});
//		}}.create(new Class[]{InputStream.class}, new Object[]{ new BufferedInputStream(new FileInputStream(file)) });
		ObjectInputStream in = new YsoserialObjectInputStream(new BufferedInputStream(new FileInputStream(file)));
		Object obj = in.readObject();
		
		ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(file.getAbsolutePath() + ".reser")));
		out.writeObject(obj);
		
		ObjectInputStream in2 = new YsoserialObjectInputStream(new BufferedInputStream(new FileInputStream(new File(file.getAbsolutePath() + ".reser"))));
		Object obj2 = in2.readObject();
		System.out.println(obj2);
	}
}
