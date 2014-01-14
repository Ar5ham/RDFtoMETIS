package cs.uga.edu.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class Util {

	/*********************************************************************
	 * @param string: string to create hash for.
	 * @return 64 bit hash of the string input
	 */
	public static long hash64(String string) {

		long h = 1125899906842597L; // prime
		int len = string.length();

		for (int i = 0; i < len; i++) {
			h = 31 * h + string.charAt(i);
		}
		return h;
	}

	/*********************************************************************
	 * Returns the contents of a File as a String.
	 * @param   f       File to convert to a String
	 * @return  contents of the specified File as a String
	 */
	public static String fileToString(File f) throws IOException{
		return new String(getContents(f));
	}

	/*********************************************************************
	 * Returns the contents of a File as a byte array.
	 * @param   f       File to convert to a byte array
	 * @return  contents of the specified File
	 */
	public static byte[] getContents(File f) throws IOException{
		FileChannel fc = new FileInputStream(f).getChannel();
		ByteBuffer buffer = ByteBuffer.allocate((int)fc.size());
		fc.read(buffer);
		fc.close();
		return buffer.array();
	}

	/*********************************************************************
	 * Converts the contents of a String to a File.
	 * @param   s       String to convert to a File
	 * @param   f       File which the String will be written to.
	 * @param   append  append mode on the File.
	 */
	public static void stringToFile(String s, File f, boolean append) throws IOException{
		if (f.getParentFile() != null && !f.getParentFile().exists()) {
			f.getParentFile().mkdirs();
		}
		FileChannel fc = new FileOutputStream(f,append).getChannel();
		byte[] b = s.getBytes();
		ByteBuffer buffer = ByteBuffer.wrap(b);
		fc.write(buffer);
		fc.close();
	}

}
