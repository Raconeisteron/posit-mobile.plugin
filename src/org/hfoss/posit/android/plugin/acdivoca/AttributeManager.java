/*
 * File: AttributeManager.java
 * 
 * Copyright (C) 2011 The Humanitarian FOSS Project (http://hfoss.org).
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

package org.hfoss.posit.android.plugin.acdivoca;   // Mobile side package
//package haiti.server.datamodel;                  // Server side package

import java.util.HashMap;
import java.util.Iterator;

/**
 * This class manages all attributes and abbreviations
 * on both the client (mobile) and server side.  
 * 
 * On the mobile side, it defines the mappings between
 * Db column names, such as 'firstname' and he abbreviation
 * used for that attribute in the SMS messasge ('f').  
 * 
 * On the server side it defines the reverse mapping, from
 * 'f' to 'firstname'.   
 * 
 */
public class AttributeManager {
	public static final String TAG = "AttributeManager";
	
	private static AttributeManager mInstance = null; 
	private static HashMap<String,String> abbreviations;

	
	public static final String ATTR_VAL_SEPARATOR = "=";
	public static final String PAIRS_SEPARATOR = ",";
	
	public static final String OUTER_DELIM = PAIRS_SEPARATOR;
	public static final String INNER_DELIM = ATTR_VAL_SEPARATOR;
	
	public static final String URL_OUTER_DELIM = "%2C";
	public static final String URL_INNER_DELIM = "%3D";
	public static final String URL_PLUS = "%2B";
	public static final String PLUS = "+";
	public static final String FORM_BENEFICIARY = null;
	
	
	// Server side widget, button, and label names used
	//  in the DataEntry forms.
	public static final String FORM_FIRST_NAME="FirstName";
	public static final String FORM_LAST_NAME="LastName";
	public static final String FORM_COMMUNE="Commune";
	public static final String FORM_SECTION="Section";
	public static final String FORM_ADDRESS="Address";
	public static final String FORM_AGE="Age";
	public static final String FORM_SEX="Sex";
	public static final String FORM_MALE="Male";
	public static final String FORM_FEMALE="Female";

	public static final String FORM_BENEFICIARY_CATEGORY="BeneficiaryCategory";
	public static final String FORM_NUMBER_IN_HOUSE= "NumberInHome"; // "Number of persons in the house:";
	public static final String FORM_HEALTH_CENTER="HealthCenter";
	public static final String FORM_DISTRIBUTION_POST="DistributionPost";
	public static final String FORM_NAME_CHILD= "NameIfChild"; // "Responsible name (if child):";
	public static final String FORM_NAME_WOMAN= "NameIfWoman";  // "Responsible name (if pregnant woman):";
	public static final String FORM_HUSBAND= "Husband"; // "Husband name (if woman):";
	public static final String FORM_FATHER= "Father";  // "Father's name (if child):";
	public static final String FORM_MOTHER_LEADER = "MotherLeader";  // "Are you a mother leader?:";
	public static final String FORM_VISIT_MOTHER = "VisitMotherLeader";   // "Do you visit a mother leader?:";
	public static final String FORM_AGRICULTURE_1 = "Agr1";  // "Is someone in your family in the";
	public static final String FORM_AGRICULTURE_2 = "Agr2";  // Agriculture Program of ACDI/VOCA?:";
	public static final String FORM_GIVE_NAME= "GiveName";  // "If yes, give the name:";

	public static final String BUTTON_YES="Yes";
	public static final String BUTTON_NO="No";
	public static final String BUTTON_MALE="MALE";
	public static final String BUTTON_FEMALE="FEMALE";
	public static final String BUTTON_INFANT_MAL="Enfant mal nourri";
	public static final String BUTTON_INFANT_PREVENTION="Enfant en prevention";
	public static final String BUTTON_MOTHER_EXPECTING="Femme enceinte";
	public static final String BUTTON_MOTHER_NURSING="Femme allaitante";
	
	public static final String FORM_DOSSIER="Dossier";
	public static final String FORM_DOB= "DateOfBirth";
	public static final String FORM_MONTHS= "MonthsRemaining";
	public static final String FORM_PRESENT= "Present";  // "Was the beneficiary present?";
	public static final String FORM_TRANSFERRED= "Transferred";  // "Should this beneficiary be transferred to a new category?";
	public static final String FORM_MODIFICATIONS= "Modifications";  // "Are modifications needed in the beneficiary's record?";
	public static final String FORM_SUSPEND= "Suspend";  // "Should the beneficiary be suspended?";
	public static final String FORM_WHY= "Why"; //"If so, why?:";
	
	
	// Abbreviated names of fields and attributes that make
	// up the Attribute side of SMS messages.
	// For example, 's=0', would represent 'status=new'.
	public static final String ABBREV_DOSSIER = "i";
	public static final String ABBREV_MESSAGE_TEXT = "tx";
	public static final String ABBREV_MESSAGE_STATUS = "ms";
	public static final String ABBREV_CREATED_AT = "t1";
	public static final String ABBREV_SENT_AT = "t2";
	public static final String ABBREV_ACK_AT = "t3";
	
