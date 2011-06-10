/*
 * File: Beneficiary.java
 * 
 * Copyright (C) 2011 Humanitarian FOSS Project (http://hfoss.org).
 * 
 * This file is part of POSIT-Haiti Server.
 *
 * POSIT-Haiti Server is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation; either version 3.0 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not visit http://www.gnu.org/licenses/gpl.html.
 *
 */

package org.hfoss.posit.android.plugin.acdivoca;

import java.util.Locale;
import java.util.ResourceBundle;

import java.util.Locale;
import java.util.ResourceBundle;

public class Beneficiary {
	
	public enum Sex {MALE, FEMALE, UNKNOWN};
	public enum InfantCategory {MALNOURISHED, PREVENTION, UNKNOWN};
	public enum MotherCategory {EXPECTING, NURSING, UNKNOWN};
	public enum Abbreviated {TRUE, FALSE};
	public enum Status {NEW, PENDING, PROCESSED, UNKNOWN};

	private String firstName = "";
	private String lastName = "";
	private String address = "";
	private String commune = "";
	private String communeSection = "";
	private int age = -1;
	private Sex sex = Sex.UNKNOWN; 
	private int numberInHome = -1;
	private InfantCategory infantCategory = InfantCategory.UNKNOWN;
	private MotherCategory motherCategory = MotherCategory.UNKNOWN;
	private int id = -1;
	private Status status = Status.UNKNOWN;
	//private int status ;


	/**
	 * Default constructor
	 */
	public Beneficiary() {
		this("first","last", 
				"commune", "section",
				0, 
				Sex.MALE,
				InfantCategory.PREVENTION, MotherCategory.NURSING, 
				0);
	}

	/**
	 * Constructs a beneficiary from a string of attribute=value pairs of
	 * the following form: attr1=value1&attr2=val2& ... &attrN=valueN
	 * @param attributeValueString
	 * @param abbreviatedAttributes true iff the attribute names are abbreviated, eg. fn
	 */
	public Beneficiary (String attributeValueString, Abbreviated abbreviatedAttributes) {
		System.out.println("Splitting " + attributeValueString);
		split(attributeValueString, ",", "=", abbreviatedAttributes);
		
	}

	public String getRandomDossierNumber(){
		return "" + (int)(Math.random() * 1000000);
	}
	public Beneficiary(String firstName, String lastName, 
			String commune, String communeSection,
			int age,
			Sex sex, 
			InfantCategory infantCategory, 
			MotherCategory motherCategory, 
			int numberInHome) {
		this.firstName = firstName;
		this.lastName = lastName;
		this.commune = commune;
		this.communeSection = communeSection;
		this.sex = sex;
		this.infantCategory = infantCategory;
		this.motherCategory = motherCategory;
		this.numberInHome = numberInHome;
	}
	
	/**
	 * Splits a string of the form attr1=value1&attr2=val2& ... &attrN=valueN
	 * @param s is the string being split
	 * @param outerDelim defines the delimiter
	 * @param abbreviated are the attributes abbreviated
	 */
	private void split(String s, String outerDelim, String innerDelim, Abbreviated abbreviated) {
		String attrvalPairs[] = s.split(outerDelim);				// Pairs like attr1=val1
		for (int k = 0; k < attrvalPairs.length; k++) {
			String attrval[] = attrvalPairs[k].split(innerDelim);	// Puts attr in 0 and val in 1
			
			AttributeManager am = AttributeManager.getInstance(); // new AttributeManager();
			String longAttr = am.mapToLong(abbreviated, attrval[0]);
			
			try {
				if (longAttr.equals(AttributeManager.LONG_ID))
					id = Integer.parseInt(attrval[1]);
				else if (longAttr.equals(AttributeManager.LONG_STATUS))
					status = Status.valueOf(attrval[1]);
				else if (longAttr.equals(AttributeManager.LONG_FIRST))
					firstName=attrval[1];
				else if (longAttr.equals(AttributeManager.LONG_LAST))
					lastName=attrval[1];
				else if (longAttr.equals(AttributeManager.LONG_ADDRESS))
					address = attrval[1];
				else if (longAttr.equals(AttributeManager.LONG_COMMUNE))
					commune=attrval[1];
				else if (longAttr.equals(AttributeManager.LONG_COMMUNE_SECTION))
					communeSection=attrval[1];
				else if (longAttr.equals(AttributeManager.LONG_INFANT_CATEGORY))
					infantCategory=InfantCategory.valueOf(attrval[1]);
				else if (longAttr.equals(AttributeManager.LONG_MOTHER_CATEGORY))
					motherCategory=MotherCategory.valueOf(attrval[1]);
				else if (longAttr.equals(AttributeManager.LONG_SEX))
					sex=Sex.valueOf(attrval[1]);
				else if (longAttr.equals(AttributeManager.LONG_AGE))
					age=Integer.parseInt(attrval[1]);
				else if (longAttr.equals(AttributeManager.LONG_NUMBER_IN_HOME))
					numberInHome=Integer.parseInt(attrval[1]);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
//			else if (longAttr.equals("id"))
//				id = Integer.parseInt(attrval[1]);
//			else if (longAttr.equals("status"))
//				status = Integer.parseInt(attrval[1]);
		}
	}
	
	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}
	
	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
	public String getCommune() {
		return commune;
	}

	public void setCommune(String commune) {
		this.commune = commune;
	}

	public String getCommuneSection() {
		return communeSection;
	}

	public void setCommuneSection(String communeSection) {
		this.communeSection = communeSection;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public Sex getSex() {
		return sex;
	}

	public void setSex(Sex sex) {
		this.sex = sex;
	}

	public int getNumberInHome() {
		return numberInHome;
	}

	public void setNumberInHome(int numberInHome) {
		this.numberInHome = numberInHome;
	}

	public InfantCategory getInfantCategory() {
		return infantCategory;
	}

	public void setInfantCategory(InfantCategory infantCategory) {
		this.infantCategory = infantCategory;
	}

	public MotherCategory getMotherCategory() {
		return motherCategory;
	}

	public void setMotherCategory(MotherCategory motherCategory) {
		this.motherCategory = motherCategory;
	}
	
	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}
	
	public String toString(String separator) {
		return "id = " + id + separator + 
		"status = " + status + separator + 
		"firstName = " + firstName + separator +
		"lastName = " + lastName + separator +
		"commune = " + commune + separator +
		"communeSection = " + communeSection + separator +
		"age = " + age + separator +
		"numberInHome = " + numberInHome + separator +
		"infantCategory = " + infantCategory + separator +
		"motherCategory = " + motherCategory;
	}
	
	public String toString() {
		return toString(System.getProperty("line.separator"));
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Beneficiary ben = new Beneficiary("AV=1,i=068MP-FAT,t=0,st=1,fn=Denisana,ln=Balthazar,a=Saint Michel,b=1947/11/31,s=F,c=P,d=24", Abbreviated.TRUE);
		// System.out.println(ben.toString("&"));
		// System.out.println(ben.toString());
		System.out.println(ben.toString());
	}

}
