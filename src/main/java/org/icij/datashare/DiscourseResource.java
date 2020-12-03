package org.icij.datashare;

import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@Prefix("/api/proxy")
public class DiscourseResource {

    private final URL discourseUrl;
    private final HttpClient httpClient;

    public DiscourseResource(PropertiesProvider propertiesProvider) throws MalformedURLException {
       discourseUrl = new URL(propertiesProvider.get("discourseUrl").orElse("http://discourse:3000"));
       httpClient = HttpClientBuilder.create().build();
    }

    @Get("/:project/:url:")
    public Payload getMethod(String project, String url, Context context) throws IOException {
        try {
            HttpGet httpUriRequest = new HttpGet(discourseUrl.toString() + "/" + url);
            HttpResponse response = httpClient.execute(httpUriRequest);
            return new Payload(response.getFirstHeader("Content-Type").toString(), response.getEntity().getContent(), response.getStatusLine().getStatusCode());
        }catch (Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}


