package org.pgptool.gui.encryption.implpgp;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.pgptool.gui.encryption.implpgp.KeyGeneratorServicePgpImpl.KeyPairParams;

public class ProactivelyGenerateMasterKeyPair implements Callable<KeyPair> {
	static final Logger log = Logger.getLogger(ProactivelyGenerateMasterKeyPair.class);

	private KeyPairParams keyPairParams;

	public ProactivelyGenerateMasterKeyPair(KeyPairParams keyPairParams) {
		this.keyPairParams = keyPairParams;
	}

	@Override
	public KeyPair call() throws Exception {
		log.debug("Generating master key pair for parameters: " + keyPairParams);
		KeyPair ret = generateKeyPair(keyPairParams);
		log.debug("Master key generated: " + keyPairParams);
		return ret;
	}

	public static KeyPair generateKeyPair(KeyPairParams keyPairParams) throws Exception {
		try {
			SecureRandom secureRandom = new SecureRandom();
			secureRandom.setSeed(System.currentTimeMillis() + secureRandom.nextLong());

			log.debug("Calculating KeyPair " + keyPairParams);
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(keyPairParams.algorithm,
					keyPairParams.provider);
			keyPairGenerator.initialize(keyPairParams.keysize, secureRandom);
			log.info("Started key generation");
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
			log.info("Key generation is complete");

//			byte[] encoded = Base64.getEncoder().encode(keyPair.getPrivate().getEncoded());
//			String pkey = new String(encoded, "UTF-8");
//			log.debug("generated private key: " + pkey);

			return keyPair;
		} catch (Throwable t) {
			log.error("Failed to generate DSA keypair " + keyPairParams, t);
			throw new Exception("Failed to generate DSA keypair" + keyPairParams, t);
		}
	}
}