	public static final String ABBREV_STATUS = "s"; 
	public static final String ABBREV_ID = "id";    
	public static final String ABBREV_AV = "AV";
	public static final String ABBREV_TYPE = "t";
	
	public static final String ABBREV_FIRST = "f";     
	public static final String ABBREV_LAST = "l";      
	public static final String ABBREV_COMMUNE = "cm";
	public static final String ABBREV_COMMUNE_SECTION = "cs";
	public static final String ABBREV_LOCALITY = "a";    
	public static final String ABBREV_DOB = "b";        
	public static final String ABBREV_SEX = "g";         
	public static final String ABBREV_CATEGORY = "c";
	public static final String ABBREV_IS_MOTHERLEADER = "ml";
	public static final String ABBREV_VISIT_MOTHERLEADER = "mv";
	public static final String ABBREV_IS_AGRI = "ag";
	public static final String ABBREV_LAND_AMT = "la";
	
	public static final String ABBREV_NUMBER_IN_HOME = "n";     
	public static final String ABBREV_HEALTH_CENTER = "h";      
	public static final String ABBREV_DISTRIBUTION_POST = "d"; 
	
	public static final String ABBREV_NAME_CHILD = "nc";
	public static final String ABBREV_NAME_WOMAN = "nw";
	public static final String ABBREV_HUSBAND = "h";
	public static final String ABBREV_FATHER = "f";
	
	// Constants for Y/N questions on the agri form
	public static final String ABBREV_IS_FARMER = "fa";
	public static final String ABBREV_IS_FISHER = "fi";
	public static final String ABBREV_IS_MUSO = "mu";
	public static final String ABBREV_IS_RANCHER = "ra";
	public static final String ABBREV_IS_STOREOWNER = "st";
	public static final String ABBREV_IS_OTHER = "ot";
	
	// This array of abbreviations is used to encode
	//  multiple fields of Y/N data -- such as 'is the 
	//  beneficiary a farmer' -- into a single integer.
	//  For example, the binary integer 'is=9' or, in binary,
	//  'is=1001' would represent 'fa and ra" or 'farmer and rancher'.
	//  Methods are available to perform the encoding.
	public static final String[] isAFields = {"fa", "fi", "mu", "ra", "st", "ot"};
	public static final String ABBREV_ISA = "is";


	// More Y/N questions for the agri form
	public static final String ABBREV_HAVE_BARREMINES = "ba";
	public static final String ABBREV_HAVE_BROUTTE = "br";
	public static final String ABBREV_HAVE_CEREAL = "ce";
	public static final String ABBREV_HAVE_HOE = "ho";
	public static final String ABBREV_HAVE_MACHETE = "ma";
	public static final String ABBREV_HAVE_PELLE = "pe";
	public static final String ABBREV_HAVE_PIOCHE = "pi";
	public static final String ABBREV_HAVE_SERPETTE = "se";
	public static final String ABBREV_HAVE_TREE = "tr";
	public static final String ABBREV_HAVE_VEG = "ve";
	public static final String ABBREV_HAVE_TUBER = "tu";
	
	// This pair of constants is used to encode/decode Y/N
	// questions regarding plant, seeds, and tools.
	public static final String ABBREV_HASA = "hs";
	public static final String[] hasAFields = {"ba", "br", "ce", "ho", "ma", 
		"pe", "pi", "se", "tr", "ve", "tu"};

	// -------------- DATA VALUES
	// These correspond to data values represented as Enums
	public static final String ABBREV_MALNOURISHED = "M";
	public static final String ABBREV_EXPECTING = "E";
	public static final String ABBREV_NURSING = "N";
	public static final String ABBREV_PREVENTION = "P";
	
	public static final String FINDS_MALNOURISHED = "MALNOURISHED";
	public static final String FINDS_EXPECTING = "EXPECTING";
	public static final String FINDS_NURSING = "NURSING";
	public static final String FINDS_PREVENTION = "PREVENTION";
	
	public static final String FINDS_FEMALE = "FEMALE";
	public static final String FINDS_MALE = "MALE";
	public static final String FINDS_NO = "NO"; 
	public static final String FINDS_YES = "YES";

	public static final String FINDS_MALNOURISHED_HA = "Enfant Mal";
	public static final String FINDS_EXPECTING_HA = "Femme Enceinte";
	public static final String FINDS_NURSING_HA = "Femme Allaitante";
	public static final String FINDS_PREVENTION_HA = "Enfant Prevention";
	
