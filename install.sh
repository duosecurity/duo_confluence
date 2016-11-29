#!/bin/sh

# default confluence install directory
CONFLUENCE=/opt/atlassian/confluence
AKEY=`python -c "import hashlib, os;  print hashlib.sha1(os.urandom(32)).hexdigest()"`

usage () {
    printf >&2 "Usage: $0 [-d confluence directory] -i ikey -s skey -h host\n"
    printf >&2 "ikey, skey, and host can be found in Duo account's administration panel at admin.duosecurity.com\n"
}

while getopts d:i:s:h: o
do  
    case "$o" in
        d)  CONFLUENCE="$OPTARG";;
        i)  IKEY="$OPTARG";;
        s)  SKEY="$OPTARG";;
        h)  HOST="$OPTARG";;
        [?]) usage
            exit 1;;
    esac
done

if [ -z $IKEY ]; then echo "Missing -i (Duo integration key)"; usage; exit 1; fi
if [ -z $SKEY ]; then echo "Missing -s (Duo secret key)"; usage; exit 1; fi
if [ -z $HOST ]; then echo "Missing -h (Duo API hostname)"; usage; exit 1; fi

echo "Installing Duo integration to $CONFLUENCE..."

CONFLUENCE_ERROR="The directory ($CONFLUENCE) does not look like a Confluence installation. Use the -d option to specify where Confluence is installed."

if [ ! -d $CONFLUENCE ]; then
    echo "$CONFLUENCE_ERROR"
    exit 1
fi
if [ ! -e $CONFLUENCE/confluence/WEB-INF/lib ]; then
    echo "$CONFLUENCE_ERROR"
    exit 1
fi

# make sure we haven't already installed
if [ -e $CONFLUENCE/confluence/WEB-INF/lib/duo.jar ]; then
    echo "duo.jar already exists in $CONFLUENCE/confluence/WEB-INF/lib.  Move or remove this jar to continue."
    echo 'exiting'
    exit 1
fi

# make sure we haven't already installed
if find $CONFLUENCE/confluence/WEB-INF/lib/duo-client-*.jar > /dev/null; then
    echo "duo-client already exists in $CONFLUENCE/confluence/WEB-INF/lib.  Move or remove this jar to continue."
    echo 'exiting'
    exit 1
fi

# make sure we haven't already installed
if find $CONFLUENCE/confluence/WEB-INF/lib/duo-filter-*-SNAPSHOT.jar > /dev/null; then
    echo "duo-filter already exists in $CONFLUENCE/confluence/WEB-INF/lib.  Move or remove this jar to continue."
    echo 'exiting'
    exit 1
fi

# we don't actually write to web.xml, so just warn if it's already there
grep '<filter-name>duoauth</filter-name>' $CONFLUENCE/confluence/WEB-INF/web.xml >/dev/null
if [ $? = 0 ]; then
    echo "Warning: It looks like the Duo authenticator has already been added to Confluence's web.xml."
fi

echo "Copying in Duo integration files..."

# install the duo_java jar
cp etc/duo.jar $CONFLUENCE/confluence/WEB-INF/lib
if [ $? != 0 ]; then
    echo 'Could not copy duo.jar, please contact support@duosecurity.com'
    echo 'exiting'
    exit 1
fi

# install the duo_client_java jar
cp etc/duo-client-0.2.1.jar $CONFLUENCE/confluence/WEB-INF/lib
if [ $? != 0 ]; then
    echo 'Could not copy duo-client-0.2.1.jar, please contact support@duosecurity.com'
    echo 'exiting'
    exit 1
fi

# install the seraph filter jar
cp etc/duo-filter-1.3.6-SNAPSHOT.jar $CONFLUENCE/confluence/WEB-INF/lib
if [ $? != 0 ]; then
    echo 'Could not copy duo-filter-1.3.5-SNAPSHOT.jar, please contact support@duosecurity.com'
    echo 'exiting'
    exit 1
fi

echo "duo_confluence jars have been installed. Next steps, in order:"
echo "- Upload and install the plugin in etc/duo-twofactor-1.4.1-SNAPSHOT.jar "
echo "  using the Confluence web UI."
echo "- Edit web.xml, located at $CONFLUENCE/confluence/WEB-INF/web.xml."
echo "- Locate the filter:"
echo "    <filter>"
echo "        <filter-name>security</filter-name>"
echo "        <filter-class>com.atlassian.confluence.web.filter.ConfluenceSecurityFilter</filter-class>"
echo "    </filter>"
echo "- Add the following directly after the filter listed above:"
echo "    <filter>"
echo "        <filter-name>duoauth</filter-name>"
echo "        <filter-class>com.duosecurity.seraph.filter.DuoAuthFilter</filter-class>"
echo "        <init-param>"
echo "            <param-name>ikey</param-name>"
echo "            <param-value>$IKEY</param-value>"
echo "        </init-param>"
echo "        <init-param>"
echo "            <param-name>skey</param-name>"
echo "            <param-value>$SKEY</param-value>"
echo "        </init-param>"
echo "        <init-param>"
echo "            <param-name>akey</param-name>"
echo "            <param-value>$AKEY</param-value>"
echo "        </init-param>"
echo "        <init-param>"
echo "            <param-name>host</param-name>"
echo "            <param-value>$HOST</param-value>"
echo "        </init-param>"
echo "    </filter>"
echo "- Locate the filter-mapping:"
echo "    <filter-mapping>"
echo "        <filter-name>security</filter-name>"
echo "        <url-pattern>/*</url-pattern>"
echo "        <dispatcher>REQUEST</dispatcher>"
echo "        <dispatcher>FORWARD</dispatcher> <!-- we want security to be applied after urlrewrites, for example -->"
echo "    </filter-mapping>"
echo "- Add the following directly after the filter-mapping listed above:"
echo "    <filter-mapping>"
echo "        <filter-name>duoauth</filter-name>"
echo "        <url-pattern>/*</url-pattern>"
echo "        <dispatcher>FORWARD</dispatcher>"
echo "        <dispatcher>REQUEST</dispatcher>"
echo "    </filter-mapping>"
echo "- Restart Confluence."
