package com.testcase;

import com.V2.jni.util.XmlAttributeExtractor;

import junit.framework.TestCase;

public class TestCaseXmlAttributeExtractor extends TestCase {

	public void testExtractAttribute() {
		String ho ="<user  address=\" do \" authtype='1' birthday='2000-01-01' job='' mobile='11' nickname='whoa1'  sex='0'  sign='' telephone='288 '> <videolist/> </user>";
		String str = XmlAttributeExtractor
				.extractAttribute(
						ho,
						"birthday");
		assertEquals(str, "2000-01-01");
		
		
		str = XmlAttributeExtractor
				.extractAttribute(
						ho,
						"job");
		assertEquals(str, "");
		

		str = XmlAttributeExtractor
				.extractAttribute(
						ho,
						"sign");
		assertEquals(str, "");
		
		str = XmlAttributeExtractor
				.extractAttribute(
						ho,
						"authtype");
		assertEquals(str, "1");
		
	}
}
