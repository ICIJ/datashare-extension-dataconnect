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
        HttpGet httpUriRequest = new HttpGet(discourseUrl.toString() + "/" + getDiscoursePath(project, docId, context));
        prepareRequest(httpUriRequest);
        HttpResponse response = httpClient.execute(httpUriRequest);
        return new Payload(response.getFirstHeader("Content-Type").toString(), response.getEntity().getContent(), response.getStatusLine().getStatusCode());
    }

    @Put("/:project/:docId")
    public Payload create(String project, String docId, Context context) throws IOException {
        CommentAndTitle comment = context.extract(CommentAndTitle.class);
        // 1 get the topic of the document if it exists
        Payload topicAndPostsPayload = getMethod(project, docId, context);
        if (topicAndPostsPayload.isError()) {
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
        } else {
            // 3 if the topic exists then add a new reply
            return Payload.ok();
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

    public static void main(String[] args) {
        String json = "{\"id\":21,\"name\":null,\"username\":\"bthomas\",\"avatar_template\":\"/letter_avatar_proxy/v4/letter/b/ad7895/{size}.png\",\"created_at\":\"2020-10-09T14:04:56.954Z\",\"cooked\":\"\\u003cp\\u003ethis a good way to know what you want to do in life\\u003c/p\\u003e\",\"post_number\":1,\"post_type\":1,\"updated_at\":\"2020-10-09T14:04:56.954Z\",\"reply_count\":0,\"reply_to_post_number\":null,\"quote_count\":0,\"incoming_link_count\":0,\"reads\":0,\"readers_count\":0,\"score\":0,\"yours\":true,\"topic_id\":17,\"topic_slug\":\"datashare-document-number-a69983\",\"display_username\":null,\"primary_group_name\":null,\"primary_group_flair_url\":null,\"primary_group_flair_bg_color\":null,\"primary_group_flair_color\":null,\"version\":1,\"can_edit\":true,\"can_delete\":false,\"can_recover\":false,\"can_wiki\":true,\"user_title\":null,\"actions_summary\":[{\"id\":3,\"can_act\":true},{\"id\":4,\"can_act\":true},{\"id\":8,\"can_act\":true},{\"id\":7,\"can_act\":true}],\"moderator\":false,\"admin\":true,\"staff\":true,\"user_id\":1,\"draft_sequence\":0,\"hidden\":false,\"trust_level\":1,\"deleted_at\":null,\"user_deleted\":false,\"edit_reason\":null,\"can_view_edit_history\":true,\"wiki\":false,\"reviewable_id\":null,\"reviewable_score_count\":0,\"reviewable_score_pending_count\":0}";
        Matcher matcher = extractTopicId.matcher(json);
        System.out.println(matcher.matches());
        System.out.println(matcher.group(1));
    }
}
