package com.duosecurity.seraph.filter;

import com.duosecurity.DuoWeb;
import java.net.URLEncoder;
import java.security.Principal;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.log4j.Category;

@SuppressWarnings("UnusedDeclaration")
public class DuoAuthFilter implements javax.servlet.Filter {
  private static final Category log = Category.getInstance(DuoAuthFilter.class);

  public static final String OS_AUTHSTATUS_KEY = "os_authstatus";
  public static final String LOGIN_SUCCESS = "success";

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
  private String[] unprotectedDirs = {
      "/download/resources/com.duosecurity.confluence.plugins.duo-twofactor:resources/",
      "/rest/gadget/1.0/login"
  };
  private boolean isOAuthUnprotected = false;

  /**
   * Return true if url should not be protected by Duo auth, even if we have
   * a local user.
   */
  private boolean isUnprotectedPage(String url) {
    // Is this url used for Duo auth?
    if (url.equals(loginUrl)) {
      return true;
    }
    for (String dir : unprotectedDirs) {
      if (url.startsWith(dir)) {
        return true;
      }
    }
    // Is this url used for mobile login?
    // This url is POSTed to after we have a user, but we'd rather not send
    // the user from here to the Duo auth, because there could be
    // credentials in the parameters that we'd want to take out of the URL
    // we redirect back to.
    if (url.equals(mobileLoginUrl)) {
      return true;
    }
    return false;
  }

  @Override public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws java.io.IOException, javax.servlet.ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
    boolean needAuth = false;

    HttpSession session = httpServletRequest.getSession();
    Principal principal = httpServletRequest.getUserPrincipal();

    String contextPath = ((HttpServletRequest) request).getContextPath();

    if (!isUnprotectedPage(httpServletRequest.getRequestURI().replaceFirst(contextPath, ""))) {
      if (request.getAttribute(OS_AUTHSTATUS_KEY) != null && isOAuthUnprotected) {
        // Request has gone through OAuth, we're done if it succeeded
        if (!request.getAttribute(OS_AUTHSTATUS_KEY).equals(LOGIN_SUCCESS)) {
          throw new ServletException("OAuth authentication failed");
        }
      } else if (principal != null) {
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
      } // no user -> Seraph has not required auth -> we don't either,
        // or user came from OAuth and we're configured to not require 2fa for that
    }     // we're serving a page for Duo auth

    if (needAuth) {
      // Redirect to servlet if we can.  If the request is committed,
      // we can't, possibly because there's already a redirection in
      // progress; this is what Seraph's SecurityFilter does.
      if (!httpServletResponse.isCommitted()) {
        String sigRequest = DuoWeb.signRequest(ikey, skey, akey, principal.getName());
        final String originalURL = contextPath + httpServletRequest.getServletPath()
            + (httpServletRequest.getPathInfo() == null ? "" : httpServletRequest.getPathInfo())
            + (httpServletRequest.getQueryString() == null ? ""
            : "?" + httpServletRequest.getQueryString());
        String qs;
        String redirectUrl;
        qs = DUO_REQUEST_KEY + "=" + sigRequest;
        qs = qs + "&" + DUO_HOST_KEY + "=" + URLEncoder.encode(host, "UTF-8");
        qs = qs + "&" + DUO_ORIGINAL_URL_KEY + "=" + URLEncoder.encode(originalURL, "UTF-8");
        redirectUrl = contextPath + loginUrl + "?" + qs;
        httpServletResponse.sendRedirect(redirectUrl);
      } else {
        log.warn("Could not redirect to Duo auth page.");
      }
      return;
    }

    // We do not need Duo auth.  Continue with the filter chain.
    chain.doFilter(request, response);
  }

  @Override public void init(final FilterConfig filterConfig) {
    ikey = filterConfig.getInitParameter("ikey");
    skey = filterConfig.getInitParameter("skey");
    akey = filterConfig.getInitParameter("akey");
    host = filterConfig.getInitParameter("host");

    if (filterConfig.getInitParameter("login.url") != null) {
      loginUrl = filterConfig.getInitParameter("login.url");
    }
    if (filterConfig.getInitParameter("unprotected.dirs") != null) {
      unprotectedDirs = filterConfig.getInitParameter("unprotected.dirs").split(" ");
    }

    if (filterConfig.getInitParameter("unprotect.OAuth") != null) {
      isOAuthUnprotected = Boolean.getBoolean(filterConfig.getInitParameter("unprotect.OAuth"));
    }
  }

  @Override public void destroy() {

  }
}
