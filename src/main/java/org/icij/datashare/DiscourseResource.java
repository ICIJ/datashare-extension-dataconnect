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
import java.util.StringJoiner;

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

    @Get("/:project/custom-fields-api/topics/:id?page=:page&limit=:limit&post_number=:post_number")
    public Payload getMethod(final String project, final String id, final String page, final String limit, final String postNumber, Context context) {
        checkProject(project,context);
        kong.unirest.HttpResponse<byte[]> httpResponse = Unirest.get(discourseUrl + "/custom-fields-api/topics/" + id + buildQueryParameter(page,limit,postNumber)).
                header("Api-Key", discourseApiKey).
                header("Api-Username", context.currentUser().login()).
                header("Content-Type", "application/json").asBytes();
        System.out.println(buildQueryParameter(page,limit,postNumber));
        return new Payload("application/json", httpResponse.getBody(), httpResponse.getStatus());
    }

    @Put("/:project/custom-fields-api/topics/:id?page=:page&limit=:limit&post_number:=post_number")
    public Payload putMethod(final String project, final String id, final String page, final String limit, final String postNumber, Context context) throws IOException {
        checkProject(project,context);
        kong.unirest.HttpResponse<byte[]> httpResponse = Unirest.put(discourseUrl + "/custom-fields-api/topics/" + id + buildQueryParameter(page,limit,postNumber)).
                header("Api-Key", discourseApiKey).
                header("Api-Username", context.currentUser().login()).
                header("Content-Type", "application/json").
                body(context.request().contentAsBytes()).asBytes();
        return new Payload("application/json", httpResponse.getBody(), httpResponse.getStatus());
    }

    @Post("/:project/custom-fields-api/topics/:id?page=:page&limit=:limit&post_number:=post_number")
    public Payload postMethod(final String project, final String id, final String page, final String limit, final String postNumber, Context context) throws IOException {
        checkProject(project,context);
        kong.unirest.HttpResponse<byte[]> httpResponse = Unirest.post(discourseUrl + "/custom-fields-api/topics/" + id + buildQueryParameter(page,limit,postNumber)).
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

    public static String buildQueryParameter(final String page, final String limit, final String postNumber) {
        StringJoiner query = new StringJoiner("&", "?","").setEmptyValue("");
        if(page != null) query.add("page=" + page);
        if(limit != null) query.add("limit=" + limit);
        if(postNumber != null) query.add("post_number=" + postNumber);
        return query.toString();
    }
}


