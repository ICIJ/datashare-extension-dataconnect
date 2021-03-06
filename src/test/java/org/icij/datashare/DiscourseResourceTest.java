package org.icij.datashare;

import net.codestory.http.filters.basic.BasicAuthFilter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.HashMap;

public class DiscourseResourceTest extends AbstractProdWebServerTest{
    @ClassRule
    public static ProdWebServerRule discourse = new ProdWebServerRule();
    @BeforeClass
    public static void setUpDiscourse() {
        discourse.configure(routes -> routes
                .get("/my/url", new HashMap<String, String>() {{
                    put("Test", "Get");
                }})
                .put("/my/url",(context -> new HashMap<String,String>(){{
                    put("Test","Put");
                }}))
                .post("/my/url",(context -> new HashMap<String,String>(){{
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
        get("/api/proxy/foo-datashare/my/url").withPreemptiveAuthentication("foo","null").should().respond(200).contain("\"Test\":\"Get\"");
    }

    @Test
    public void test_put() {
        put("/api/proxy/foo-datashare/my/url").withPreemptiveAuthentication("foo","null").should().respond(200).contain("\"Test\":\"Put\"");
    }

    @Test
    public void test_post() {
        post("/api/proxy/foo-datashare/my/url").withPreemptiveAuthentication("foo","null").should().respond(200).contain("\"Test\":\"Post\"");
    }

    @Test
    public void test_unauthorized_user() {
        get("/api/proxy/foo-datashare/my/url").withPreemptiveAuthentication("baz","null").should().respond(401);
    }

    @Test
    public void test_unknown_project() {
        get("/api/proxy/unknown_project/my/url").withPreemptiveAuthentication("foo","null").should().respond(401);
    }

    @Test
    public void test_unknown_url() {
        get("/api/proxy/foo-datashare/unknown/url").withPreemptiveAuthentication("foo","null").should().respond(404);
    }

    @Test(expected = IllegalStateException.class)
    public void test_no_api_key_fails_at_constructor() {
        new DiscourseResource(new PropertiesProvider());
    }
}