	public static final String ABBREV_FEMALE= "F";
	public static final String ABBREV_MALE= "M";
	public static final String ABBREV_YES= "Y";
	public static final String ABBREV_NO= "N";
	
	
	// ---------------- LONG COLUMNS NAMES FROM PHONE"S DB------
	// Mobile side column names used in the phone's Db. Mostly 
	//  used by DbHelper to construct the SMS messages. A raw
	//  message pulled from the phone's DB would take the form:
	//  'firstname=joe,lastname=smith,...,sex=M'
	//  Using the data defined here, it would be encoded as
	// 'f=joe,l=smith,...,g=M'
	public static final String FINDS_DOSSIER =  "dossier";
	public static final String FINDS_TYPE =  "type";
	public static final String FINDS_STATUS =  "status";
	public static final String MESSAGE_TEXT =  "message";
	public static final String FINDS_MESSAGE_STATUS =  "message_status";
	public static final String FINDS_FIRSTNAME =  "firstname";
	public static final String FINDS_LASTNAME =  "lastname";
	public static final String FINDS_ADDRESS =  "address";
	public static final String FINDS_DOB =  "dob";
	public static final String FINDS_HOUSEHOLD_SIZE =  "household_size";
	public static final String FINDS_BENEFICIARY_CATEGORY =  "beneficiary_category";
	public static final String FINDS_SEX =  "sex";
	public static final String FINDS_HEALTH_CENTER =  "health_center";
	public static final String FINDS_DISTRIBUTION_POST =  "distribution_post";
	public static final String MESSAGE_BENEFICIARY_ID =  "beneficiary_id";
	public static final String MESSAGE_CREATED_AT =  "created_time";
	public static final String MESSAGE_SENT_AT =  "sent_time";
	public static final String MESSAGE_ACK_AT =  "acknowledged_time";
	
	public static final String FINDS_Q_MOTHER_LEADER = "mother_leader";
	public static final String FINDS_Q_VISIT_MOTHER_LEADER = "visit_mother_leader";
	public static final String FINDS_Q_PARTICIPATING_AGRI = "pariticipating_agri";
	public static final String FINDS_NAME_AGRI_PARTICIPANT = "name_agri_paricipant";
	public static final String FINDS_LAND_AMOUNT = "amount_of_land";
	public static final String FINDS_RELATIVE_1 = "relative_1";
	public static final String FINDS_RELATIVE_2 = "relative_2";
	public static final String FINDS_MONTHS_REMAINING = "MonthsRemaining";

	
	public static final String FINDS_IS_FARMER = "is_farmer";
	public static final String FINDS_IS_FISHER = "is_fisher";
	public static final String FINDS_IS_MUSO = "is_MUSO";
	public static final String FINDS_IS_RANCHER = "is_rancher";
	public static final String FINDS_IS_STOREOWN = "is_store_owner";
	public static final String FINDS_IS_OTHER = "is_other";
	
	
	public static final String FINDS_HAVE_BARREAMINES = "have_barreamines";
	public static final String FINDS_HAVE_BROUETTE = "have_brouette";
	public static final String FINDS_HAVE_CEREAL = "have_cereal";
	public static final String FINDS_HAVE_HOUE = "have_houe";
	public static final String FINDS_HAVE_MACHETTE = "have_machette";
	public static final String FINDS_HAVE_PELLE = "have_pelle";
	public static final String FINDS_HAVE_PIOCHE = "have_pioche";
	public static final String FINDS_HAVE_SERPETTE = "have_serpette";
	public static final String FINDS_HAVE_TREE = "have_tree";
	public static final String FINDS_HAVE_TUBER = "have_tuber";
	public static final String FINDS_HAVE_VEGE = "have_vege";

	
	
//  These don't seem to be necessary any more, but keep as commented out
//	public static final String ABBREV_AGRICULTURE_1 = "a1";
//	public static final String ABBREV_AGRICULTURE_2 = "a2";
//	public static final String ABBREV_GIVE_NAME = "gn";
//	public static final String ABBREV_YES = "y";
//	public static final String ABBREV_NO = "n";
//	public static final String ABBREV_MALE = "M"; //"m";
//	public static final String ABBREV_FEMALE = "F"; //"f";
//	public static final String ABBREV_INFANT_CATEGORY = "ic";
//	public static final String ABBREV_INFANT_MAL = "ia";
//	public static final String ABBREV_INFANT_PREVENTION = "P"; //"ip";
//	public static final String ABBREV_MOTHER_CATEGORY = "mc";
//	public static final String ABBREV_MOTHER_EXPECTING = "E"; //"me";
//	public static final String ABBREV_MOTHER_NURSING = "N"; //"mn";
//	public static final String ABBREV_DATA = "d";
//	public static final String ABBREV_GENERAL_INFORMATION = "gi";
//	public static final String ABBREV_MCHN_INFORMATION = "mchn";
//	public static final String ABBREV_CONTROLS = "ctrl";

	
	// ------------- LONG NAMES FOR SERVER SIDE FORMS
	// Don't know whether all of these are necessary?
	// TODO:  Clean up this list
	public static final String LONG_FIRST = "firstName";
	public static final String LONG_LAST = "lastName";
	public static final String LONG_COMMUNE = "commune";
	public static final String LONG_COMMUNE_SECTION = "communeSection";
	public static final String LONG_ADDRESS = "address";
	public static final String LONG_AGE = "age";
	public static final String LONG_SEX = "sex";
	public static final String LONG_BENEFICIARY = "beneficiary";
	public static final String LONG_NUMBER_IN_HOME = "NumberInHome";
	public static final String LONG_HEALTH_CENTER = "HealthCenter";
	public static final String LONG_DISTRIBUTION_POST = "DistributionPost";
	public static final String LONG_NAME_CHILD = "nameChild";
	public static final String LONG_NAME_WOMAN = "nameWoman";
	public static final String LONG_HUSBAND = "husband";
	public static final String LONG_FATHER = "father";
	public static final String LONG_MOTHER_LEADER = "motherLeader";
	public static final String LONG_VISIT_MOTHER = "visitMotherLeader";
	public static final String LONG_AGRICULTURE_1 = "agriculture1";
	public static final String LONG_AGRICULTURE_2 = "agriculture2";
	public static final String LONG_GIVE_NAME = "giveName";
	public static final String LONG_YES = "yes";
	public static final String LONG_NO = "no";
	public static final String LONG_MALE = "male";
	public static final String LONG_FEMALE = "female";
	public static final String LONG_INFANT_CATEGORY = "InfantCategory";
	public static final String LONG_INFANT_MAL = "InfantMal";
	public static final String LONG_INFANT_PREVENTION = "InfantPrevention";
	public static final String LONG_MOTHER_CATEGORY = "MotherCategory";
	public static final String LONG_MOTHER_EXPECTING = "MotherExpecting";
	public static final String LONG_MOTHER_NURSING = "MotherNursing";
	public static final String LONG_DATA = "data";
	public static final String LONG_GENERAL_INFORMATION = "generalInformation";
	public static final String LONG_MCHN_INFORMATION = "mchnInformation";
	public static final String LONG_CONTROLS = "controls";
	public static final String LONG_STATUS = "status";
	public static final String LONG_ID = "id";
	public static final String LONG_AV = "AV";
	public static final String LONG_TYPE = "type";
	
