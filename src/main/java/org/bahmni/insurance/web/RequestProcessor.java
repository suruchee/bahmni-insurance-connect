package org.bahmni.insurance.web;

import static org.apache.log4j.Logger.getLogger;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.bahmni.insurance.AppProperties;
import org.bahmni.insurance.ImisConstants;
import org.bahmni.insurance.auth.AuthenticationFilter;
import org.bahmni.insurance.client.RestTemplateFactory;
import org.bahmni.insurance.dao.FhirResourceDaoServiceImpl;
import org.bahmni.insurance.dao.IFhirResourceDaoService;
import org.bahmni.insurance.model.ClaimLineItemRequest;
import org.bahmni.insurance.model.ClaimParam;
import org.bahmni.insurance.model.ClaimResponseModel;
import org.bahmni.insurance.model.EligibilityResponseModel;
import org.bahmni.insurance.model.FhirResourceModel;
import org.bahmni.insurance.model.VisitSummary;
import org.bahmni.insurance.service.AFhirConstructorService;
import org.bahmni.insurance.service.AInsuranceClientService;
import org.bahmni.insurance.service.FInsuranceServiceFactory;
import org.bahmni.insurance.service.IOpenmrsOdooService;
import org.bahmni.insurance.serviceImpl.BahmniOpenmrsApiClientServiceImpl;
import org.bahmni.insurance.serviceImpl.FhirConstructorServiceImpl;
import org.bahmni.insurance.serviceImpl.OpenmrsOdooServiceImpl;
import org.bahmni.insurance.utils.InsuranceUtils;
import org.hl7.fhir.dstu3.model.Claim;
import org.hl7.fhir.dstu3.model.EligibilityRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;

@RestController
public class RequestProcessor {

	private final Logger logger = getLogger(RequestProcessor.class);

	private final AFhirConstructorService fhirConstructorService;
	private final IOpenmrsOdooService odooService;
	private final IFhirResourceDaoService fhirDaoService;
	private final FInsuranceServiceFactory insuranceImplFactory;
	private final BahmniOpenmrsApiClientServiceImpl bahmniOpenmrsService;
	private final IParser FhirParser = FhirContext.forDstu3().newJsonParser();

	private final AppProperties properties;
	private final AuthenticationFilter authenticationFilter;

	@Autowired
	public RequestProcessor(FhirConstructorServiceImpl fhirConstructorServiceImpl,
			OpenmrsOdooServiceImpl openmrsOdooServiceImpl, FhirResourceDaoServiceImpl fhirServiceImpl,
			FInsuranceServiceFactory insuranceImplFactory, RestTemplateFactory restFactory,
			AuthenticationFilter authenticationFilter,
			AInsuranceClientService insuranceClientService,AppProperties props,
			BahmniOpenmrsApiClientServiceImpl bahmniOpenmrsService) {
		this.fhirConstructorService = fhirConstructorServiceImpl;
		this.odooService = openmrsOdooServiceImpl;
		this.fhirDaoService = fhirServiceImpl;
		this.insuranceImplFactory = insuranceImplFactory;
		this.properties = props;
		this.authenticationFilter = authenticationFilter;
		this.bahmniOpenmrsService = bahmniOpenmrsService;
	}

	@ExceptionHandler({ AccessDeniedException.class })
	public ResponseEntity<Object> handleAccessDeniedException(Exception ex, WebRequest request) {
		return new ResponseEntity<Object>("Access denied ", new HttpHeaders(), HttpStatus.FORBIDDEN);
	}
	
	@RequestMapping(path = "/hasInsurancePrivilege")
	public Boolean checkPrevilage(HttpServletRequest request, HttpServletResponse response) throws Exception {
    	return  authenticationFilter.preHandle(request, response);
    }
	
