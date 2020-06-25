package com.duosecurity.confluence.plugins;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.ws.rs.core.UriBuilder;
import com.atlassian.templaterenderer.TemplateRenderer;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.net.URLEncoder;
import org.apache.log4j.Category;
import org.apache.commons.lang.StringEscapeUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class TwoFactorLoginServlet extends HttpServlet
{
    private static final Category log = Category.getInstance(TwoFactorLoginServlet.class);

    private final TemplateRenderer renderer;

    /** keys in a session where Duo attributes are stored. */
    public static final String DUO_REQUEST_KEY = "duo.request.key";
    public static final String DUO_HOST_KEY = "duo.host.key";
    public static final String DUO_ORIGINAL_URL_KEY = "duo.originalurl.key";

    /** key in a session for Duo response. */
    private static final String DUO_RESPONSE_ATTRIBUTE = "sig_response";
    
    public TwoFactorLoginServlet(TemplateRenderer renderer)
    {
        this.renderer = renderer;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        Map<String,Object> context = new HashMap<String,Object>();
        final String actionUrl = request.getServletPath();
        // Encode the original URL suitably for a query param, since the
        // template will quote it if we're >= 5.1.2.
        final String actionArg = URLEncoder.encode(request.getParameter(DUO_ORIGINAL_URL_KEY), "UTF-8").replaceAll("\\+", "%20");

        // Encode the template arguments in case we're < 5.1.2
        context.put("sigRequest", StringEscapeUtils.escapeJavaScript(request.getParameter(DUO_REQUEST_KEY)));
        context.put("duoHost", StringEscapeUtils.escapeJavaScript(request.getParameter(DUO_HOST_KEY)));
        context.put("actionUrl", StringEscapeUtils.escapeJavaScript(request.getContextPath() + UriBuilder.fromUri(actionUrl).queryParam(DUO_ORIGINAL_URL_KEY, actionArg).build().toString()));
        context.put("contextPath", request.getContextPath());
        renderer.render("duologin.vm", context, response.getWriter());
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        HttpSession session = request.getSession();
        // Put the Duo response in the session.
        session.setAttribute(DUO_RESPONSE_ATTRIBUTE, request.getParameter(DUO_RESPONSE_ATTRIBUTE));
        // Send the user to the original destination.

        String safeURL = getSafeURL(request.getParameter(DUO_ORIGINAL_URL_KEY));

        response.sendRedirect(safeURL);        
    }

    private String getSafeURL(String originalurl) {
        URI uri;

        try {
            uri = new URI(originalurl);

            if (uri.isOpaque()) {        
                return "/";
            }
            else if (uri.isAbsolute()) {
                return uri.getPath().toString();
            }
            else {
                return originalurl;
            }
        }
        catch(URISyntaxException e) {
            log.warn("URISyntaxException when handling Duo original redirect URL:" + originalurl);
            log.warn(e.toString());
            return "/";
        }
    }
}
