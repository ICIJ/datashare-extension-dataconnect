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
                .get("/my/url?foo=:foo&baz=:baz", (context, param1, param2) -> new HashMap<String, String>() {{
                    put("Method", "Get");
                    put("ParameterFoo", param1);
                    put("ParameterBaz", param2);
                }})
                .put("/my/url?foo=:foo",((context, param1) -> new HashMap<String,String>(){{
                    put("Method","Put");
                    put("ParameterFoo", param1);
                }}))
                .post("/my/url?foo=:foo&baz=:baz&quuz=:quuz",((context, param1, param2, param3) -> new HashMap<String,String>(){{
                    put("Method","Post");
                    put("ParameterFoo", param1);
                    put("ParameterBaz", param2);
                    put("ParameterQuuz", param3);}})));
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
        get("/api/proxy/foo-datashare/my/url").withPreemptiveAuthentication("foo","null").should().respond(200).contain("\"Method\":\"Get\"");
    }

    @Test
    public void test_get_with_request_parameters() {
        get("/api/proxy/foo-datashare/my/url?foo=bar&baz=qux").withPreemptiveAuthentication("foo","null").should().respond(200).
                contain("\"ParameterFoo\":\"bar\"").contain("\"ParameterBaz\":\"qux\"");
    }

    @Test
    public void test_put() {
        put("/api/proxy/foo-datashare/my/url").withPreemptiveAuthentication("foo","null").should().respond(200).contain("\"Method\":\"Put\"");
    }

    @Test
    public void test_put_with_request_parameters() {
        put("/api/proxy/foo-datashare/my/url?foo=bar&baz=qux").withPreemptiveAuthentication("foo","null").should().respond(200).
                contain("\"ParameterFoo\":\"bar\"");
    }

    @Test
    public void test_post() {
        post("/api/proxy/foo-datashare/my/url").withPreemptiveAuthentication("foo","null").should().respond(200).contain("\"Method\":\"Post\"");
    }

    @Test
    public void test_post_with_request_parameters() {
        post("/api/proxy/foo-datashare/my/url?foo=bar&baz=qux&quuz=thud").withPreemptiveAuthentication("foo","null").should().respond(200).
                contain("\"ParameterFoo\":\"bar\"").contain("\"ParameterBaz\":\"qux\"").contain("\"ParameterQuuz\":\"thud\"");
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
