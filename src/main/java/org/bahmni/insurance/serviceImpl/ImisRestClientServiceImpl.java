package org.bahmni.insurance.serviceImpl;

import static org.apache.log4j.Logger.getLogger;

import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


import org.apache.commons.codec.binary.Base64;
import org.bahmni.insurance.AppProperties;
import org.bahmni.insurance.ImisConstants;
import org.bahmni.insurance.client.RestTemplateFactory;
import org.bahmni.insurance.model.ClaimLineItemResponse;
import org.bahmni.insurance.model.ClaimResponseModel;
import org.bahmni.insurance.model.EligibilityBalance;
import org.bahmni.insurance.model.EligibilityResponseModel;
import org.bahmni.insurance.service.AInsuranceClientService;
import org.bahmni.insurance.utils.InsuranceUtils;
import org.hl7.fhir.dstu3.model.Claim;
import org.hl7.fhir.dstu3.model.ClaimResponse;
import org.hl7.fhir.dstu3.model.ClaimResponse.AdjudicationComponent;
import org.hl7.fhir.dstu3.model.ClaimResponse.ItemComponent;
import org.hl7.fhir.dstu3.model.CodeableConcept;
import org.hl7.fhir.dstu3.model.Coding;
import org.hl7.fhir.dstu3.model.EligibilityRequest;
import org.hl7.fhir.dstu3.model.EligibilityResponse;
import org.hl7.fhir.dstu3.model.EligibilityResponse.BenefitComponent;
import org.hl7.fhir.dstu3.model.EligibilityResponse.BenefitsComponent;
import org.hl7.fhir.dstu3.model.EligibilityResponse.InsuranceComponent;
import org.hl7.fhir.dstu3.model.Money;
import org.hl7.fhir.dstu3.model.Task;
import org.hl7.fhir.exceptions.FHIRException;/*
												import org.openmrs.module.fhir.api.client.ClientHttpEntity;
												import org.openmrs.module.fhir.api.helper.ClientHelper;*/
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@Component
public class ImisRestClientServiceImpl extends AInsuranceClientService {
	private final RestTemplate restTemplate = new RestTemplate();
	private final IParser FhirParser = FhirContext.forDstu3().newJsonParser();
	private final org.apache.log4j.Logger logger = getLogger(ImisRestClientServiceImpl.class);
	
	
	private AppProperties properties;

	public ImisRestClientServiceImpl(AppProperties prop) {
		properties = prop;
		// restTemplate = getRestClient();
	}

	public RestTemplate getRestClient() {
		RestTemplateFactory restFactory = new RestTemplateFactory(properties);
		return restFactory.getRestTemplate(ImisConstants.OPENIMIS_FHIR);
	}

