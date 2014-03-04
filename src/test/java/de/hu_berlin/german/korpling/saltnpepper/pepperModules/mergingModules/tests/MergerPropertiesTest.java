package de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.hu_berlin.german.korpling.saltnpepper.pepperModules.mergingModules.MergerProperties;

public class MergerPropertiesTest {
	
	private MergerProperties fixture= null;
	public MergerProperties getFixture() {
		return fixture;
	}

	public void setFixture(MergerProperties fixture) {
		this.fixture = fixture;
	}
	@Before
	public void setUp(){
		setFixture(new MergerProperties());
	}
	
	@Test
	public void testGetPunctuations(){
		assertNotNull(getFixture().getPunctuations());
		char[] punctuations = {'.',',',':',';','!','?','(',')','{','}','<','>'};
		assertEquals(punctuations.length, getFixture().getPunctuations().size());
		for (int i=0; i< punctuations.length; i++){
			assertTrue(getFixture().getPunctuations().contains(punctuations[i]));
		}
	}
}
