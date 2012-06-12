package com.duosecurity.confluence.plugins;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.atlassian.templaterenderer.TemplateRenderer;

import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

import org.apache.log4j.Category;
import org.apache.commons.lang.StringEscapeUtils;

public class TwoFactorLoginServlet extends HttpServlet
{
    private static final Category log = Category.getInstance(TwoFactorLoginServlet.class);

    private final TemplateRenderer renderer;

    /** keys in a session where Duo attributes are stored. */
    public static final String DUO_REQUEST_KEY = "duo.request.key";
    public static final String DUO_HOST_KEY = "duo.host.key";
    public static final String DUO_ORIGINAL_URL_KEY = "duo.originalurl.key";
    
    public TwoFactorLoginServlet(TemplateRenderer renderer)
    {
        this.renderer = renderer;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        HttpSession session = request.getSession();
        Map<String,Object> context = new HashMap<String,Object>();
        context.put("sigRequest", StringEscapeUtils.escapeJavaScript(request.getParameter(DUO_REQUEST_KEY)));
        context.put("duoHost", StringEscapeUtils.escapeJavaScript(request.getParameter(DUO_HOST_KEY)));
        context.put("actionUrl", StringEscapeUtils.escapeJavaScript(request.getParameter(DUO_ORIGINAL_URL_KEY)));
        renderer.render("duologin.vm", context, response.getWriter());
    }

}
