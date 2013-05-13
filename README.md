# Overview

**duo_confluence** - Duo two-factor authentication filter for Confluence.

Adds Duo two-factor authentication to existing Seraph user authentication for
Confluence by redirecting the user to provide Duo credentials for requests
which require a logged-in user.

There are two parts to this project.  `duo_seraph_filter` is a Seraph
filter which redirects the user.  `duo_twofactor` is a Confluence plugin which
displays the Duo frame to the user to gather credentials.

This project has been tested with Confluence 4.2.1.

Documentation and installation instructions:
<https://www.duosecurity.com/docs/confluence>
