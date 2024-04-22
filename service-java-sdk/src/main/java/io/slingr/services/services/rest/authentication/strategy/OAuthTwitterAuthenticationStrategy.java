package io.slingr.services.services.rest.authentication.strategy;

import io.slingr.services.services.rest.HttpRequest;
import io.slingr.services.utils.Json;
import io.slingr.services.utils.Strings;
import org.apache.commons.codec.binary.Base64;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class OAuthTwitterAuthenticationStrategy implements AuthenticationStrategy {

    private final String oauth_token;
    private final String oauth_token_secret;
    private final String apiKey;
    private final String apiSecret;


    public  OAuthTwitterAuthenticationStrategy(Map<String, String> params) {
        this.oauth_token = params.get("oauth_token");
        this.oauth_token_secret = params.get("oauth_token_secret");
        this.apiKey = params.get("apiKey");
        this.apiSecret = params.get("apiSecret");
    }

    @Override
    public void addAuthentication(Client client, WebTarget apiTarget, HttpRequest request) {

        client.register((ClientRequestFilter) requestContext -> {

            Map<String, String> otherHeaders = new HashMap<>();
            otherHeaders.put("oauth_token", encode(oauth_token));

            requestContext.getHeaders().add("Authorization", getOauthHeader(
                    requestContext.getMethod(),
                    getTargetUrl(requestContext),
                    otherHeaders,
                    apiKey,
                    apiSecret.concat("&").concat(encode(oauth_token_secret)),
                    true));
        });
    }


    private String getOauthHeader(String method, String url, Map<String, String> headers, String apiKey, String secretKey, boolean includeParams) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put("oauth_consumer_key", apiKey);
        headers.put("oauth_signature_method", "HMAC-SHA1");
        headers.put("oauth_nonce", Strings.randomUUIDString().replaceAll("-", ""));
        headers.put("oauth_timestamp", Long.toString(System.currentTimeMillis() / 1000L));
        headers.put("oauth_version", "1.0");
        Map<String, String> otherParams = new HashMap<>();
        String cleanUrl = url;
        if (url.contains("?")) {
            int i = url.indexOf("?");
            cleanUrl = url.substring(0, i);
            if (includeParams) {
                String[] params = url.substring(i + 1).split("&");
                for (String param : params) {
                    String[] p = param.split("=");
                    if (p.length == 2) {
                        otherParams.put(p[0], p[1]);
                    }
                }
            }
        }
        Map<String, String> complete = new HashMap<>(headers);
        if (!otherParams.isEmpty()) {
            complete.putAll(otherParams);
        }
        List<String> keys = new ArrayList<>(complete.keySet());
        Collections.sort(keys);
        String parameterString = "";
        String prefix = "";
        for (String key : keys) {
            parameterString = parameterString.concat(prefix).concat(key).concat("=").concat(complete.get(key));
            prefix = "&";
        }
        String baseString = method + "&" + encode(cleanUrl) + "&" + encode(parameterString);
        String signature = computeSignature(baseString, secretKey);
        String authHeader = "OAuth ";
        prefix = "";
        for (String key : headers.keySet()) {
            authHeader = authHeader.concat(prefix).concat(key).concat("=\"").concat(headers.get(key)).concat("\"");
            prefix = ",";
        }
        authHeader += ",oauth_signature=\"" + encode(signature) + "\"";
        return authHeader;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("*", "%2A")
                .replace("+", "%20")
                .replace("%7E", "~");
    }

    private static String computeSignature(String baseString, String keyString) {

        SecretKey secretKey;

        byte[] keyBytes = keyString.getBytes();
        secretKey = new SecretKeySpec(keyBytes, "HmacSHA1");

        Mac mac = null;
        try {
            mac = Mac.getInstance("HmacSHA1");
            mac.init(secretKey);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            // do nothing
        }

        if (mac != null) {
            byte[] text = baseString.getBytes();

            return new String(Base64.encodeBase64(mac.doFinal(text))).trim();
        }

        return null;

    }

    // Internal methods

    private String getTargetUrl(ClientRequestContext requestContext) {
        StringBuilder url = new StringBuilder(requestContext.getUri().toString());
        Object entity = requestContext.getEntity();

        if (entity instanceof Json) {
            Json json = (Json) entity;
            if (!json.isEmpty("url")) {
                url.append(json.string("url"));
            }
            if (!json.isEmpty("params")) {
                Map<String, Object> params = json.json("params").toMap();
                String prefix = url.toString().contains("?") ? "&" : "?";
                for (String key : params.keySet()) {
                    url.append(prefix).append(key).append("=").append(encode(params.get(key).toString()));
                    prefix = "&";
                }
            }
        }
        return url.toString();
    }


}