	private HttpHeaders createHeaders(String username, String password) {
		return new HttpHeaders() {
			private static final long serialVersionUID = 1L;
			{
				String auth = username + ":" + password;
				byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(Charset.forName("US-ASCII")));
				String authHeader = "Basic " + new String(encodedAuth);
				set("Authorization", authHeader);
			}
		};
	}

	private ResponseEntity<String> sendPostRequest(String requestJson, String url) {

		HttpHeaders headers = createHeaders(properties.imisUser, properties.imisPassword);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.add("Content-Type", "application/json");
		HttpEntity<String> entity = new HttpEntity<String>(requestJson, headers);
		return restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

		/*
		 * ClientHelper helper = getClientHelper(ImisConstants.REST_CLIENT);
		 * prepareRestTemplate(helper); ClientHttpEntity<?> request =
		 * helper.createRequest(properties.imisUrl + url, object); return
		 * exchange(helper, request, String.class);
		 */
	}

	private String sendGetRequest(String url) {
		HttpHeaders headers = createHeaders(properties.imisUser, properties.imisPassword);
		headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
		headers.add("Content-Type", "application/json");
		HttpEntity<String> entity = new HttpEntity<String>(headers);
		return restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();

	}

	/*
	 * private ResponseEntity<String> exchange(ClientHelper helper,
	 * ClientHttpEntity<?> request, Class<String> clazz) {
	 * 
	 * HttpHeaders headers = new HttpHeaders(); HttpEntity<?> entity = new
	 * HttpEntity<Object>(request.getBody(), headers); return
	 * restTemplate.exchange(request.getUrl(), request.getMethod(), entity, clazz);
	 * }
	 */
	/*
	 * private HttpHeaders setRequestHeaders(ClientHelper clientHelper, HttpHeaders
	 * headers) { for (ClientHttpRequestInterceptor interceptor :
	 * clientHelper.getCustomInterceptors(properties.imisUser,
	 * properties.imisPassword)) { interceptor.addToHeaders(headers); } return
	 * headers; }
	 */

	/*
	 * private void prepareRestTemplate(ClientHelper clientHelper) {
	 * List<HttpMessageConverter<?>> converters = new
	 * ArrayList<>(clientHelper.getCustomMessageConverter()); converters.add(new
	 * RequestWrapperConverter()); restTemplate.setMessageConverters(converters); }
	 */

	@Override
	public ClaimResponseModel submitClaim(Claim claimRequest) {
		String jsonClaimRequest = FhirParser.encodeResourceToString(claimRequest);
		ResponseEntity<String> responseObject = sendPostRequest(jsonClaimRequest, properties.openImisFhirApiClaim);
		ClaimResponse claimResponse = (ClaimResponse) FhirParser.parseResource(responseObject.getBody());
		System.out.println(FhirParser.encodeResourceToString(claimResponse));
		BigDecimal totalclaimedAmount = claimRequest.getTotal().getValue();
		return populateClaimRespModel(claimResponse, totalclaimedAmount);
	}
	private ClaimResponseModel populateClaimRespModel(ClaimResponse claimResponse, BigDecimal totalclaimedAmount) {
		ClaimResponseModel clmRespModel = new ClaimResponseModel();
		
		clmRespModel.setClaimStatus(claimResponse.getOutcome().getText());
		clmRespModel.setClaimId(claimResponse.getId());
		
		if(ImisConstants.CLAIM_OUTCOME.REJECTED.getOutCome().equals(claimResponse.getOutcome().getText())) {
			clmRespModel.setApprovedTotal(claimResponse.getTotalBenefit().getValue());
			clmRespModel.setDateProcessed(claimResponse.getPayment().getDate());
		}

		List<ClaimLineItemResponse> claimLineItems = new ArrayList<>();
		for (ItemComponent responseItem : claimResponse.getItem()) {
			ClaimLineItemResponse claimItem = new ClaimLineItemResponse();
			claimItem.setSequence(responseItem.getSequenceLinkIdElement().getValue());

			for (AdjudicationComponent adj : responseItem.getAdjudication()) {
				if(ImisConstants.CLAIM_ADJ_CATEGORY.GENERAL.equals(adj.getCategory().getText())){
					claimItem.setStatus(adj.getReason().getText());
					claimItem.setQuantityApproved(adj.getValue());
					if (ImisConstants.CLAIM_ITEM_STATUS.PASSED.equalsIgnoreCase(adj.getReason().getText())) {
						claimItem.setTotalApproved(adj.getAmount().getValue());
					}
				}
				if(ImisConstants.CLAIM_ADJ_CATEGORY.REJECTED_REASON.equals(adj.getCategory().getText())){
					claimItem.setRejectedReason(ImisConstants.ERROR_CODE_TO_TEXT_MAP.get(Integer.parseInt(adj.getReason().getCoding().get(0).getCode())));
				}
			}
			claimItem.setSequence(responseItem.getSequenceLinkId());
			claimLineItems.add(claimItem);
			
		}
		
		if (claimResponse.getTotalBenefit().getValue() != null) {
			clmRespModel.setApprovedTotal(claimResponse.getTotalBenefit().getValue());
		} else if (totalclaimedAmount != null) {
			clmRespModel.setApprovedTotal(totalclaimedAmount);
		}
		
		clmRespModel.setClaimLineItems(claimLineItems);
		return clmRespModel;
	}

	@Override
	public EligibilityResponseModel checkEligibility(EligibilityRequest eligbilityRequest){
		String jsonEligRequest = FhirParser.encodeResourceToString(eligbilityRequest);
		EligibilityResponseModel ResponseELigible;
		if(properties.openImisPolicyEnabled) {
			System.out.println("I am from specific policy config");
		ResponseEntity<String> responseObject = sendPostRequest(jsonEligRequest, properties.openImisFhirApiEligPolicyEnabled);
		EligibilityResponse eligibilityResponse = (EligibilityResponse) FhirParser.parseResource(responseObject.getBody());
		ResponseELigible = populateEligibilityRespModelPolicyEnabled(eligibilityResponse);
		}else {
			System.out.println("I am from global config");
			ResponseEntity<String> responseObject = sendPostRequest(jsonEligRequest, properties.openImisFhirApiElig);
			EligibilityResponse eligibilityResponse = (EligibilityResponse) FhirParser.parseResource(responseObject.getBody());
			ResponseELigible = populateEligibilityRespModel(eligibilityResponse);	
			
		}
		
		return ResponseELigible;
	}
	
	private EligibilityResponseModel populateEligibilityRespModelPolicyEnabled(EligibilityResponse eligibilityResponse) {
		EligibilityResponseModel eligRespModel = new EligibilityResponseModel();
		eligRespModel.setPolicy(properties.openImisPolicyEnabled);

		List<EligibilityBalance> eligibilityBalance = new ArrayList<>();	
		EligibilityBalance eligBalance = new EligibilityBalance();

		for (InsuranceComponent insurance : eligibilityResponse.getInsurance()) {
			if(insurance.getContract().getReference() != null) {
				String last = (insurance.getContract().getReference()).substring((insurance.getContract().getReference()).lastIndexOf('/') + 1);
				eligBalance.setValidDate(last);

				}
			for (BenefitsComponent benefitBalance : insurance.getBenefitBalance()){				
						
				eligBalance.setCategory(benefitBalance.getCategory().getText());
					for(BenefitComponent financial : benefitBalance.getFinancial()) {
						if (financial.getAllowed() instanceof Money) {
							eligBalance.setBenefitBalance(financial.getAllowedMoney().getValue().subtract(financial.getUsedMoney().getValue()));
									eligibilityBalance.add(eligBalance);

						}

					}

			}	

			eligRespModel.setEligibilityBalance(eligibilityBalance);
		}
		return eligRespModel;
	}
	private EligibilityResponseModel populateEligibilityRespModel(EligibilityResponse eligibilityResponse) {
		EligibilityResponseModel eligRespModel = new EligibilityResponseModel();
		/*eligRespModel.setNhisId(eligibilityResponse.getId());
		eligRespModel.setPatientId(eligibilityResponse.getId());*/	
		List<EligibilityBalance> eligibilityBalance = new ArrayList<>();
		for (InsuranceComponent insurance : eligibilityResponse.getInsurance()) {	
			for (BenefitsComponent benefitBalance : insurance.getBenefitBalance()){
				EligibilityBalance eligBalance = new EligibilityBalance();
				
				
				for(Coding code : benefitBalance.getCategory().getCoding()) {
					eligBalance.setCategory(code.getSystem());
				}
				
				
				for(BenefitComponent financial : benefitBalance.getFinancial()) {
						if (financial.getAllowed() instanceof Money) {
							eligBalance.setBenefitBalance(financial.getAllowedMoney().getValue());
							eligibilityBalance.add(eligBalance);
						}
					}
			}
			eligRespModel.setEligibilityBalance(eligibilityBalance);
		}
		return eligRespModel;
	}
	

	@Override
	public ClaimResponse getClaimStatus(Task claimStatusRequest) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String loginCheck() {
		return sendGetRequest(properties.imisUrl);
	}

	@Override
	public ClaimResponseModel getClaimResponse(String claimID) {
		String claimResponseStr = sendGetRequest(properties.imisUrl+"/ClaimResponse/"+claimID);
		ClaimResponse claimResponse = (ClaimResponse) FhirParser.parseResource(claimResponseStr);
		return populateClaimRespModel(claimResponse, null); 
		
	}
}
