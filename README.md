# Responsive Nodes Clean Up Tool

This tool cleans up the content structure that changed due to updates in the template editor,
when migrating from 6.2 to 6.3.

You can search for shadow nodes and either display or remove them. A shadow node is a responsive node
(node name = cq:responsive) which has a twin node in the corresponding page template structure (<template>/structure),
for which the twin node has an editable parent (editable=true).
Since 6.3, shadow nodes are not necessary any more and should be removed.

You can search for orphan nodes. An orphan node is a responsive node (node name = cq:responsive) that does not have a
twin node in the corresponding template structure (<template>/structure), but has an editable parent (editable=true).
Those nodes could be added to the template structure and removed from the page content.

To use the tool: in your browser, request a page with the 'responsiveNodesCleanup' selector and define the action to perform
by adding specific URL parameters.

E.g.: to search for shadow nodes below /content and display a detailed view, request:
http://localhost:4502/content/we-retail/us/en/products/men/shirts.responsiveNodesCleanup.html?type=shadow&viewType=detailed&searchPaths=/content

Following URL parameters are available:

'type': defines the type of the responsive nodes. Possible values:
- shadow: searches for shadow nodes. This is the default value when 'type' is not specified
- shadowInTemplateInitial: searches for shadow responsive nodes that are part of a template initial page
- shadowInContent: searches for shadow responsive nodes that are below /content
- orphan: searches for ophan nodes

'viewType': defines how to display the nodes. Possible values:
- simple: displays the list of nodes. This is the default value when 'viewType' is not specified
- detailed: displays the list of nodes with their configurations
- noView: displays a blank page

'searchPaths' parameter. Defines the paths to search in (e.g.: /conf):
- multiple values are possible. E.g. &searchPaths=/content/sites1&searchPaths=/content/sites2 will search below
/content/sites1 and /content/sites2
- the root path ('/') is not allowed and will be skipped
- the default paths are: /content and /conf

'operation': defines which operation to perform. Possible values:
- view: displays the nodes. This is the default value when 'operation' is not specified
- remove: removes the nodes
