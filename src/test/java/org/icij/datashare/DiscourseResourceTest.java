package org.icij.datashare;

import net.codestory.http.errors.UnauthorizedException;
import net.codestory.http.filters.basic.BasicAuthFilter;
import net.codestory.http.security.Users;
import org.icij.datashare.session.LocalUserFilter;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import java.net.MalformedURLException;
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
    public void setUp() throws MalformedURLException {
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
    public void test_unauthorized_user() {
        get("/api/proxy/foo-datashare/my/url").withPreemptiveAuthentication("baz","null").should().respond(401);
    }

    @Test
    public void test_unknown_project() {
        get("/api/proxy/unknown_project/my/url").withPreemptiveAuthentication("foo","null").should().respond(401);
    }

    @Test(expected = RuntimeException.class)
    public void test_no_api_key() throws MalformedURLException {
        DiscourseResource discourseResource = new DiscourseResource(new PropertiesProvider());
        configure(routes -> routes.add(discourseResource).filter(new BasicAuthFilter("/api","ds", DatashareUser.singleUser("foo"))));
        get("/api/proxy/foo-datashare/my/url").withPreemptiveAuthentication("foo","null");
    }

//    @Test
//    public void test_user_in_discourse_response_header() {
//        get("/api/proxy/foo-datashare/my/url").withPreemptiveAuthentication("foo","bar").should().haveHeader("Api-Username","foo");
//    }

    @Test
    public void test_put() {
        put("/api/proxy/foo-datashare/my/url").withPreemptiveAuthentication("foo","null").should().respond(200).contain("\"Test\":\"Put\"");
    }

    @Test
    public void test_post() {
        post("/api/proxy/foo-datashare/my/url").withPreemptiveAuthentication("foo","null").should().respond(200).contain("\"Test\":\"Post\"");
    }
}
