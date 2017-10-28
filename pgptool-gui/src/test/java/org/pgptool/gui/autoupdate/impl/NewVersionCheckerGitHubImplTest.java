package org.pgptool.gui.autoupdate.impl;

import static org.junit.Assert.*;

import org.junit.Test;
import org.pgptool.gui.autoupdate.api.UpdatePackageInfo;

public class NewVersionCheckerGitHubImplTest {

	@Test
	public void testFindNewUpdateIfAvailable() throws Exception {
		NewVersionCheckerGitHubImpl f = new NewVersionCheckerGitHubImpl();
		f.setConfiguredVersion("0.0.0.0");

		UpdatePackageInfo result = f.findNewUpdateIfAvailable();
		assertNotNull(result);
	}

	@Test
	public void testGetCurrentVersion_ExpectNullByDefault() {
		NewVersionCheckerGitHubImpl f = new NewVersionCheckerGitHubImpl();
		assertEquals(f.getCurrentVersion(), NewVersionCheckerGitHubImpl.VERSION_UNRESOLVED);
	}

	@Test
	public void testGetCurrentVersion_ExpectHardcoded() {
		NewVersionCheckerGitHubImpl f = new NewVersionCheckerGitHubImpl();
		f.setConfiguredVersion("0.0.0.0");
		assertEquals(f.getCurrentVersion(), "0.0.0.0");
	}

}
