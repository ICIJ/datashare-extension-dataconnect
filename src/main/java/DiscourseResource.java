import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.codestory.http.Context;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Post;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.errors.UnauthorizedException;
import net.codestory.http.payload.Payload;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.icij.datashare.PropertiesProvider;
import org.icij.datashare.user.User;

@Prefix("comments")
public class DiscourseResource {
    private final HttpClient httpClient;
    private final URL discourseUrl;
    private final String discourseApiUser;
    private final String discourseApiKey;
    @Inject
    public DiscourseResource(PropertiesProvider propertiesProvider) throws MalformedURLException {
        httpClient = HttpClientBuilder.create().build();
        discourseUrl = new URL(propertiesProvider.get("discourseUrl").orElse("http://discourse:3000"));
        discourseApiKey = propertiesProvider.get("discourseApiKey").orElseThrow(() -> new RuntimeException("no discourse api key found in settings"));
        discourseApiUser = propertiesProvider.get("discourseApiUser").orElseThrow(() -> new RuntimeException("no discourse api user found in settings"));
    }

    @Get("/:project/:docId/all")
    public Payload getMethod(String project, String docId, Context context) throws IOException {
        HttpGet httpUriRequest = new HttpGet(discourseUrl.toString() + "/" + getDiscoursePath(project, docId, context));
        prepareRequest(httpUriRequest);
        HttpResponse response = httpClient.execute(httpUriRequest);
        return new Payload(response.getFirstHeader("Content-Type").toString(), response.getEntity().getContent(), response.getStatusLine().getStatusCode());
    }

    @Post("/:project/:docId/all")
    public Payload postMethod(String project, String docId, Context context) throws IOException {
        HttpPost httpUriRequest = new HttpPost(discourseUrl.toString() + "/" + getDiscoursePath(project, docId, context));
        BasicHttpEntity entity = new BasicHttpEntity();
        entity.setContent(context.request().inputStream());
        httpUriRequest.setEntity(entity);
        prepareRequest(httpUriRequest);
        HttpResponse response = httpClient.execute(httpUriRequest);
        return new Payload(response.getFirstHeader("Content-Type").toString(), response.getEntity().getContent(), response.getStatusLine().getStatusCode());
    }

    private void prepareRequest(HttpUriRequest httpUriRequest) {
        httpUriRequest.addHeader("Api-Key", discourseApiKey);
        httpUriRequest.addHeader("Api-Username", discourseApiUser);
        httpUriRequest.addHeader("Content-Type", "application/json");
    }

    private String getDiscoursePath(String project, String docId, Context context) {
        if (!((User)context.currentUser()).isGranted(project)) {
            throw new UnauthorizedException();
        }
        return "t/hello-world-topic/11.json";
    }
}