	public static final String LONG_DOSSIER = "dossier";
		
	
	/**
	 * Private constructor means it can't be instantiated.
	 * @param activity
	 */
	private AttributeManager(){
	}
	
	public static AttributeManager getInstance(){
		mInstance = new AttributeManager();
		mInstance.init();
		//assert(mInstance != null);
		return mInstance;
	}
	
	/**
	 * Default constructor, inserts all the attributes into a HashMap
	 */
//	public AttributeManager() {
	public static void init() {
		abbreviations = new HashMap<String, String>();
//		abbreviations.put(ABBREV_ATTRIBUTE, LONG_ATTRIBUTE);
		abbreviations.put(ABBREV_FIRST, LONG_FIRST);
		abbreviations.put(ABBREV_LAST, LONG_LAST);
		abbreviations.put(ABBREV_COMMUNE, LONG_COMMUNE);
		abbreviations.put(ABBREV_COMMUNE_SECTION, LONG_COMMUNE_SECTION);
		abbreviations.put(ABBREV_LOCALITY, LONG_ADDRESS);
		abbreviations.put(ABBREV_DOB, LONG_AGE);
		abbreviations.put(ABBREV_SEX, LONG_SEX);
		
		abbreviations.put(ABBREV_CATEGORY, LONG_BENEFICIARY);
		abbreviations.put(ABBREV_NUMBER_IN_HOME, LONG_NUMBER_IN_HOME);
		abbreviations.put(ABBREV_HEALTH_CENTER, LONG_HEALTH_CENTER);
		abbreviations.put(ABBREV_DISTRIBUTION_POST, LONG_DISTRIBUTION_POST);
		abbreviations.put(ABBREV_NAME_CHILD, LONG_NAME_CHILD);
		abbreviations.put(ABBREV_NAME_WOMAN, LONG_NAME_WOMAN);
		abbreviations.put(ABBREV_HUSBAND, LONG_HUSBAND);
		abbreviations.put(ABBREV_FATHER, LONG_FATHER);
//		abbreviations.put(ABBREV_MOTHER_LEADER, LONG_MOTHER_LEADER);
//		abbreviations.put(ABBREV_VISIT_MOTHER, LONG_VISIT_MOTHER);
//		abbreviations.put(ABBREV_AGRICULTURE_1, LONG_AGRICULTURE_1);
//		abbreviations.put(ABBREV_AGRICULTURE_2, LONG_AGRICULTURE_2);
//		abbreviations.put(ABBREV_GIVE_NAME, LONG_GIVE_NAME);
//		abbreviations.put(ABBREV_YES, LONG_YES);
//		abbreviations.put(ABBREV_NO, LONG_NO);
//		abbreviations.put(ABBREV_MALE, LONG_MALE);
//		abbreviations.put(ABBREV_FEMALE, LONG_FEMALE);
//		abbreviations.put(ABBREV_INFANT_CATEGORY, LONG_INFANT_CATEGORY);
//		abbreviations.put(ABBREV_INFANT_MAL, LONG_INFANT_MAL);
//		abbreviations.put(ABBREV_INFANT_PREVENTION, LONG_INFANT_PREVENTION);
//		abbreviations.put(ABBREV_MOTHER_CATEGORY, LONG_MOTHER_CATEGORY);
//		abbreviations.put(ABBREV_MOTHER_EXPECTING, LONG_MOTHER_EXPECTING);
//		abbreviations.put(ABBREV_MOTHER_NURSING, LONG_MOTHER_NURSING);
//		abbreviations.put(ABBREV_DATA, LONG_DATA);
//		abbreviations.put(ABBREV_GENERAL_INFORMATION, LONG_GENERAL_INFORMATION);
//		abbreviations.put(ABBREV_MCHN_INFORMATION, LONG_MCHN_INFORMATION);
//		abbreviations.put(ABBREV_CONTROLS, LONG_CONTROLS);
		
		abbreviations.put(ABBREV_STATUS,LONG_STATUS);
		abbreviations.put(ABBREV_ID,LONG_ID);
		abbreviations.put(ABBREV_AV, LONG_AV);
		abbreviations.put(ABBREV_TYPE, LONG_TYPE);
		
//		// This group maps SMS abbreviations to full attribute names (for the server)
//		abbreviations.put("i", FINDS_DOSSIER);
//		abbreviations.put("t", FINDS_TYPE);
//		abbreviations.put( "s", FINDS_STATUS);
//		abbreviations.put("t", MESSAGE_TEXT);
//		abbreviations.put( "m", FINDS_MESSAGE_STATUS);
//		abbreviations.put("f", FINDS_FIRSTNAME);
//		abbreviations.put("l", FINDS_LASTNAME);
//		abbreviations.put("a", FINDS_ADDRESS);
//		abbreviations.put("b", FINDS_DOB);
//		abbreviations.put("n", FINDS_HOUSEHOLD_SIZE);
//		abbreviations.put("c", FINDS_BENEFICIARY_CATEGORY);
//		abbreviations.put("g", FINDS_SEX);
//		abbreviations.put("h", FINDS_HEALTH_CENTER);
//		abbreviations.put("d", FINDS_DISTRIBUTION_POST);
//		abbreviations.put("#", MESSAGE_BENEFICIARY_ID);
//		abbreviations.put("t1", MESSAGE_CREATED_AT);
//		abbreviations.put("t2", MESSAGE_SENT_AT);
//		abbreviations.put("t3", MESSAGE_ACK_AT);
//		
//		
		
		// ----------- DATA MAPPINGS -------------------------
		abbreviations.put(FINDS_FEMALE, ABBREV_FEMALE);
		abbreviations.put(FINDS_MALE,ABBREV_MALE);
		abbreviations.put(FINDS_NO, ABBREV_NO);
		abbreviations.put(FINDS_YES, ABBREV_YES);

		abbreviations.put(FINDS_EXPECTING, ABBREV_EXPECTING);
		abbreviations.put(FINDS_EXPECTING_HA, ABBREV_EXPECTING);
		abbreviations.put(FINDS_NURSING, ABBREV_NURSING);
		abbreviations.put(FINDS_NURSING_HA, ABBREV_NURSING);	
		abbreviations.put(FINDS_PREVENTION, ABBREV_PREVENTION);
		abbreviations.put(FINDS_PREVENTION_HA, ABBREV_PREVENTION);		
		abbreviations.put(FINDS_MALNOURISHED, ABBREV_MALNOURISHED);
		abbreviations.put(FINDS_MALNOURISHED_HA, ABBREV_MALNOURISHED);	
		
		//  Server needs abbrev --> long for these Enums
		abbreviations.put(ABBREV_EXPECTING, FINDS_EXPECTING);
		abbreviations.put(ABBREV_NURSING, FINDS_NURSING);
		abbreviations.put(ABBREV_PREVENTION, FINDS_PREVENTION);
		abbreviations.put(ABBREV_MALNOURISHED, FINDS_MALNOURISHED);
		
		//  Not sure whether these are needed on server side?
//		abbreviations.put("F", "FEMALE");
//		abbreviations.put("M", "MALE");
//		abbreviations.put("E", "EXPECTING");
//		abbreviations.put("N", "NURSING");
//		abbreviations.put("P", "PREVENTION");
//		abbreviations.put("MA", "MALNOURISHED");
				
		// ---------- MOBILE SIDE MAPPINGS TO ABBREVIATIONS
		// This group maps Db column names in the on-phone Db to SMS abbreviations
		abbreviations.put(FINDS_DOSSIER, ABBREV_DOSSIER);
		abbreviations.put(FINDS_TYPE, ABBREV_TYPE);
		abbreviations.put(FINDS_STATUS, ABBREV_STATUS);
		abbreviations.put(MESSAGE_TEXT, ABBREV_MESSAGE_TEXT);
		abbreviations.put(FINDS_MESSAGE_STATUS, ABBREV_MESSAGE_STATUS);
		abbreviations.put(FINDS_FIRSTNAME, ABBREV_FIRST);
		abbreviations.put(FINDS_LASTNAME, ABBREV_LAST);
		abbreviations.put(FINDS_ADDRESS, ABBREV_LOCALITY);
		abbreviations.put(FINDS_DOB, ABBREV_DOB);
		abbreviations.put(FINDS_HOUSEHOLD_SIZE, ABBREV_NUMBER_IN_HOME);
		abbreviations.put(FINDS_BENEFICIARY_CATEGORY, ABBREV_CATEGORY );
		abbreviations.put(FINDS_SEX, ABBREV_SEX);
		abbreviations.put(FINDS_HEALTH_CENTER, ABBREV_HEALTH_CENTER);
		abbreviations.put(FINDS_DISTRIBUTION_POST,ABBREV_DISTRIBUTION_POST);
		abbreviations.put(MESSAGE_BENEFICIARY_ID, ABBREV_ID);
		abbreviations.put(MESSAGE_CREATED_AT, ABBREV_CREATED_AT);
		abbreviations.put(MESSAGE_SENT_AT, ABBREV_SENT_AT);
		abbreviations.put(MESSAGE_ACK_AT, ABBREV_ACK_AT);
		
		abbreviations.put(FINDS_Q_MOTHER_LEADER, ABBREV_IS_MOTHERLEADER);
		abbreviations.put(FINDS_Q_VISIT_MOTHER_LEADER, ABBREV_VISIT_MOTHERLEADER);
		abbreviations.put(FINDS_Q_PARTICIPATING_AGRI, ABBREV_IS_AGRI);
		abbreviations.put(FINDS_LAND_AMOUNT, ABBREV_LAND_AMT);
		abbreviations.put(FINDS_RELATIVE_1, "r1");
		abbreviations.put(FINDS_RELATIVE_2, "r2");
		abbreviations.put(FINDS_MONTHS_REMAINING, "mo");
		abbreviations.put(FINDS_IS_FARMER, ABBREV_IS_FARMER);
		abbreviations.put(FINDS_IS_FISHER, ABBREV_IS_FISHER);
		abbreviations.put(FINDS_IS_MUSO, ABBREV_IS_MUSO);
		abbreviations.put(FINDS_IS_RANCHER, ABBREV_IS_RANCHER);
		abbreviations.put(FINDS_IS_STOREOWN, ABBREV_IS_STOREOWNER);
		abbreviations.put(FINDS_IS_OTHER, ABBREV_IS_OTHER);
		abbreviations.put(FINDS_HAVE_BARREAMINES, ABBREV_HAVE_BARREMINES);
		abbreviations.put(FINDS_HAVE_BROUETTE, ABBREV_HAVE_BROUTTE);
		abbreviations.put(FINDS_HAVE_CEREAL, ABBREV_HAVE_CEREAL);
		abbreviations.put(FINDS_HAVE_HOUE, ABBREV_HAVE_HOE);
		abbreviations.put(FINDS_HAVE_MACHETTE, ABBREV_HAVE_MACHETE);
		abbreviations.put(FINDS_HAVE_PELLE, ABBREV_HAVE_PELLE);
		abbreviations.put(FINDS_HAVE_PIOCHE, ABBREV_HAVE_PIOCHE);
		abbreviations.put(FINDS_HAVE_SERPETTE, ABBREV_HAVE_SERPETTE);
		abbreviations.put(FINDS_HAVE_TREE, ABBREV_HAVE_TREE);
		abbreviations.put(FINDS_HAVE_TUBER, ABBREV_HAVE_TUBER);
		abbreviations.put(FINDS_HAVE_VEGE, ABBREV_HAVE_VEG);
	
		
		// ---------- ACDIV/VOCA DATA -----------------
		// There should be mappings for all fixed data
		// used on the phone.
//		<string-array name="distribution_point_names"> 
		abbreviations.put("Centre Platon Cedre",  "d1" );
		abbreviations.put("Point Fixe Ka Tousen",  "d2" );
		abbreviations.put("Anse a Pitres",  "d3" );
		abbreviations.put("Dispensaire Banane",  "d4" );
		abbreviations.put("Pt fixe Calumette",  "d5" );
		abbreviations.put("Centre Belle-Ance",  "d6" );
		abbreviations.put("Dispensaire Mapou", "d7"  );
		abbreviations.put("Pt fixe Baie d_orange", "d8"  );
		abbreviations.put("Dispensaire marbriole", "d9"  );	
		abbreviations.put("Pt fixe Corail Lamothe", "d10"  );
		abbreviations.put("Pt fixe Pichon",   "d11" );	
		abbreviations.put("Pt Fixe Bel-air",  "d12"  );	
		abbreviations.put("Labiche",   "d13" );	
		abbreviations.put("Centre St Joseph",   "d14" );	
		abbreviations.put("Dispensaire Ste rose de lima",  "d15"  );	
		abbreviations.put("Dispensaire Boucan Belier", "d16"   );	
		abbreviations.put("Dispensaire Ricot",   "d17" );	
		abbreviations.put("Pt Fixe Macieux",  "d18"  );	
		abbreviations.put("Point Fixe de Mayette",   "d19" );	
		abbreviations.put("Point Fixe de Amazone",   "d20" );
		abbreviations.put("Dispensaire Grand Gosier", "d21"   );
		abbreviations.put("Dispensaire Bodarie",   "d22" );
		abbreviations.put("Pt fixe Boulay",  "d23"  );
		abbreviations.put("Centre Sacre Coeur",   "d24" );
		abbreviations.put("Dispensaire de Savane Zombi",  "d25"  );
		abbreviations.put("Dispensaire de Bleck/ Mar Mirande",   "d26" );
		
//		<string-array name="health_center_names"> 
		abbreviations.put("Centre de santé une", "h1" );
		abbreviations.put("Centre de santé deux", "h2"  );
		abbreviations.put("Centre de santé trois", "h3" );
		abbreviations.put("Centre de santé quatre", "h4" );
		abbreviations.put("Centre de santé cinq", "h4" );
		
	}
	
