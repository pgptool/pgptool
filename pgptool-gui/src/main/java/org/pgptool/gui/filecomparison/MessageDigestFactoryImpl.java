package org.pgptool.gui.filecomparison;

import java.security.MessageDigest;

import org.bouncycastle.jcajce.provider.digest.SHA1;

public class MessageDigestFactoryImpl implements MessageDigestFactory {
	@Override
	public MessageDigest createNew() {
		return new SHA1.Digest();
	}
}
