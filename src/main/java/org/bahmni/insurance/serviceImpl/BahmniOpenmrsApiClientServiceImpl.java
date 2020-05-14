package org.bahmni.insurance.serviceImpl;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.bahmni.insurance.exception.ApiException;
import org.bahmni.insurance.model.BahmniDiagnosis;
import org.bahmni.insurance.model.ClaimLineItemResponse;
import org.bahmni.insurance.model.Diagnosis;
import org.bahmni.insurance.model.VisitSummary;
import org.bahmni.insurance.service.IApiClientService;
import org.bahmni.insurance.utils.InsuranceUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

@Component	
public class BahmniOpenmrsApiClientServiceImpl implements IApiClientService {

	@Value("${openmrs.root.url}")
	private String openmrsAPIUrl;

	@Value("${openmrs.user}")
	private String openmrsUser;

	@Value("${openmrs.password}")
	private String openmrsPassword;

	final static RestTemplate restTemplate = new RestTemplate();

	private HttpHeaders httpHeaders = new HttpHeaders();

	@Override
	public HttpHeaders getAuthHeaders() {
		if (httpHeaders.containsKey("Authorization")) {
			return httpHeaders;
		} else {
			httpHeaders = new HttpHeaders() {
				private static final long serialVersionUID = 1L;
				{
					String auth = openmrsUser + ":" + openmrsPassword;
					byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
					String authHeader = "Basic " + new String(encodedAuth);
					set("Authorization", authHeader);
				}
			};
		}
		return httpHeaders;

	}

	@Override
	public ResponseEntity<String> sendPostRequest(String requestJson, String url) {
		HttpHeaders headers = getAuthHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.add("Content-Type", "application/json");
		HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);
		return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
	}

	@Override
	public String sendGetRequest(String url) {
		HttpHeaders headers = getAuthHeaders();
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		return restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
	}
	
	public VisitSummary getVisitDetail(String visitUUID) throws JsonParseException, JsonMappingException, IOException {
		String visitDetailsJson =  sendGetRequest(openmrsAPIUrl+"/bahmnicore/visit/summary?visitUuid="+visitUUID);
		VisitSummary visit = null;
		if(visitDetailsJson != null){
			visit = InsuranceUtils.mapFromJson(visitDetailsJson, VisitSummary.class);
		} else {
			throw new ApiException(" VisitSummary is null ");
		}
		return visit;
	}
	

	public BahmniDiagnosis getDiagnosis(String patientUUID, String visitUUID) throws JsonParseException, JsonMappingException, IOException {
		String diagnosisJson =  sendGetRequest(openmrsAPIUrl+"/bahmnicore/diagnosis/search?patientUuid="+patientUUID+"&visitUuid="+visitUUID);
		BahmniDiagnosis bahmniDiagnosisList = null;
		if(diagnosisJson != null && diagnosisJson.length() > 2){ //diagnosisJson contains atleast 2 chars []
			diagnosisJson = "{\"diagnosis\" : "+diagnosisJson+ "}";
		}
		/*
		 * else { String fromDateStr = InsuranceUtils.convertBahmniDateStr(fromDate);
		 * diagnosisJson =
		 * sendGetRequest(openmrsAPIUrl+"/bahmnicore/diagnosis/search?patientUuid="+
		 * patientUUID+"&fromDate="+fromDateStr); diagnosisJson =
		 * "{\"diagnosis\" : "+diagnosisJson+ "}"; }
		 */
		bahmniDiagnosisList = InsuranceUtils.mapFromJson(diagnosisJson, BahmniDiagnosis.class);
		return bahmniDiagnosisList;
	}
	
	

}