	/**
	 * Server side method -- Converted to remove reference to server side enum
	 * Maps a short form attribute or value name to a long form
	 * @param abbreviatedAttributes TRUE if abbreviated, FALSE if not
	 * @param s the String to be mapped to long
	 * @return the long form of the String
	 */
//	public static String mapToLong(Beneficiary.Abbreviated abbreviatedAttributes, String s){
	public static String mapToLong(boolean abbreviatedAttributes, String s){
		if (abbreviatedAttributes) {
			String str = abbreviations.get(s);
			if (str != null)
				return str;
			else 
				return "";
		}
		else
			return s;
	}
	
//	/**
//	 * Server side method.
//	 * Maps the short form field names to long form
//	 * @param abbreviatedAttributes TRUE if abbreviated, FALSE if not
//	 * @param s the String to be mapped to long
//	 * @return the long form of the String
//	 */
//	public String mapToLong(SmsMessage.Abbreviated abbreviatedAttributes, String s){
//		if (abbreviatedAttributes == SmsMessage.Abbreviated.TRUE) {
//			String str = abbreviations.get(s);
//			if (str != null)
//				return str;
//			else 
//				return "";
//		}
//		else
//			return s;
//	}
	
	/**
	 * Mobile side method. 
	 * Converts attribute-value pairs to abbreviated attribute-value pairs.
	 * @param attr  a long attribute name, e.g. 'firstname'
	 * @param val   a data value, e.g. 'Joseph'
	 * @return  a string of the form a1=v1,a2=b2, ..., aN=vN
	 */
	public static String convertAttrValPairToAbbrev(String attr, String val) {
		String attrAbbrev = getMapping(attr);
		String valAbbrev = getMapping(val);
		
		return attrAbbrev + ATTR_VAL_SEPARATOR + valAbbrev;
	}
	
//	/**
//	 * Convert a value to an abbreviated value. This is mostly
//	 * used for the names of health centers and distribution posts.
//	 * @param val a String of the form "Name of Some Health Center"
//	 * @return a String of the form "1" representing that health center
//	 */
//	public static String convertValToAbbrev(String val) {
//		String abbrev = abbreviations.get(val);
//		if (abbrev != null)
//			return abbrev;
//		else 
//			return val;
//	}
//	
	/**
	 * Mobile side. 
	 * Convert a value to an abbreviated value. This is mostly
	 * used for the names of health centers and distribution posts.
	 * @param val a String of the form "Name of Some Health Center"
	 * @return a String of the form "h1" representing that health center
	 */
	public static String getMapping(String val) {
		System.out.println("val = " + val);
		String result = abbreviations.get(val);
		if (result != null) {
				return result;
		}
		else 
			return val;
	}
	
