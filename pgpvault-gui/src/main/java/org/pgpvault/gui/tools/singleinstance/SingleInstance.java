package org.pgpvault.gui.tools.singleinstance;

public interface SingleInstance {
	/**
	 * @return true if this instance happen to be "Primary" instance.
	 */
	boolean tryClaimPrimaryInstanceRole(PrimaryInstanceListener primaryInstanceListener);

	boolean isPrimaryInstance();

	/**
	 * @return true if other (Primary) instance confirmed that args are received
	 */
	boolean sendArgumentsToOtherInstance(String[] args);
}
