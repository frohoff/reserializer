import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


public class Simple implements Serializable {
	private String string = "abc";
	private String[] strings = new String[]{ "1", "2" };
	private Map<String,String> map = new HashMap<String,String>(){{ put("a", "b"); put("c","d"); }};
}