	/**
	 * A simple test method. 
	 */
	public void testAllAttributes() {
		Iterator<String> it = abbreviations.keySet().iterator();
		//Iterator<String> it = abbreviations.values().iterator();
		while (it.hasNext()) { 
			String s = it.next();
			
			System.out.println(s +  " = " + abbreviations.get(s));
		}
	}

	/**
	 * Replaces some attribute/value Y/N pairs with an encoded base-2 number.
	 * Example:  'fa=1,ra=1' would be converted to 9 decimal, which 1001 in 
	 * binary or 9 = 1001 = 2^0 + 2^3 where 'fa' and 'ra' are contained in the
	 * array  ["fa", "fi", "mu", "ra", "st", "ot"] .  
	 * @param smsMsg the message to be encoded
	 * @param attributes an array of attribute names: 
	 * @param newAttr a String giving the name for the int attribute in the message
	 *  -- e.g., 'is' or 'hs'
	 * @return  an SMS where an attr/val pair such as 'is=9' replaces 'fa=1,ra=1'.  
	 */
	public static String encodeBinaryFields(String smsMsg, String[] attributes, String newAttr) {
		int sum = 0;
		String result = "";
		String pairs[] = smsMsg.split(PAIRS_SEPARATOR);  
		
		// Split each attr/value pair in the Sms message into its attr and its val
		for (int k = 0; k < pairs.length; k++) {
			String attrval[] = pairs[k].split(ATTR_VAL_SEPARATOR);
			String attr = "", val = "";
			if (attrval.length == 2) {
				attr = attrval[0];
				val = attrval[1];
			} else if (attrval.length == 1) {
				attr = attrval[0];
			}
			//System.out.println(TAG + "attr = " + attr + " val = " + val);

			// If the attribute (eg 'fa') is contained in the attributes array
			// its value represents 2^x where x is its index in the array. Add
			// that value to the running total.  If the attribute is not in the
			// array, just put it back in the result string.
			int x = getIndex(attributes, attr);
			if (x != -1) {
				int valInt = Integer.parseInt(val);
				if (valInt == 1)
					sum += Math.pow(2, x);              // Compute a running total
			} else {
				result += attr + ATTR_VAL_SEPARATOR + val + PAIRS_SEPARATOR;
			}
		}
		result += newAttr + ATTR_VAL_SEPARATOR + sum;	
		
		// To test the encoding, print out the decoding
		System.out.println(TAG +  "Decoded sum = " + decodeBinaryFieldsInt(sum, attributes));
		System.out.println(TAG +  "Result= " + result);

		return result;
	}
	
