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

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.Template;
import com.day.cq.wcm.api.TemplateManager;

/**
 * The responsive item Class
 */
public class ResponsiveItem {

    private final static String CONTENT_ROOT = "/content";
    private final static String NN_RESPONSIVE = "cq:responsive";
    private final static String NN_TEMPLATE_STRUCTURE = "structure";
    private final static String NN_TEMPLATE_INITIAL = "initial";
    private final static String PN_EDITABLE = "editable";

    private Resource resource;
    private ResourceResolver resolver;
    private PageManager pageManager;
    private TemplateManager templateManager;


    public ResponsiveItem(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("The resource does not exist.");
        }
        if (!isResponsive(resource)) {
            throw new IllegalArgumentException("The resource at " + resource.getPath() + " is not responsive.");
        }
        this.resource = resource;
        this.resolver = resource.getResourceResolver();
        this.pageManager = resolver.adaptTo(PageManager.class);
        this.templateManager = resolver.adaptTo(TemplateManager.class);
    }

    /**
     * Returns the underlying resource.
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * Returns the underlying resource path.
     */
    public String getPath() {
        return resource.getPath();
    }

    /**
     * Returns the corresponding editable resource in the template structure.
     */
    public Resource getTemplateStructureEditable() {
        Template template = getTemplate();
        if (template != null) {
            String templatePath = template.getPath();
            String contentParentRelPath = getRelPath(resource.getParent());
            if (StringUtils.isNotEmpty(templatePath) && StringUtils.isNotEmpty(contentParentRelPath)) {
                Resource res = resolver.getResource(templatePath + "/" + NN_TEMPLATE_STRUCTURE + "/" + contentParentRelPath);
                if (res != null) {
                    ValueMap vm = res.getValueMap();
                    boolean isEditable = vm.get(PN_EDITABLE, false);
                    if (isEditable) {
                        return res;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Checks if the responsive item is a 'shadow' node (see docs).
     */
    public boolean isShadow() {
        return !isInTemplateStructure() && hasStructureTemplateEditable();
    }

    /**
     * Checks if the responsive item is a 'shadow' node in the template initial structure.
     */
    public boolean isShadowInTemplateInitial() {
        return isShadow() && isInTemplateInitial();
    }

    /**
     * Checks if the responsive item is a 'shadow' node below /content.
     */
    public boolean isShadowInContent() {
        return isShadow() && isInContent();
    }

    /**
     * Checks if the responsive item is an 'orphan' node.
     */
    public boolean isOrphan() {
        return isInContent() && hasStructureTemplateEditable() && !hasStructureTemplateEditableResponsive();
    }



        /* ============================= private stuff ============================= */


    private boolean hasStructureTemplateEditable() {
        return getTemplateStructureEditable() != null;
    }

    private boolean isInContent() {
        return resource.getPath().startsWith(CONTENT_ROOT);
    }

    private boolean isInTemplateInitial() {
        Template template = templateManager.getContainingTemplate(resource);
        return template != null && resource.getPath().startsWith(template.getPath() + "/" + NN_TEMPLATE_INITIAL);
    }

    private boolean isInTemplateStructure() {
        Template template = templateManager.getContainingTemplate(resource);
        return template != null && resource.getPath().startsWith(template.getPath() + "/" + NN_TEMPLATE_STRUCTURE);
    }

    private boolean hasStructureTemplateEditableResponsive() {
        return getTemplateStructureEditable() != null && getTemplateStructureEditable().getChild(NN_RESPONSIVE) != null;
    }

    private boolean isResponsive(Resource resource) {
        return resource != null && NN_RESPONSIVE.equals(resource.getName());
    }

    // Returns the relative path of the resource, compared to the containing page
    private String getRelPath(Resource resource) {
        Page page = pageManager.getContainingPage(resource);
        if (resource != null && page != null) {
            return resource.getPath().substring(page.getPath().length()+1);
        }
        return null;
    }

    private Template getTemplate() {
        return pageManager.getContainingPage(resource).getTemplate();
    }

}
