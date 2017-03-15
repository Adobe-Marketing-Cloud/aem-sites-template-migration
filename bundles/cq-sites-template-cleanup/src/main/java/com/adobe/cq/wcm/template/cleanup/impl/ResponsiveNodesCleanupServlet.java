/*************************************************************************
 *
 * ADOBE CONFIDENTIAL
 * __________________
 *
 *  Copyright 2017 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 **************************************************************************/
package com.adobe.cq.wcm.template.cleanup.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.jcr.query.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

/**
 * Servlet that cleans up unneeded cq:responsive nodes due to updates in the template editor
 */
@SuppressWarnings("serial")
@Component
@Service
@Properties(value={
        @Property(name = "service.description", value = "Cleans up cq:responsive nodes."),
        @Property(name = "sling.servlet.resourceTypes", value = "sling/servlet/default"),
        @Property(name = "sling.servlet.selectors", value = { "responsiveNodesCleanup" }),
        @Property(name = "sling.servlet.extensions", value = {"html"}),
        @Property(name = "sling.servlet.methods", value = "GET")
})
public class ResponsiveNodesCleanupServlet extends SlingAllMethodsServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponsiveNodesCleanupServlet.class);

    private final static String DOCS_URL = "https://github.com/Adobe-Marketing-Cloud/aem-sites-template-migration";

    private final static String JCR_ROOT = "/jcr:root";
    private final static String NT_BASE = "nt:base";
    private final static String CONTENT_ROOT = "/content";
    private final static String CONF_ROOT = "/conf";
    private final static String NN_RESPONSIVE = "cq:responsive";
    private final static String PN_WIDTH = "width";

    private final static String PARAM_TYPE = "type";
    private final static String PARAM_VIEW_TYPE = "viewType";
    private final static String PARAM_OPERATION = "operation";
    private final static String PARAM_SEARCH_PATHS = "searchPaths";

    private final static String PARAM_VALUE_SHADOW = "shadow";
    private final static String PARAM_VALUE_SHADOW_IN_TEMPLATE_INITIAL = "shadowInTemplateInitial";
    private final static String PARAM_VALUE_SHADOW_IN_CONTENT = "shadowInContent";
    private final static String PARAM_VALUE_ORPHAN = "orphan";

    private final static String PARAM_VALUE_NO_VIEW = "noView";
    private final static String PARAM_VALUE_SIMPLE = "simple";
    private final static String PARAM_VALUE_DETAILED = "detailed";

    private final static String PARAM_VALUE_VIEW = "view";
    private final static String PARAM_VALUE_REMOVE = "remove";

    private SlingHttpServletResponse response;

    protected void doGet(SlingHttpServletRequest req, SlingHttpServletResponse resp) throws ServletException, IOException {

        this.response = resp;
        response.setContentType("text/html");
        ResourceResolver resolver = req.getResourceResolver();

        String responsiveType = req.getParameter(PARAM_TYPE);
        String viewType = req.getParameter(PARAM_VIEW_TYPE);
        String operation = req.getParameter(PARAM_OPERATION);
        RequestParameter[] searchPathsParams = req.getRequestParameters(PARAM_SEARCH_PATHS);
        List<String> searchPaths = new ArrayList<String>();

        if (StringUtils.isEmpty(responsiveType)) {
            responsiveType = PARAM_VALUE_SHADOW;
        }
        if (StringUtils.isEmpty(viewType)) {
            viewType = PARAM_VALUE_SIMPLE;
        }
        if (StringUtils.isEmpty(operation)) {
            operation = PARAM_VALUE_VIEW;
        }
        if (searchPathsParams == null || searchPathsParams.length == 0) {
            searchPaths.add(CONTENT_ROOT);
            searchPaths.add(CONF_ROOT);
        } else {
            for (RequestParameter searchPath : searchPathsParams) {
                searchPaths.add(searchPath.getString());
            }
        }

        List<ResponsiveItem> responsiveItems = findResponsiveItems(resolver, responsiveType, searchPaths);
        displayIntro(responsiveType, searchPaths, viewType, operation);
        if (PARAM_VALUE_VIEW.equals(operation)) {
            displayItems(responsiveItems, viewType);
        } else if (PARAM_VALUE_REMOVE.equals(operation)) {
            displayItems(responsiveItems, viewType);
            if (responsiveItems != null && responsiveItems.size() > 1) {
                displayLine("<br/>");
                displayLine("Deleting the responsive nodes...");
                removeItems(resolver, responsiveItems);
                displayLine("The responsive nodes have been deleted.");
            }
        } else {
            displayLine("The operation '" + operation + "' does not exist.");
        }

    }

    /**
     * Searches the repo to get responsive nodes according to the searched type and searched paths.
     */
    private List<ResponsiveItem> findResponsiveItems(ResourceResolver resolver, String responsiveType, List<String> searchPaths) {
        List<ResponsiveItem> responsiveItems = new ArrayList<ResponsiveItem>();
        for (String searchPath : searchPaths) {
            if (StringUtils.isNotEmpty(searchPath) && !"/".equals(searchPath) && resolver.getResource(searchPath) != null) {
                String query = JCR_ROOT + searchPath + "//element(" + NN_RESPONSIVE + "," + NT_BASE + ")";
                Iterator<Resource> it = resolver.findResources(query, Query.XPATH);
                while (it.hasNext()) {
                    Resource res = it.next();
                    ResponsiveItem responsiveItem = new ResponsiveItem(res);
                    if (itemMatchesType(responsiveItem, responsiveType)) {
                        responsiveItems.add(responsiveItem);
                    }
                }
            }
        }
        return responsiveItems;
    }

    /**
     * Checks if the responsive item matches the given type.
     */
    private boolean itemMatchesType(ResponsiveItem item, String type) {
        if (item != null) {
            if (PARAM_VALUE_SHADOW.equals(type)) {
                return item.isShadow();
            }
            if (PARAM_VALUE_SHADOW_IN_TEMPLATE_INITIAL.equals(type)) {
                return item.isShadowInTemplateInitial();
            }
            if (PARAM_VALUE_SHADOW_IN_CONTENT.equals(type)) {
                return item.isShadowInContent();
            }
            if (PARAM_VALUE_ORPHAN.equals(type)) {
                return item.isOrphan();
            }
        }
        return false;
    }

    /**
     * Removes the responsive items.
     */
    private void removeItems(ResourceResolver resolver, List<ResponsiveItem> responsiveItems) {
        for (ResponsiveItem item: responsiveItems) {
            try {
                resolver.delete(item.getResource());
                resolver.commit();
            } catch (PersistenceException e) {
                LOGGER.error("Error deleting the resource at {}", item.getResource().getPath());
            }
        }
    }

    /**
     * Displays the criteria used to search.
     */
    private void displayIntro(String responsiveType, List<String> searchPaths,
                              String viewType, String operation) throws IOException {

        StringBuilder searchPathsString = new StringBuilder();
        int idx = 1;
        for (String path : searchPaths) {
            searchPathsString.append(path);
            if (idx < searchPaths.size()) {
                searchPathsString.append(", ");
            }
            idx++;
        }
        displayLine("<h2>Responsive Nodes Clean Up Tool</h2>");
        displayLine("Please refer to the <a href='" + DOCS_URL + "'>documentation</a>.");
        displayLine("<br/>");
        displayLine("Responsive nodes meeting the following criteria:");
        displayLine("- type: " + responsiveType);
        displayLine("- viewType: " + viewType);
        displayLine("- searchPaths: " + searchPathsString.toString());
        displayLine("- operation: " + operation);
        displayLine("<br/>");
    }

    /**
     * Displays the responsive items.
     */
    private void displayItems(List<ResponsiveItem> responsiveItems, String viewType) throws IOException{

        boolean noView = PARAM_VALUE_NO_VIEW.equals(viewType);
        boolean simpleView = PARAM_VALUE_SIMPLE.equals(viewType);
        boolean detailedView = PARAM_VALUE_DETAILED.equals(viewType);

        if (noView) {
            return;
        }

        if (responsiveItems.isEmpty()) {
            displayLine("-> this category is empty.");
        }
        for (ResponsiveItem item: responsiveItems) {
            if (simpleView) {
                displayLine(item.getPath());
            } else if (detailedView) {
                displayLine("--------------------------");
                displayLine("responsive node:");
                displayLine(item.getPath());
                displayResponsiveConfig(item.getResource());

                displayLine("<br/>");
                displayLine("template structure node:");
                Resource templateStructureEditable = item.getTemplateStructureEditable();
                if (templateStructureEditable != null) {
                    displayLine(templateStructureEditable.getPath());
                    Resource templateStructureResponsive = templateStructureEditable.getChild(NN_RESPONSIVE);
                    if (templateStructureResponsive != null) {
                        displayResponsiveConfig(templateStructureResponsive);
                    } else {
                        displayLine("<br/>");
                        displayLine("no cq:responsive node");
                    }
                }
            }
        }
    }

    /**
     * Displays the child nodes and their 'width' property.
     */
    private void displayResponsiveConfig(Resource resource) throws IOException{
        Iterator<Resource> children = resource.listChildren();
        while (children.hasNext()) {
            Resource child = children.next();
            String name = child.getName();
            displayLine("<div style='text-indent: 20px'>+ " + name + "</div>");
            ValueMap vm = child.getValueMap();
            String width = vm.get(PN_WIDTH, "");
            displayLine("<div style='text-indent: 40px'>- width: " + width + "</div>");
        }
    }

    /**
     * Displays the string within a <div> element.
     */
    private void displayLine(String line) throws IOException {
        response.getWriter().println("<div>" + line + "</div>");
    }

}
