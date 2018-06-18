package com.tcs.telecom.tib.filter;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.tcs.telecom.tib.vo.ResultVo;

@Component
public class AuthenticationFilter extends ZuulFilter {

	private static Logger log = LoggerFactory.getLogger(AuthenticationFilter.class);

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private Environment environment;

	@Value("${security.service.url}")
	String securityServiceURL;

	@Override
	public Object run() {

		// Implementation filters like authentication/authorization filters here
		RequestContext ctx = RequestContext.getCurrentContext();
		HttpServletRequest request = ctx.getRequest();

		try {
			String sessionToken = request.getHeader(environment.getProperty("header.sessionToken"));
			String sessionId = request.getHeader(environment.getProperty("header.sessionId"));

			log.debug("Session Token " + sessionToken);
			log.debug("Session ID " + sessionId);

			if (sessionToken != null && sessionId != null) {
				Map<String, String> params = new HashMap<>();
				params.put(environment.getProperty("security.service.key.sessionToken"), sessionToken);
				params.put(environment.getProperty("security.service.key.sessionId"), sessionId);

				ResultVo result = restTemplate.getForObject(securityServiceURL, ResultVo.class, params);
				if (result.getErrorCode() != null) {
					log.error("Send Error code back to client as unauthroized. Error - " + result.getErrorCode() + " " + result.getErrorMsg());
					ctx.setSendZuulResponse(false);
					ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
					ctx.setResponseBody(HttpStatus.UNAUTHORIZED.getReasonPhrase());
					//ctx.setResponseBody(result.getErrorMsg());
				}
				ctx.addZuulRequestHeader(environment.getProperty("header.opId"), result.getOpId());
				ctx.addZuulRequestHeader(environment.getProperty("header.buId"), result.getBuId());
				ctx.addZuulRequestHeader(environment.getProperty("header.lang"), result.getLang());
				ctx.addZuulRequestHeader(environment.getProperty("header.userId"), result.getUserId());
				ctx.addZuulRequestHeader(environment.getProperty("header.domainId"), result.getDomainId());
				if (result.getGroups() != null && !result.getGroups().isEmpty()) {
					String groupId = StringUtils.join(result.getGroups(), ',');
					ctx.addZuulRequestHeader(environment.getProperty("header.groupId"), groupId);
				}
				populateAttributes(ctx, result);
			} else {
				// ctx.setSendZuulResponse(false);
				// ctx.setResponseStatusCode(401);
				return null;
			}
		} catch (Exception e) {
			log.error("Error in HobsRestGateway - " + e.getMessage());
			ctx.setSendZuulResponse(false);
			ctx.setResponseStatusCode(HttpStatus.UNAUTHORIZED.value());
			ctx.setResponseBody(HttpStatus.UNAUTHORIZED.getReasonPhrase());
			if (log.isDebugEnabled()) {
				log.error("Stacktrace for error in HobsRestGateway - ", e);
			}
		}
		return null;
	}

	private void populateAttributes(RequestContext ctx, ResultVo result) {
		Map<String, String> params = new HashMap<>();
		String response = null;
		params.put(environment.getProperty("header.opId"), result.getOpId());
		params.put(environment.getProperty("header.buId"), result.getBuId());
		params.put(environment.getProperty("header.userId"), result.getUserId());
		String url = environment.getProperty("sso.userGroupAttributes.url");
		
		try {
			response = restTemplate.getForObject(url, String.class, params);
		} catch (RestClientException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ctx.addZuulRequestHeader(environment.getProperty("header.attributes"), response);
	}

	@Override
	public boolean shouldFilter() {
		// Auto-generated method stub
		return true;
	}

	@Override
	public int filterOrder() {
		// Auto-generated method stub
		return 1;
	}

	@Override
	public String filterType() {
		return "pre";
	}

	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}

}
