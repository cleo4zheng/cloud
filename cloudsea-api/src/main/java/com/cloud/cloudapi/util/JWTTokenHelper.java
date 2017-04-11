package com.cloud.cloudapi.util;

import java.security.Key;
import java.util.HashMap;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;

import com.cloud.cloudapi.pojo.common.CloudUser;
import com.cloud.cloudapi.pojo.common.TokenGui;
import com.cloud.cloudapi.pojo.common.Util;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class JWTTokenHelper {
	
	
	private static final String key="cblkey";

	public static String createEncryptToken(TokenGui token, CloudUser user, Boolean isAdmin) {
		// The JWT signature algorithm we will be using to sign the token
		SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
		// We will sign our JWT with our ApiKey secret
		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(key);
		Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

		HashMap<String, String> tokenMap = new HashMap<String, String>();
		tokenMap.put("tokenid", token.getTokenid());
		tokenMap.put("userAccount", user.getAccount());
		// tokenMap.put("isAdmin", "true");
		tokenMap.put("isAdmin", isAdmin.toString());
		tokenMap.put("currentRegion", token.getCurrentRegion());
		//tokenMap.put("domain", token.getDomainName());
		tokenMap.put("locale", token.getLocale());
		if (null == token.getEnableZabbix())
			tokenMap.put("enableZabbix", "false");
		else
			tokenMap.put("enableZabbix", Boolean.toString(token.getEnableZabbix()));
		if (null == token.getEnableWorkflow())
			tokenMap.put("enableWorkflow", "false");
		else
			tokenMap.put("enableWorkflow", Boolean.toString(token.getEnableWorkflow()));
		tokenMap.put("projectName", token.getTenantname());
		tokenMap.put("projectNames", Util.listToString(token.getProjectNames(), ','));
		tokenMap.put("exp", DateHelper.longToStrByFormat(token.getExpiresTime(), "yyyy-MM-dd'T'HH:mm:ssZ"));
		String subject = new JsonHelper<HashMap, String>().generateJsonBodySimple(tokenMap, "token");
		// Let's set the JWT Claims
		JwtBuilder builder = Jwts.builder().setSubject(subject).signWith(signatureAlgorithm, signingKey);

		// if it has been specified, let's add the expiration
		// Builds the JWT and serializes it to a compact, URL-safe string
		return builder.compact();
	}

	public static String getGuiTokenFromEncryptToken(String encryptToken) throws Exception{
		String tokeninfo=parseEncryptToken(encryptToken);
	    return tokeninfo;
	}
	
	public static String getGuiTokenIdFromEncryptToken(String encryptToken) throws Exception{
		String tokeninfo=parseEncryptToken(encryptToken);
	    ObjectMapper  objectMapper = new ObjectMapper();
		try {
			JsonNode node = objectMapper.readTree(tokeninfo);
			return node.path("token").path("tokenid").textValue();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}	
	}
	
	/**
	 * 从guitoken获取是否为admin用户
	 * @return
	 * @throws Exception 
	 */
	public static Boolean isAdminFromEncryptToken(String encryptToken) throws Exception{
		String tokeninfo=parseEncryptToken(encryptToken);
	    ObjectMapper  objectMapper = new ObjectMapper();
	    boolean isAdmin = false;
		try {
			JsonNode node = objectMapper.readTree(tokeninfo);
			isAdmin =  Boolean.valueOf(StringHelper.objectToString(node.path("token").path("isAdmin").textValue()));
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}	
		
		return isAdmin;
	}
	
	/**
	 * 从guitoken获取account
	 * @return
	 * @throws Exception 
	 */
	public static String getAccountFromEncryptToken(String encryptToken) throws Exception{
		String tokeninfo=parseEncryptToken(encryptToken);
	    ObjectMapper  objectMapper = new ObjectMapper();
	    String account = "";
		try {
			JsonNode node = objectMapper.readTree(tokeninfo);
			account = StringHelper.objectToString(node.path("token").path("userAccount").textValue());
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}	
		
		return account;
	}
	
	public static String parseEncryptToken(String jwt) {
		//This line will throw an exception if it is not a signed JWS (as expected)
		Claims claims = Jwts.parser()         
		   .setSigningKey(DatatypeConverter.parseBase64Binary(key))
		   .parseClaimsJws(jwt).getBody();
		System.out.println("Subject: " + claims.getSubject());	
		return  claims.getSubject();
	}	
	
	// Sample method to construct a JWT

//	private static  String createJWT(String id, String issuer, String subject, long ttlMillis) {
//
//		// The JWT signature algorithm we will be using to sign the token
//		SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
//
//		long nowMillis = System.currentTimeMillis();
//		Date now = new Date(nowMillis);
//
//		// We will sign our JWT with our ApiKey secret
//		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(key);
//		Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
//
//		// Let's set the JWT Claims
//		JwtBuilder builder = Jwts.builder().setId(id).setIssuedAt(now).setSubject(subject).setIssuer(issuer)
//				.signWith(signatureAlgorithm, signingKey);
//
//		// if it has been specified, let's add the expiration
//		if (ttlMillis >= 0) {
//			long expMillis = nowMillis + ttlMillis;
//			Date exp = new Date(expMillis);
//			builder.setExpiration(exp);
//		}
//
//		// Builds the JWT and serializes it to a compact, URL-safe string
//		return builder.compact();
//	}

//	private static  void parseJWT(String jwt) {
//		//This line will throw an exception if it is not a signed JWS (as expected)
//		Claims claims = Jwts.parser()         
//		   .setSigningKey(DatatypeConverter.parseBase64Binary(key))
//		   .parseClaimsJws(jwt).getBody();
//		System.out.println("ID: " + claims.getId());
//		System.out.println("Subject: " + claims.getSubject());
//		System.out.println("Issuer: " + claims.getIssuer());
//		System.out.println("Expiration: " + claims.getExpiration());
//	}
	
	
//	public static void main(String args[]){
//		
//		TokenGui guitoken= new TokenGui();
//		//guitoken.setTokenid(Util.makeUUID());
//		guitoken.setTokenid("0fdc2e7c-f20a-4355-a2c8-e0819c85091b");
//		guitoken.setExpiresTime(System.currentTimeMillis()+24*3600*1000); 
//		guitoken.setCurrentRegion("RegionOne");
//		CloudUser user = new CloudUser();
//		user.setAccount("cloud002");
//		
//		String testtoken = createEncryptToken(guitoken,user,false);
//		System.out.println(testtoken);
//		System.out.println(parseEncryptToken(testtoken));
//		
//		//String realtoken="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ7XCJ0b2tlblwiOntcInRva2VuaWRcIjpcImU0MDBmOWI1LWQwZGUtNGU3Mi05YWUyLWE0MGFkN2RiZjhhNFwiLFwidXNlckFjY291bnRcIjpcImNsb3VkMDAzXCIsXCJpc0FkbWluXCI6XCJ0cnVlXCIsXCJleHBcIjpcIjIwMTYtMDctMDlUMDA6NTM6NDYrMDgwMFwifX0ifQ.g2uBvZ6ca21JcXnog2r5uojIb0dqBXp80u7gWEZzx5A";
//		
//		long time_begin=System.currentTimeMillis();
//		try {
//			System.out.println(getGuiTokenIdFromEncryptToken(testtoken));
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		long time_end=System.currentTimeMillis();
//		
//		System.out.println(time_end-time_begin);
//		
//		String realtoken="eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ7XCJ0b2tlblwiOntcInRva2VuaWRcIjpcImJjNjk1ZmZiLWVlNjQtNDJkNy05ZDkxLWEzYmM0NmVmZjAzOVwiLFwiY3VycmVudFJlZ2lvblwiOlwiUmVnaW9uT25lXCIsXCJ1c2VyQWNjb3VudFwiOlwiY2xvdWQwMDNcIixcImlzQWRtaW5cIjpcInRydWVcIixcImV4cFwiOlwiMjAxNi0wNy0yMlQxNjo1MzowOSswODAwXCJ9fSJ9.VoLjFu30JRk3SEI3oay4q_TH3-clauBoBhoju4QhR5U";
//		System.out.println(parseEncryptToken(realtoken));
//		
//	}
}