	/**
	 * This is the reverse mapping of encodeBinaryFields.  It takes a
	 * decimal (base-10) number and converts it to binary where 1 bits
	 * correspond to the attributes in the attributes array.  Those are
	 * returns as a String of attr=val pairs.  For example the codedInt
	 * 9 would translate to 1001 or 2^0 + 2^3 or 'fa=1,ra=1' using the
	 * attributes array ["fa", "fi", "mu", "ra", "st", "ot"]
	 * @param codedInt is a base-10 number
	 * @param attributes is an array of attribute abbreviations
	 * @return  a string of attr=val pairs
	 */
	public static String decodeBinaryFieldsInt(int codedInt, String[] attributes) {
		String result = "";
		int len = attributes.length;    
		
		// Moving right to left in the attributes array, use the array index
		// as the exponent in 2^k, subtracting 2^k from the codedInt on each 
		// iteration. If 2^k can be subtracted from codedInt that means that
		// attributes[k] contains an attribute whose value should be 1. So
		// insert that attr=1 into the result string.
		for (int k = len; k >= 0; k--)  {
			int pow2 = (int) Math.pow(2, k);
			if (codedInt >= pow2) {
				codedInt -= pow2;     // Subtract that power of 2
				result += attributes[k] + ATTR_VAL_SEPARATOR + "1" + PAIRS_SEPARATOR;
			}
		}
		System.out.println(TAG + "Result = " + result);
		return result;	
	}
	

	/**
	 * Helper method to find the location of the String s in a String array.
	 * @param arr
	 * @param s
	 * @return returns the index of s in arr or -1 if it is not contained therein
	 */
	private static int getIndex(String []arr, String s)  {
		for (int k = 0; k < arr.length; k++) 
			if (s.equals(arr[k])) 
				return k;
		return -1;
	}
	
	
	/**
	 * TODO:  This method should thoroughly test this class. For example, 
	 * print out all mappings of short to long.
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Hello Attribute Manager");
		AttributeManager am = AttributeManager.getInstance(); // new AttributeManager();
		System.out.print(am.mapToLong(false, "f"));
		//am.testAllAttributes();
		//System.out.println("Str= " + am.abbreviations.get("M"));
		
	}

}