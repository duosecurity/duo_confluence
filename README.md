# Overview

**duo_confluence** - Duo two-factor authentication filter for Confluence.

Adds Duo two-factor authentication to existing Seraph user authentication for
Confluence by redirecting the user to provide Duo credentials for requests
which require a logged-in user.

There are two parts to this project.  `duo_seraph_filter` is a Seraph
filter which redirects the user.  `duo_twofactor` is a Confluence plugin which
displays the Duo frame to the user to gather credentials.

This project has been tested with Confluence 4.2.1 and the Duo Web SDK v1.

# Installation and configuration

## Acquire and generate keys

See <http://www.duosecurity.com/docs/duoweb>.  Sign up to get an skey, ikey, and
host, and generate your own secret akey.

## Install duo_java

Copy the prebuilt duo.jar from etc into the Confluence lib directory; on my
CentOS box, the location is:

    cp etc/duo.jar /opt/atlassian/confluence/confluence/WEB-INF/lib

If you'd prefer to build your own duo.jar instead, the source is available from
github at <https://github.com/duosecurity/duo_java>.  In a temporary directory:

    git clone git://github.com/duosecurity/duo_java.git
    mkdir class
    javac duo_java/DuoWeb/src/com/duosecurity/Base64.java duo_java/DuoWeb/src/com/duosecurity/DuoWeb.java duo_java/DuoWeb/src/com/duosecurity/Util.java -d class
    jar cf duo.jar -C class .

After this step, the built duo.jar can be copied to the Confluence lib
directory as above.        

## Install plugin

The plugin only provides the UI to send credentials to Duo and post
results back.  Any page, served from any location, which accepts the same URL
arguments and serves the Duo frame and Javascript can be used; the plugin is
probably the easiest way.

### Configure the duologin.vm authentication page

The `duologin.vm` page can be used as-is, or styled to match your organization.
Put it in the resources directory at `duo_twofactor/src/main/resources`.  If
you want the Duo authentication page to include other resources, such as
scripts or images, put them in the resources directory as well, and edit
`atlassian-plugin.xml` to add them to the served resources.

### Add Duo javascript to resources

Copy the Duo javascript page into duo_twofactor/src/main/resources.  This page
is copied from the duo_java distribution.

  cp etc/Duo-Web-v1.bundled.js duo_twofactor/src/main/resources

If you'd prefer to use a different script distributed with duo_java, the source
is available from github as mentioned in 'install duo_java'.  The script to
use and its path are determined by the contents of duologin.vm.  The example
duologin.vm page uses:

    <script src="/Duo-Web-v1.bundled.js">

If you use a different script, edit `atlassian-plugin.xml` to add it to the
served resources.

### Build the plugin

    cd duo_twofactor
    mvn package

### Install the plugin

Using the Confluence UI, upload and install the jar built in the target
directory.

## Build and install the filter

  cd duo_seraph_filter
  mvn package
  cp target/duo-filter-1.0-SNAPSHOT.jar /opt/atlassian/confluence/confluence/WEB-INF/lib

## Configure Confluence

Edit web.xml to add the filter.  Use the appropriate values for `skey`,
`ikey`, `akey`, and `host`.

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

If you're not using the supplied plugin, add values for login.url and
unprotected.dirs.  URLs matching these will not require Duo authentication.
login.url is a single path, which must match the URL path of the request
exactly, while unprotected.dirs is a space-separated sequence of URL prefixes,
which must match the beginning of the URL path of the request.  If these
are not configured, the defaults, which work for the supplied plugin, are:

        <init-param>
            <param-name>login.url</param-name>
            <param-value>/plugins/servlet/duologin</param-value>
        </init-param>
        <init-param>
            <param-name>unprotected.dirs</param-name>
            <param-value>/download/resources/com.duosecurity.confluence.plugins.duo-twofactor:resources/</param-value>
        </init-param>

If you are using the supplied plugin, but need its Duo auth page to include
further resources (such as images or scripts), either put them in the plugin's
resources directory, or add them to a directory which is in unprotected.dirs.

Edit web.xml to add the filter mapping.  The mapping must be after security
filter and before any post-seraph filters.

    <filter-mapping>
        <filter-name>duoauth</filter-name>
        <url-pattern>/*</url-pattern>
        <dispatcher>FORWARD</dispatcher>
        <dispatcher>REQUEST</dispatcher>
    </filter-mapping>

# Next steps

Restart confluence.  To test, visit any page which requires authentication.
You will need to authenticate with Duo after you have authenticated locally.

# Notes

To deactivate the filter, remove or comment out the filter mapping from web.xml
and restart Confluence.  Duo authentication will no longer be required.

XML-RPC and SOAP are not authenticated with Seraph unless an empty
authentication token is used.  For more information, see <http://confluence.atlassian.com/display/DOC/Understanding+User+Management+in+Confluence>.

# Support

Report any bugs, feature requests, etc. to us directly:
<https://github.com/duosecurity/duo_confluence/issues>
