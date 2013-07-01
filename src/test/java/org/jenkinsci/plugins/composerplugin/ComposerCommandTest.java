package org.jenkinsci.plugins.composerplugin;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ComposerCommandTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testInstall() {
		ComposerCommand composerCommand = new ComposerCommand();
		try{
			composerCommand.install();
		}catch(IOException e){
			fail("Not yet implemented");	
		}
		
	
	}
	

}
