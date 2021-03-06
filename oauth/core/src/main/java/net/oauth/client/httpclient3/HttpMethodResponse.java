/*
 * Copyright 2007 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.oauth.client.httpclient3;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.oauth.OAuth;
import net.oauth.OAuthProblemException;
import net.oauth.client.OAuthResponseMessage;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;

/**
 * The response part of an HttpMethod, encapsulated as an OAuthMessage.
 * 
 * @author John Kristian
 */
public class HttpMethodResponse extends OAuthResponseMessage {

    /**
     * Construct an OAuthMessage from the HTTP response, including parameters
     * from OAuth WWW-Authenticate headers and the body. The header parameters
     * come first, followed by the ones from the response body.
     */
    public HttpMethodResponse(HttpMethod method, byte[] requestBody,
            String requestEncoding) throws IOException {
        super(method.getName(), method.getURI().toString());
        this.method = method;
        this.requestBody = requestBody;
        this.requestEncoding = requestEncoding;
        for (Header header : method.getResponseHeaders("WWW-Authenticate")) {
            decodeWWWAuthenticate(header.getValue());
        }
        Header[] headers = method.getResponseHeaders("Content-Type");
        contentType = (headers == null || headers.length <= 0) ? null
                : headers[headers.length - 1].getValue();
    }

    private final HttpMethod method;
    private final byte[] requestBody;
    private final String requestEncoding;
    private String bodyAsString = null;
    private final String contentType;

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public InputStream getBodyAsStream() throws IOException {
        if (bodyAsString == null) {
            return method.getResponseBodyAsStream();
        }
        return super.getBodyAsStream();
    }

    @Override
    public String getBodyAsString() throws IOException {
        if (bodyAsString == null) {
            bodyAsString = method.getResponseBodyAsString();
        }
        return bodyAsString;
    }

    @Override
    protected void completeParameters() throws IOException {
        Header contentType = method.getResponseHeader("Content-Type");
        if (contentType == null || isDecodable(contentType.getValue())) {
            super.completeParameters();
        }
    }

    /** Return a complete description of the HTTP exchange. */
    @Override
    protected void dump(Map<String, Object> into) throws IOException {
        super.dump(into);
        {
            StringBuilder request = new StringBuilder(method.getName());
            request.append(" ").append(method.getPath());
            String query = method.getQueryString();
            if (query != null && query.length() > 0) {
                request.append("?").append(query);
            }
            request.append(EOL);
            for (Header header : method.getRequestHeaders()) {
                request.append(header.getName()).append(": ").append(
                        header.getValue()).append(EOL);
            }
            into.put(HTTP_REQUEST_HEADERS, request.toString());
            request.append(EOL);
            if (requestBody != null) {
                request.append(new String(requestBody, requestEncoding));
            }
            into.put(HTTP_REQUEST, request.toString());
        }
        into.put(OAuthProblemException.HTTP_STATUS_CODE, //
                new Integer(method.getStatusCode()));
        {
            List<OAuth.Parameter> responseHeaders = new ArrayList<OAuth.Parameter>();
            StringBuilder response = new StringBuilder();
            String value = method.getStatusLine().toString();
            response.append(value).append(EOL);
            responseHeaders.add(new OAuth.Parameter(null, value));
            for (Header header : method.getResponseHeaders()) {
                String name = header.getName();
                value = header.getValue();
                response.append(name).append(": ").append(value).append(EOL);
                responseHeaders.add(new OAuth.Parameter(name.toLowerCase(),
                        value));
            }
            into.put(OAuthProblemException.RESPONSE_HEADERS, responseHeaders);
            response.append(EOL);
            String body = getBodyAsString();
            if (body != null) {
                response.append(body);
            }
            into.put(HTTP_RESPONSE, response.toString());
        }
    }
}
