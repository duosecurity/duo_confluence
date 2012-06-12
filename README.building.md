# Building manually

## Build the duo_java jar

If you'd prefer to build your own duo.jar, the source is available from github
at <https://github.com/duosecurity/duo_java>.  In a temporary directory:

    git clone git://github.com/duosecurity/duo_java.git
    mkdir class
    javac duo_java/DuoWeb/src/com/duosecurity/Base64.java duo_java/DuoWeb/src/com/duosecurity/DuoWeb.java duo_java/DuoWeb/src/com/duosecurity/Util.java -d class
    jar cf duo.jar -C class .

After this step, the built jar can be copied to the Confluence lib directory
as described in **Install duo.jar**.

## Build the plugin jar

### Optionally customize the Duo authentication page

The authentication page is `duo_twofactor/src/main/resources/duologin.vm`.  It
can be used as-is, or styled to match your organization.

If you want the Duo authentication page to include other resources, such as 
scripts or images, put them in the resources directory as well, and edit
`atlassian-plugin.xml` to add them to the served resources.  After customizing,
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

