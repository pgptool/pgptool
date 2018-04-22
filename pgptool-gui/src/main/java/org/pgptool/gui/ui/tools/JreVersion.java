package org.pgptool.gui.ui.tools;

/**
 * This class will abstract application from JRE versioning schema and provide
 * comparing capabilities.
 * 
 * NOTE: I've added same class to launch4j code
 * 
 * @author sergeyk
 *
 */
public class JreVersion implements Comparable<JreVersion> {
	public static final String VERSION_PATTERN = "((\\d\\.){2}\\d(_\\d+)?)|(\\d+(\\.\\d+)*)";

	private int x1;
	private int x2;
	private int x3;
	private int x4;

	public static JreVersion parseString(String versionStr) {
		JreVersion ret = new JreVersion();
		if (versionStr == null || versionStr.trim().length() == 0) {
			return ret;
		}
		if (!versionStr.matches(VERSION_PATTERN)) {
			// TODO: Move to message to messages file and use Validator.checkTrue or
			// something like that used throughout the rest of application
			throw new IllegalArgumentException(
					"JRE version is of a wrong format. It should be either x.x.x[_xx] or x.x.x");
		}

		// TODO: Add full support to all crazy possible version formats. Official regex:
		// [1-9][0-9]*((\.0)*\.[1-9][0-9]*)*
		// see: http://openjdk.java.net/jeps/223

		String[] parts = versionStr.split("[\\._]");
		int first = Integer.parseInt(parts[0]);
		if (first >= 9) {
			// java 9+ version schema
			ret.x1 = 1;
			ret.x2 = first;
			if (parts.length >= 2) {
				ret.x3 = Integer.parseInt(parts[1]);
				if (parts.length >= 3) {
					ret.x4 = Integer.parseInt(parts[2]);
				}
			}
		} else {
			// java <= 1.8 version schema
			ret.x1 = first;

			ret.x2 = Integer.parseInt(parts[1]);
			ret.x3 = Integer.parseInt(parts[2]);
			if (parts.length == 4) {
				ret.x4 = Integer.parseInt(parts[3]);
			}
		}

		return ret;
	}

	@Override
	public String toString() {
		if (x2 >= 9) {
			return "" + x2 + "." + x3 + "." + x4;
		}

		return "" + x1 + "." + x2 + "." + x3 + (x4 > 0 ? "_" + x4 : "");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + x1;
		result = prime * result + x2;
		result = prime * result + x3;
		result = prime * result + x4;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JreVersion other = (JreVersion) obj;
		if (x1 != other.x1)
			return false;
		if (x2 != other.x2)
			return false;
		if (x3 != other.x3)
			return false;
		if (x4 != other.x4)
			return false;
		return true;
	}

	@Override
	public int compareTo(JreVersion o) {
		if (this.equals(o)) {
			return 0;
		}

		if (x1 != o.x1) {
			return x1 - o.x1;
		}
		if (x2 != o.x2) {
			return x2 - o.x2;
		}
		if (x3 != o.x3) {
			return x3 - o.x3;
		}
		if (x4 != o.x4) {
			return x4 - o.x4;
		}

		throw new IllegalStateException(
				"We shouldn't be here, it would mean versions are equald and shouldn't been handled by equals check");
	}
}
