package name.kratunov.mealbooking;

public class Utils {
	static public String concatStringArray(String[] array, char delim)
	{
		if (array == null || array.length == 0) return null;
		
		StringBuilder strBld = new StringBuilder();
		for(String item: array)
		{
			if(item != null)
			{
				strBld.append(item);
				strBld.append(delim);
			}
		}
		strBld.deleteCharAt(strBld.length()-1);
		return strBld.toString();
	}
}
