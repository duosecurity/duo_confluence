# Overview

**duo_confluence** - Duo two-factor authentication filter for Confluence.

Adds Duo two-factor authentication to existing Seraph user authentication for
Confluence by redirecting the user to provide Duo credentials for requests
which require a logged-in user.

There are two parts to this project.  `duo_seraph_filter` is a Seraph
filter which redirects the user.  `duo_twofactor` is a Confluence plugin which
displays the Duo frame to the user to gather credentials.

This project has been tested with Confluence 4.2.1.

# First Steps

* [Sign up for a Duo account](http://www.duosecurity.com/pricing).
* Create a new Web SDK integration to get an ikey, skey, and API hostname.
(See [Getting Started](http://www.duosecurity.com/docs/getting_started)
for help.)
* Use [NTP](http://www.ntp.org/) to ensure that your server's time is correct.
* Generate an akey.  This is string of at least 40 characters that you should
keep secret from Duo.

See <http://www.duosecurity.com/docs/duoweb> for detailed instructions.

# Automatic Installation Instructions

Run the install script as follows:

```
$ ./install.sh -i <your_ikey> -s <your_skey> -h <your_host> -d <confluence_location>
```

- The -d option specifies where Confluence is installed (not required, defaults to /opt/atlassian/confluence)
- You can get your ikey, skey, and host from the administrative panel at http://admin.duosecurity.com. The integration type should be Web SDK.

After running the install script, follow the instructions to edit your
configuration and install the plugin.

# Manual Installation Instructions

Find the top directory of your Confluence installation, called `$CONFLUENCE_DIR`
below.  This is usually /opt/atlassian/confluence.

## Install the duo_java jar

Copy the prebuilt duo.jar from etc into the Confluence lib directory.

    cp etc/duo.jar $CONFLUENCE_DIR/confluence/WEB-INF/lib

## Install the plugin

The plugin provides the UI to send credentials to Duo and post results back.

Using the Confluence UI, upload and install
etc/duo-twofactor-1.0-SNAPSHOT.jar.

## Install the Seraph filter

Copy the prebuilt duo-filter-1.0-SNAPSHOT.jar from etc into the Confluence lib directory.

    cp etc/duo-filter-1.0-SNAPSHOT.jar $CONFLUENCE_DIR/confluence/WEB-INF/lib

## Configure Confluence

Configure Confluence by editing web.xml, located at
`$CONFLUENCE_DIR`/confluence/WEB-INF/web.xml.

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

Edit web.xml to add the filter mapping.  The mapping must be after the security
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

# Appendix: Building manually

Jars and templates are located in the etc directory.  If
you'd prefer to build your own jars, here is how to do it.  The plugin jar
must be rebuilt if you want to customize the Duo authentication page.

## Build the duo_java jar

If you'd prefer to build your own duo.jar, the source is available from Github
at <https://github.com/duosecurity/duo_java>.  In a temporary directory:

    git clone git://github.com/duosecurity/duo_java.git
    mkdir class
    javac duo_java/DuoWeb/src/com/duosecurity/Base64.java duo_java/DuoWeb/src/com/duosecurity/DuoWeb.java duo_java/DuoWeb/src/com/duosecurity/Util.java -d class
    jar cf duo.jar -C class .

After this step, the built jar can be copied to the Confluence lib directory
as described in **Install duo.jar**.

## Build the plugin jar

### Optionally customize the Duo authentication page

The authentication page template is
duo_twofactor/src/main/resources/duologin.vm.  It can be used as-is, or styled
to match your organization.

If you want the Duo authentication page to include other resources, such as 
scripts or images, put them in the resources directory as well, and edit
atlassian-plugin.xml to add them to the served resources.  After customizing,
rebuild and install the jar.

### Build the jar

If you'd prefer to build your own duo-twofactor-1.0-SNAPSHOT.jar, it can
be built with Maven:

    cd duo_twofactor
    mvn package

After this step, the built jar can be installed as described in
**Install the plugin**.

## Build the Seraph filter jar

If you'd prefer to build your own duo-filter-1.0-SNAPSHOT.jar, it can be
built with Maven:

    cd duo_seraph_filter
    mvn package

After this step, the built jar can be installed as described in
**Install the Seraph filter**.
