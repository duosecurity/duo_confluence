package com.duosecurity.seraph.filter;

import java.io.IOException;
import java.security.Principal;
import java.net.URLEncoder;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Category;

import com.duosecurity.DuoWeb;

public class DuoAuthFilter implements javax.servlet.Filter
{
    private static final Category log = Category.getInstance(DuoAuthFilter.class);
    private FilterConfig filterConfig;

    public static final String OS_AUTHSTATUS_KEY = "os_authstatus";

    /** keys in a session where Duo attributes are stored. */
    public static final String DUO_REQUEST_KEY = "duo.request.key";
    public static final String DUO_HOST_KEY = "duo.host.key";
    public static final String DUO_ORIGINAL_URL_KEY = "duo.originalurl.key";
    public static final String DUO_AUTH_SUCCESS_KEY = "duo.authsuccess.key";

    /** key in a session for Duo response. */
    private static final String DUO_RESPONSE_ATTRIBUTE = "sig_response";

    /* page used for mobile login */
    private String mobileLoginUrl = "/plugins/servlet/mobile/login";
    /* config */
    private String ikey;
    private String skey;
    private String akey;
    private String host;
    private String loginUrl = "/plugins/servlet/duologin";
    private String[] unprotectedDirs = {"/download/resources/com.duosecurity.confluence.plugins.duo-twofactor:resources/"};

    /**
     * Return true if url should not be protected by Duo auth, even if we have
     * a local user.
     */
    private boolean isUnprotectedPage(String url) {
        // Is this url used for Duo auth?
        if (url.equals(this.loginUrl)) {
            return true;
        }
        for (String dir : this.unprotectedDirs) {
            if (url.startsWith(dir)) {
                return true;
            }
        }
        // Is this url used for mobile login?
        // This url is POSTed to after we have a user, but we'd rather not send
        // the user from here to the Duo auth, because there could be
        // credentials in the parameters that we'd want to take out of the URL
        // we redirect back to.
        if (url.equals(this.mobileLoginUrl)) {
            return true;
        }
        return false;
    }
 
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) 
        throws java.io.IOException, javax.servlet.ServletException
    {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        boolean needAuth = false;

        HttpSession session = httpServletRequest.getSession();
        Principal principal = httpServletRequest.getUserPrincipal();

        if (!isUnprotectedPage(httpServletRequest.getRequestURI())) {
            if (principal != null) {
                // User has logged in locally, has there been a Duo auth?
                if (session.getAttribute(DUO_AUTH_SUCCESS_KEY) == null) {
                    // are we coming from the Duo auth servlet?
                    String duoResponse = (String) session.getAttribute(DUO_RESPONSE_ATTRIBUTE);
                    if (duoResponse != null) {
                        String duoUsername = DuoWeb.verifyResponse(ikey, skey, akey, duoResponse);
                        if (duoUsername.equals(principal.getName())) {
                            session.setAttribute(DUO_AUTH_SUCCESS_KEY, true);
                        } else {
                            needAuth = true;
                        }
                    } else {
                        needAuth = true;
                    }
                } // user has already authed with us this session
            } // no user -> Seraph has not required auth -> we don't either
        }     // we're serving a page for Duo auth

        if (needAuth) {
            // Redirect to servlet if we can.  If the request is committed,
            // we can't, possibly because there's already a redirection in
            // progress; this is what Seraph's SecurityFilter does.
            if (!httpServletResponse.isCommitted()) {
                String sigRequest = DuoWeb.signRequest(ikey, skey, akey, principal.getName());
                final String originalURL = httpServletRequest.getServletPath() + (httpServletRequest.getPathInfo() == null ? "" : httpServletRequest.getPathInfo()) + (httpServletRequest.getQueryString() == null ? "" : "?" + httpServletRequest.getQueryString());
                String qs = new String();
                String redirectUrl = new String();
                qs = DUO_REQUEST_KEY + "=" + sigRequest;
                qs = qs + "&" + DUO_HOST_KEY + "=" + URLEncoder.encode(host, "UTF-8");
                qs = qs + "&" + DUO_ORIGINAL_URL_KEY + "=" + URLEncoder.encode(originalURL, "UTF-8");
                redirectUrl = this.loginUrl + "?" + qs;
                httpServletResponse.sendRedirect(redirectUrl);
            } else {
                log.warn("Could not redirect to Duo auth page.");
            }
            return;
        }

        // We do not need Duo auth.  Continue with the filter chain.
        chain.doFilter(request, response);
    }
 
    public void init(final FilterConfig filterConfig)
    {
        this.ikey = filterConfig.getInitParameter("ikey");
        this.skey = filterConfig.getInitParameter("skey");
        this.akey = filterConfig.getInitParameter("akey");
        this.host = filterConfig.getInitParameter("host");

        if (filterConfig.getInitParameter("login.url") != null) {
            this.loginUrl = filterConfig.getInitParameter("login.url");
        }
        if (filterConfig.getInitParameter("unprotected.dirs") != null) {
            this.unprotectedDirs = filterConfig.getInitParameter("unprotected.dirs").split(" ");
        }
    }
 
    public void destroy()
    {
        filterConfig = null;
    }
}
