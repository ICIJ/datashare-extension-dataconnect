package org.icij.datashare;

import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.errors.UnauthorizedException;
import net.codestory.http.payload.Payload;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.icij.datashare.user.User;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@Prefix("/api/proxy")
public class DiscourseResource {

    private final URL discourseUrl;
    private final HttpClient httpClient;
    private final String discourseApiKey;

    @Inject
    public DiscourseResource(PropertiesProvider propertiesProvider) throws MalformedURLException {
       discourseUrl = new URL(propertiesProvider.get("discourseUrl").orElse("http://discourse:3000"));
       discourseApiKey = propertiesProvider.get("discourseApiKey").orElseThrow(() -> new RuntimeException("no discourse api key found in settings"));
       httpClient = HttpClientBuilder.create().build();
    }

    @Get("/:project/:url:")
    public Payload getMethod(String project, String url, Context context) throws IOException {
        checkProject(project,context);
        try {
            HttpGet httpUriRequest = new HttpGet(discourseUrl.toString() + "/" + url);
            prepareRequest(httpUriRequest,context.currentUser().login());
            HttpResponse response = httpClient.execute(httpUriRequest);
            return new Payload(response.getFirstHeader("Content-Type").toString(), response.getEntity().getContent(), response.getStatusLine().getStatusCode());
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Put("/:project/:url:")
    public Payload putMethod(String project, String url, Context context) throws IOException {
        checkProject(project,context);
        return getPayload(context, new HttpPut(discourseUrl.toString() + "/" + url));
    }

    @Post("/:project/:url:")
    public Payload postMethod(String project, String url, Context context) throws IOException {
        checkProject(project,context);
        return getPayload(context, new HttpPost(discourseUrl.toString() + "/" + url));
    }

    private Payload getPayload(Context context, HttpEntityEnclosingRequestBase httpUriRequest) throws IOException {
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContentType(new BasicHeader("Content-Type", context.header("Content-Type")));
        byte[] contentAsBytes = context.request().contentAsBytes();
        entity.setContent(new ByteArrayInputStream(contentAsBytes));
        entity.setContentLength(contentAsBytes.length);
        httpUriRequest.setEntity(entity);
        prepareRequest(httpUriRequest, context.currentUser().login());
        HttpResponse response = httpClient.execute(httpUriRequest);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(response.getEntity().getContent(), output);
        String payload = output.toString();
        return new Payload(response.getFirstHeader("Content-Type").toString(), payload, response.getStatusLine().getStatusCode());
    }

    private void prepareRequest(HttpUriRequest httpUriRequest, String login) {
        httpUriRequest.addHeader("Api-Key", discourseApiKey);
        httpUriRequest.addHeader("Api-Username", login);
        httpUriRequest.addHeader("Content-Type", "application/json");
    }

    private void checkProject(String project, Context context) {
        if (!((User)context.currentUser()).isGranted(project)) {
            throw new UnauthorizedException();
        }
    }
}


