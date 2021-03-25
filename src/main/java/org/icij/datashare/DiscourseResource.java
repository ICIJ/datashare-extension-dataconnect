package org.icij.datashare;

import kong.unirest.Unirest;
import kong.unirest.apache.ApacheClient;
import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.annotations.Put;
import net.codestory.http.errors.UnauthorizedException;
import net.codestory.http.payload.Payload;
import org.apache.http.impl.client.HttpClientBuilder;
import org.icij.datashare.user.User;

import javax.inject.Inject;
import java.io.IOException;

@Prefix("/api/proxy")
public class DiscourseResource {
    private final String discourseUrl;
    private final String discourseApiKey;

    @Inject
    public DiscourseResource(PropertiesProvider propertiesProvider) {
       discourseUrl = propertiesProvider.get("discourseUrl").orElse("http://discourse:3000");
       discourseApiKey = propertiesProvider.get("discourseApiKey").orElseThrow(() -> new IllegalStateException("no discourse api key found in settings"));
       Unirest.config().httpClient(ApacheClient.builder(HttpClientBuilder.create().build()));
    }

    @Get("/:project/:url:")
    public Payload getMethod(String project, String url, Context context) {
        checkProject(project,context);
        kong.unirest.HttpResponse<byte[]> httpResponse = Unirest.get(discourseUrl + "/" + url).
                header("Api-Key", discourseApiKey).
                header("Api-Username", context.currentUser().login()).
                header("Content-Type", "application/json").asBytes();
        return new Payload("application/json", httpResponse.getBody(), httpResponse.getStatus());
    }

    @Put("/:project/:url:")
    public Payload putMethod(String project, String url, Context context) throws IOException {
        checkProject(project,context);
        kong.unirest.HttpResponse<byte[]> httpResponse = Unirest.put(discourseUrl + "/" + url).
                header("Api-Key", discourseApiKey).
                header("Api-Username", context.currentUser().login()).
                header("Content-Type", "application/json").
                body(context.request().contentAsBytes()).asBytes();
        return new Payload("application/json", httpResponse.getBody(), httpResponse.getStatus());
    }

    @Post("/:project/:url:")
    public Payload postMethod(String project, String url, Context context) throws IOException {
        checkProject(project,context);
        kong.unirest.HttpResponse<byte[]> httpResponse = Unirest.post(discourseUrl + "/" + url).
                header("Api-Key", discourseApiKey).
                header("Api-Username", context.currentUser().login()).
                header("Content-Type", "application/json").
                body(context.request().contentAsBytes()).asBytes();
        return new Payload("application/json", httpResponse.getBody(), httpResponse.getStatus());
    }

    private void checkProject(String project, Context context) {
        if (!((User)context.currentUser()).isGranted(project)) {
            throw new UnauthorizedException();
        }
    }
}


