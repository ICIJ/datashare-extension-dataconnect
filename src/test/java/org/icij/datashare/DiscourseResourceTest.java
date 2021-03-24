package org.icij.datashare;

import net.codestory.http.filters.basic.BasicAuthFilter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import static org.fest.assertions.Assertions.assertThat;

import java.util.HashMap;

import static org.icij.datashare.DiscourseResource.buildQueryParameter;

public class DiscourseResourceTest extends AbstractProdWebServerTest{
    @ClassRule
    public static ProdWebServerRule discourse = new ProdWebServerRule();
    @BeforeClass
    public static void setUpDiscourse() {
        discourse.configure(routes -> routes
                .get("/custom-fields-api/topics/id", new HashMap<String, String>() {{
                    put("Test", "Get");
                }})
                .put("/custom-fields-api/topics/id",(context -> new HashMap<String,String>(){{
                    put("Test","Put");
                }}))
                .post("/custom-fields-api/topics/id",(context -> new HashMap<String,String>(){{
                    put("Test","Post");}})));
    }
    @Before
    public void setUp() {
        DiscourseResource discourseResource = new DiscourseResource(new PropertiesProvider(new HashMap<String, String>() {{
            put("discourseUrl", "http://localhost:" + discourse.port());
            put("discourseApiKey", "testApiKey");
        }}));
        configure(routes -> routes.add(discourseResource).filter(new BasicAuthFilter("/api","ds", DatashareUser.singleUser("foo"))));
    }

    @Test
    public void test_get() {
        get("/api/proxy/foo-datashare/custom-fields-api/topics/id").withPreemptiveAuthentication("foo","null").should().respond(200).contain("\"Test\":\"Get\"");
    }

    @Test
    public void test_get_with_parameters() {
        get("/api/proxy/foo-datashare/custom-fields-api/topics/id?page=foo&limit=bar&post_number=baz").withPreemptiveAuthentication("foo","null").should().respond(200).contain("\"Test\":\"Get\"");
    }

    @Test
    public void test_put() {
        put("/api/proxy/foo-datashare/custom-fields-api/topics/id").withPreemptiveAuthentication("foo","null").should().respond(200).contain("\"Test\":\"Put\"");
    }

    @Test
    public void test_post() {
        post("/api/proxy/foo-datashare/custom-fields-api/topics/id").withPreemptiveAuthentication("foo","null").should().respond(200).contain("\"Test\":\"Post\"");
    }

    @Test
    public void test_unauthorized_user() {
        get("/api/proxy/foo-datashare/custom-fields-api/topics/id").withPreemptiveAuthentication("baz","null").should().respond(401);
    }

    @Test
    public void test_unknown_project() {
        get("/api/proxy/unknown_project/custom-fields-api/topics/id").withPreemptiveAuthentication("foo","null").should().respond(401);
    }

    @Test
    public void test_unknown_id() {
        get("/api/proxy/foo-datashare/custom-fields-api/topics/unknown/id").withPreemptiveAuthentication("foo","null").should().respond(404);
    }

    @Test
    public void test_build_query_parameter_empty() {
        assertThat(buildQueryParameter(null,null,null)).isEqualTo("");
    }

    @Test
    public void test_build_query_parameter() {
        assertThat(buildQueryParameter("foo",null,null)).isEqualTo("?page=foo");
        assertThat(buildQueryParameter(null,"bar",null)).isEqualTo("?limit=bar");
        assertThat(buildQueryParameter("foo","bar","baz")).isEqualTo("?page=foo&limit=bar&post_number=baz");
    }

    @Test(expected = IllegalStateException.class)
    public void test_no_api_key_fails_at_constructor() {
        new DiscourseResource(new PropertiesProvider());
    }
}
