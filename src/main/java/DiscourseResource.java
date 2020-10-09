import static java.lang.String.format;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.icij.datashare.PropertiesProvider;
import org.icij.datashare.user.User;

@Prefix("comments")
public class DiscourseResource {
    static Pattern extractTopicId = Pattern.compile(".*,\"topic_id\":(\\d+),.*");
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
        HttpResponse response = getTopicPosts(project, docId, context);
        return new Payload(response.getFirstHeader("Content-Type").toString(), response.getEntity().getContent(), response.getStatusLine().getStatusCode());
    }

    @Put("/:project/:docId")
    public Payload create(String project, String docId, Context context) throws IOException {
        CommentAndTitle comment = context.extract(CommentAndTitle.class);
        // 1 get the topic of the document if it exists
        HttpResponse topicAndPostsPayload = getTopicPosts(project, docId, context);
        if (topicAndPostsPayload.getStatusLine().getStatusCode() == 404) {
            // 2 if it doesn't exist create a topic with the comment
            HttpPost httpUriRequest = new HttpPost(discourseUrl.toString() + "/posts.json");
            BasicHttpEntity entity = new BasicHttpEntity();
            entity.setContentType(new BasicHeader("Content-Type","application/json"));
            String json = new ObjectMapper().writeValueAsString(comment);
            entity.setContent(new ByteArrayInputStream(json.getBytes()));
            httpUriRequest.setEntity(entity);
            prepareRequest(httpUriRequest);
            entity.setContentLength(json.length());
            HttpResponse response = httpClient.execute(httpUriRequest);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            IOUtils.copy(response.getEntity().getContent(), output);
            String topicPayload = output.toString();

            // 2.1 then add a custom field for the topic
            Matcher matcher = extractTopicId.matcher(topicPayload);
            if (matcher.matches()) {
                String topicId = matcher.group(1);
                HttpPut httpCustomField = new HttpPut(discourseUrl.toString() + getDiscoursePath(project, topicId, context));
                BasicHttpEntity entityForPut = new BasicHttpEntity();
                entityForPut.setContentType(new BasicHeader("Content-Type","application/json"));
                String body = format("{\"datashare_document_id\": \"%s\"}", docId);
                entityForPut.setContent(new ByteArrayInputStream(body.getBytes()));
                entityForPut.setContentLength(body.length());
                httpCustomField.setEntity(entityForPut);
                prepareRequest(httpCustomField);
                HttpResponse putResponse = httpClient.execute(httpCustomField);
                return new Payload(putResponse.getFirstHeader("Content-Type").toString(), putResponse.getEntity().getContent(), putResponse.getStatusLine().getStatusCode());
            } else {
                throw new IllegalStateException("cannot find topic id");
            }
        } else if (topicAndPostsPayload.getStatusLine().getStatusCode() == 200) {
            // 3 if the topic exists then add a new reply
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            IOUtils.copy(topicAndPostsPayload.getEntity().getContent(), output);
            String topicPayload = output.toString();
            Matcher matcher = extractTopicId.matcher(topicPayload);
            if (matcher.matches()) {
                String topicId = matcher.group(1);
                HttpPost replyPost = new HttpPost(discourseUrl.toString() + "/posts.json");
                BasicHttpEntity entity = new BasicHttpEntity();
                entity.setContentType(new BasicHeader("Content-Type","application/json"));
                String body = format("{\"topic_id\": \"%s\",\"raw\":\"%s\"}", topicId, comment.raw);
                entity.setContent(new ByteArrayInputStream(body.getBytes()));
                entity.setContentLength(body.length());
                replyPost.setEntity(entity);
                prepareRequest(replyPost);
                HttpResponse postResponse = httpClient.execute(replyPost);
                return new Payload(postResponse.getFirstHeader("Content-Type").toString(), postResponse.getEntity().getContent(), postResponse.getStatusLine().getStatusCode());
            } else {
                throw new IllegalStateException("cannot find topic id");
            }
        } else {
            return new Payload(topicAndPostsPayload.getFirstHeader("Content-Type").toString(), topicAndPostsPayload.getEntity().getContent(), topicAndPostsPayload.getStatusLine().getStatusCode());
        }
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
    private HttpResponse getTopicPosts(String project, String docId, Context context) throws IOException {
        HttpGet httpUriRequest = new HttpGet(discourseUrl.toString() + "/" + getDiscoursePath(project, docId, context));
        prepareRequest(httpUriRequest);
        HttpResponse response = httpClient.execute(httpUriRequest);
        return response;
    }

    private String getDiscoursePath(String project, String docId, Context context) {
        if (!((User)context.currentUser()).isGranted(project)) {
            throw new UnauthorizedException();
        }
        return "/custom-fields-api/topics/" + docId + ".json";
    }

    private static class CommentAndTitle {
        public final String raw;
        public final String title;
        @JsonCreator
        private CommentAndTitle(@JsonProperty("title") String title, @JsonProperty("raw") String raw) {
            this.raw = raw;
            this.title = title;
        }
    }
}