	@RequestMapping(method = RequestMethod.GET, value = "/check/eligibility/{chfID}", produces = "application/json")
	@ResponseBody
	public EligibilityResponseModel checkEligibility(HttpServletResponse response, @PathVariable("chfID") String chfID)
			throws RestClientException, URISyntaxException, DataFormatException, IOException {
		logger.debug("checkEligibility : ");
		
		
		EligibilityRequest eligRequest = fhirConstructorService.constructFhirEligibilityRequest(chfID);
		logger.error("eligibilityRequest : "+FhirParser.encodeResourceToString(eligRequest));
		String eligReqStr = FhirParser.encodeResourceToString(eligRequest);
		/*if(properties.saveEligResource) {
			fhirDaoService.insertFhirResource(eligReqStr, ImisConstants.FHIR_RESOURCE_TYPE.ELIGIBILITYREQUEST.getValue() );
		}*/
		fhirConstructorService.validateRequest(eligReqStr);
		logger.error("After validation");
		EligibilityResponseModel eligibilityResponseModel = insuranceImplFactory
				.getInsuranceServiceImpl(ImisConstants.OPENIMIS_FHIR, properties).checkEligibility(eligRequest);
		logger.error("After eligiblity reply" + eligibilityResponseModel );
		logger.error("eligibilityResponseModel : " + InsuranceUtils
				.mapToJson(eligibilityResponseModel));
		return eligibilityResponseModel;

	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/resubmit/fhir", produces = "application/json")
	@ResponseBody
	public ClaimResponseModel resubmitClaimFhir(HttpServletResponse response, @RequestBody String claimFhirEdited)
			throws RestClientException, URISyntaxException, DataFormatException, IOException {
		//logger.error("submitClaim : "+InsuranceUtils.mapToJson(claimParams));

		logger.error("claimRequest : "+claimFhirEdited);
		// String claimReqStr = FhirParser.encodeResourceToString(claimRequest);
		if(properties.saveClaimResource) {
			fhirDaoService.insertFhirResource(claimFhirEdited, ImisConstants.FHIR_RESOURCE_TYPE.CLAIM.getValue() );
		}
		fhirConstructorService.validateRequest(claimFhirEdited);
		Claim claimRequest =  (Claim) FhirParser.parseResource(claimFhirEdited);
		ClaimResponseModel claimResponseModel = insuranceImplFactory
				.getInsuranceServiceImpl(ImisConstants.OPENIMIS_FHIR, properties).submitClaim(claimRequest);
		logger.error("claimResponseModel : " + InsuranceUtils.mapToJson(claimResponseModel));
		return claimResponseModel;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/submit/claim", produces = "application/json")
	@ResponseBody
	public ClaimResponseModel submitClaim(HttpServletResponse response, @RequestBody ClaimParam claimParams)
			throws RestClientException, URISyntaxException, DataFormatException, IOException {
		logger.error("submitClaim : "+InsuranceUtils.mapToJson(claimParams));

		//claimParams.setClaimId("980"); // TODO: remove hard coded
		
		Claim claimRequest = fhirConstructorService.constructFhirClaimRequest(claimParams);
		logger.error("claimRequest : "+FhirParser.encodeResourceToString(claimRequest));
		String claimReqStr = FhirParser.encodeResourceToString(claimRequest);
		if(properties.saveClaimResource) {
			fhirDaoService.insertFhirResource(claimReqStr, ImisConstants.FHIR_RESOURCE_TYPE.CLAIM.getValue() );
		}
		fhirConstructorService.validateRequest(claimReqStr);

		ClaimResponseModel claimResponseModel = insuranceImplFactory
				.getInsuranceServiceImpl(ImisConstants.OPENIMIS_FHIR, properties).submitClaim(claimRequest);
		logger.error("claimResponseModel : " + InsuranceUtils.mapToJson(claimResponseModel));
		return claimResponseModel;
		/*ClaimResponseModel claimResponseModel = insuranceImplFactory
				.getInsuranceServiceImpl(ImisConstants.OPENIMIS_FHIR, properties).getClaimResponse("L21");
		System.out.println("claimResponseModel : " + InsuranceUtils.mapToJson(claimResponseModel));
		return claimResponseModel;*/
		

	}

	@RequestMapping(method = RequestMethod.GET, value = "get/claimresponse/{claimId}", produces = "application/json")
	@ResponseBody
	public ClaimResponseModel getClaimResponse(HttpServletResponse response,@PathVariable("claimId") String claimId)
			throws IOException {
		//claimId = "980"; //TODO: remove hardcoded
		ClaimResponseModel claimResponseModel = insuranceImplFactory
				.getInsuranceServiceImpl(ImisConstants.OPENIMIS_FHIR, properties).getClaimResponse(claimId);
		System.out.println("claimResponseModel : " + InsuranceUtils.mapToJson(claimResponseModel));

		return claimResponseModel;

	}
	
	@RequestMapping(path = "/openIMIS/login")
	@ResponseBody
	public String checkLogin(HttpServletResponse response) throws RestClientException, URISyntaxException {
		logger.debug("checkLogin");
		return insuranceImplFactory.getInsuranceServiceImpl(ImisConstants.OPENIMIS_FHIR, properties).loginCheck();
	}

	@RequestMapping(path = "/get/fhir/claims")
	@ResponseBody
	public List<FhirResourceModel> getFhirClaim() {
		return fhirDaoService.findAll();
	}

/*	@RequestMapping(path = "/add/fhir/claim")
	@ResponseBody
	public int addFhirClaim() {
		return fhirDaoService.insertFhirResource();
	}*/
	
	@RequestMapping(path = "/get/claimRequest/{claimId}")
	@ResponseBody
	public String getFhirClaim(@PathVariable("claimId") String claimId) {
		//claimId = "980"; //TODO: remove hard coded
		return fhirDaoService.getClaimRequestByClaimId(claimId);
	}

	@RequestMapping(path = "/get/fhir/claim/id")
	@ResponseBody
	public List<String> getFhirClaimId() {
		return fhirDaoService.getClaimId();

	}

	@RequestMapping(method = RequestMethod.GET, value = "/patient/{name}", produces = "application/json")
	@ResponseBody
	public String retrievePatientByName(HttpServletResponse response, @PathVariable("name") String name) {
		logger.debug("retreivePatient : ");
		return fhirConstructorService.getFhirPatient(name);

	}

	@RequestMapping(method = RequestMethod.POST, value = "/patient", produces = "application/json")
	@ResponseBody
	public ResponseEntity<String> generatePatient(HttpServletResponse response, @RequestBody String personJson) {
		logger.debug("generatePatient : ");
		return fhirConstructorService.createFhirPatient(personJson);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/visit/{visitUUID}", produces = "application/json")
	@ResponseBody
	public VisitSummary getVisitDetails(HttpServletResponse response, @PathVariable("visitUUID") String visitUUID) throws JsonParseException, JsonMappingException, IOException {
		logger.debug("getVisitDetails : ");
		return bahmniOpenmrsService.getVisitDetail(visitUUID);

	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/request/authenticate", produces = "application/json")
	@ResponseBody
	public void authenticateOpenIMIS(HttpServletResponse response) throws JsonParseException, JsonMappingException, IOException {
		logger.debug("Authenticated : ");

	}

	/*
	 * @RequestMapping(path = "/odercost") public void
	 * getOrderCost(HttpServletResponse response) { logger.debug("getOrderCost");
	 * System.out.println(odooService.getOrderCost(
	 * "9065024b-9499-4c9b-9a2f-a53f703be2aa"));
	 * 
	 * }
	 */

}
