package org.pgptool.gui.filecomparison;

import org.summerb.approaches.jdbccrud.common.DtoBase;

/**
 * This structure is used to store file size and checksum. This is how we see if
 * target file is different from source file. Do not confuse this class with
 * fingerprints in cryptography
 * 
 * @author sergeyk
 *
 */
public class Fingerprint implements DtoBase {
	private static final long serialVersionUID = -3893655717810653990L;

	private long size;
	private String checksum;

	public Fingerprint() {
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String crc) {
		this.checksum = crc;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((checksum == null) ? 0 : checksum.hashCode());
		result = prime * result + (int) (size ^ (size >>> 32));
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
		Fingerprint other = (Fingerprint) obj;
		if (checksum == null) {
			if (other.checksum != null)
				return false;
		} else if (!checksum.equals(other.checksum))
			return false;
		if (size != other.size)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Fingerprint [size=" + size + ", checksum=" + checksum + "]";
	}
}
