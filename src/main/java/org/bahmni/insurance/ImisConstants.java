package org.bahmni.insurance;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ImisConstants {

	public final static int OPENIMIS_FHIR = 0;
	public final static int OPENMRS_FHIR = 1;
	public final static int OPENMRS_ODOO = 2;
	public final static int OPENMRS_API = 3;

	// Reference :
	// https://github.com/openimis/web_app_vb/blob/master/IMIS_BL/General.vb#L914

	public static final Map<Integer, String> ERROR_CODE_TO_TEXT_MAP = Collections
			.unmodifiableMap(new HashMap<Integer, String>() {
				private static final long serialVersionUID = 1L;
				{
					put(-1, "rejected by medical officer");
					put(0, "accepted");
					put(1, "item service not in the register");
					put(2, "item service not in the pricelist");
					put(3, "item service not covered by policy");
					put(4, "item service doesnt comply with limitation");
					put(5, "item service doesnt comply with frequency");
					put(6, "item service duplicated");
					put(7, "item service not valid insurance number");
					put(8, "diagnosis code not in the currentlist");
					put(9, "target date of provision healthcare invalid");
					put(10, "item service doesnot comply with care constraint");
					put(11, "maximum number of inpatient exceeded");
					put(12, "maximum number of outpatient exceeded");
					put(13, "maximum number of consultations exceeded");
					put(14, "maximum number of surgeries exceeded");
					put(15, "maximum number of deliveries exceeded");
					put(16, "maximum number of provisions exceeded");
					put(17, "item service cannot be covered with waitingperiod");
					put(18, "na");
					put(19, "maximum number of antenetal exceeded");
				}
			});

	public class CLAIM_ITEM_STATUS {
		public final static String PASSED = "passed";
		public final static String REJECTED = "rejected";
	}

	public class CLAIM_ADJ_CATEGORY {
		public final static String GENERAL = "general";
		public final static String REJECTED_REASON = "rejected_reason";
	}

	public static class CLAIM_VISIT_TYPE {
		public final static String OPD = "OPD";
		public final static String IPD = "IPD";
		public final static String OTHERS_CODE = "O";
		public final static String EMERGENCY_CODE = "E";
		public final static String REFFERALS_CODE = "R";
		public final static String OTHERS = "others";
		public final static String EMERGENCY = "emergency";
		public final static String REFFERALS = "referrals";
	}

	public static enum FHIR_RESOURCE_TYPE {
		CLAIM("CLAIM"), CLAIMRESPONSE("CLAIMRESPONSE"), ELIGIBILITYREQUEST("ELIGIBILITYREQUEST"), ELIGIBILITYRESPONSE(
				"ELIGIBILITYRESPONSE"), CLAIMTRACK("CLAIMTRACK");
		private String value;
		FHIR_RESOURCE_TYPE(String value) {
			this.value = value;
		}
		public String getValue() {
			return value;
		}
	};

	public static enum CLAIM_OUTCOME {
		REJECTED("rejected"), ENTERED("entered"), CHECKED("checked"), PROCESSED("processed"), VALUATED("valuated");

		private String outcome;

		CLAIM_OUTCOME(String outcome) {
			this.outcome = outcome;
		}

		public String getOutCome() {
			return outcome;
		}
	};
	
	public final static String FHIR_VERSION = "STU3";
	public final static String FHIR_CLIENT = "fhir";
	public final static String REST_CLIENT = "rest";
	public final static String ADJUDICATION_ELIGIBLE = "eligible";
	public final static String ADJUDICATION_BENEFIT = "benefit";
	public final static String CLAIM_ID = "claimId";
	public final static String CLAIM_ITEMS = "item";
	public final static String ICD_10 = "ICD 10 - WHO";
	public final static String INSUREE_ID = "insureeId";
	public final static String PATIENT_UUID = "patientUUID";
	public final static String VISIT_UUID = "visitUUID";
	public final static String FHIR_CODE_FOR_IMIS_CLAIM_CODE_TYPE = "MR";
	public final static String FHIR_VALUESET_SYSTEM = "https://hl7.org/fhir/valueset-identifier-type.html";

}
