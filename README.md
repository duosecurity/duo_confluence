# Overview

**duo_confluence** - Duo two-factor authentication filter for Confluence.

Adds Duo two-factor authentication to existing Seraph user authentication for
Confluence by redirecting the user to provide Duo credentials for requests
which require a logged-in user.

There are two parts to this project.  `duo_seraph_filter` is a Seraph
filter which redirects the user.  `duo_twofactor` is a Confluence plugin which
displays the Duo frame to the user to gather credentials.

This project has been tested with Confluence 4.2.1.

# Installation and configuration

Find the top directory of your Confluence installation, called $CONFLUENCE_DIR
below.  This is usually /opt/atlassian/confluence.

## First Steps

* [Sign up for a Duo account](http://www.duosecurity.com/pricing)
(free for <10 users!)
* Create a new Web SDK integration to get an ikey, skey, and API hostname.
(See [Getting Started](http://www.duosecurity.com/docs/getting_started)
for help.)
* Use [NTP](http://www.ntp.org/) to ensure that your server's time is correct.
* Generate an akey.  This is string of at least 40 characters that you should
keep secret from Duo.

See <http://www.duosecurity.com/docs/duoweb> for detailed instructions.

## Install the duo_java jar

Copy the prebuilt duo.jar from etc into the Confluence lib directory.

    cp etc/duo.jar $CONFLUENCE_DIR/confluence/WEB-INF/lib

## Install the plugin

The plugin provides the UI to send credentials to Duo and post results back.

Using the Confluence UI, upload and install
`etc/duo-twofactor-1.0-SNAPSHOT.jar`.

## Install the Seraph filter

  cp etc/duo-filter-1.0-SNAPSHOT.jar $CONFLUENCE_DIR/confluence/WEB-INF/lib

## Configure Confluence

web.xml is located at `$CONFLUENCE_DIR/confluence/WEB-INF/web.xml`.

Edit web.xml to add the filter.  Use the appropriate values for `ikey`,
`skey`, `akey`, and `host`.

    <filter>
        <filter-name>duoauth</filter-name>
        <filter-class>com.duosecurity.seraph.filter.DuoAuthFilter</filter-class>
        <init-param>
            <param-name>ikey</param-name>
            <param-value></param-value>
        </init-param>
        <init-param>
            <param-name>skey</param-name>
            <param-value></param-value>
        </init-param>
        <init-param>
            <param-name>akey</param-name>
            <param-value></param-value>
        </init-param>
        <init-param>
            <param-name>host</param-name>
            <param-value></param-value>
        </init-param>
    </filter>

Edit web.xml to add the filter mapping.  The mapping must be after security
filter and before any post-seraph filters.

    <filter-mapping>
        <filter-name>duoauth</filter-name>
        <url-pattern>/*</url-pattern>
        <dispatcher>FORWARD</dispatcher>
        <dispatcher>REQUEST</dispatcher>
    </filter-mapping>

# Next steps

Restart Confluence.  To test, visit any page which requires authentication.
You will need to authenticate with Duo after you have authenticated locally.

# Notes

To deactivate the filter, remove or comment out the filter mapping from web.xml
and restart Confluence.  Duo authentication will no longer be required.

XML-RPC and SOAP are not authenticated with Seraph unless an empty
authentication token is used.  For more information, see <http://confluence.atlassian.com/display/DOC/Understanding+User+Management+in+Confluence>.

# Support

Report any bugs, feature requests, etc. to us directly:
<https://github.com/duosecurity/duo_confluence/issues>
