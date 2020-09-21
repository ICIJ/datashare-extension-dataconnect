import static java.lang.String.join;
import static java.util.stream.Collectors.toList;

import javax.inject.Inject;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import net.codestory.http.Context;
import net.codestory.http.Query;
import net.codestory.http.annotations.Get;
import net.codestory.http.annotations.Prefix;
import net.codestory.http.payload.Payload;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.icij.datashare.PropertiesProvider;

@Prefix("discourse")
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

    @Get("/:path:")
    public Payload getMethod(String path, Context context) throws IOException {
        HttpGet httpUriRequest = new HttpGet(discourseUrl.toString() + "/" + checkPath(path, context));
        httpUriRequest.addHeader("Api-Key", discourseApiKey);
        httpUriRequest.addHeader("Api-Username", discourseApiUser);
        httpUriRequest.addHeader("Content-Type", "application/json");
        HttpResponse response = httpClient.execute(httpUriRequest);
        return new Payload(response.getFirstHeader("Content-Type").toString(), response.getEntity().getContent(), response.getStatusLine().getStatusCode());
    }

    private String checkPath(String path, Context context) {
        //(DatashareUser)context.currentUser()).isGranted(index)
        return getUrlString(context, path);
    }

     private String getUrlString(Context context, String s) {
         if (context.query().keyValues().size() > 0) {
             s += "?" + getQueryAsString(context.query());
         }
         return s;
     }

     static String getQueryAsString(final Query query) {
         return join("&", query.keyValues().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(toList()));
     }
}
