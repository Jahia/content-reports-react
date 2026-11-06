/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.contentreports.bean;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.jahia.api.Constants;
import org.jahia.exceptions.JahiaException;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.jcr.RepositoryException;
import java.util.*;

/**
 * ReportByStatus Class.
 *
 * Created by Juan Carlos Rodas.
 */
public class ReportByStatus extends BaseReport {

    private final String PROPERTY_NAME = "name";
    private final String PROPERTY_LIST = "list";
    private final String PROPERTY_ITEMS = "items";
    private Map<String, Map<String, Object>> dataMap;

    /**
     * The class constructor.
     */
    public ReportByStatus(JCRSiteNode siteNode, String path) {
        super(siteNode);
        this.dataMap = new HashMap<>();
    }

    @Override
    public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException, JahiaException {
//        String strQuery = "SELECT * FROM [jmix:editorialContent] AS item WHERE ISDESCENDANTNODE(item,['" + searchPath + "'])";
//        return fillIreport(session, strQuery, new ReportByStatus(), null).getJson();

    }

    /**
     * addItem
     *
     * @param node {@link JCRNodeWrapper}
     * @throws RepositoryException
     */
    public void addItem(JCRNodeWrapper node) throws RepositoryException {
        if (node.hasProperty(Constants.WORKINPROGRESS))
            addItemDataToList("WIP", "cgnt_contentReports.status.workInProgress", "work in progress", node);
        else if (node.getLastPublishedAsDate() != null && node.getLastPublishedAsDate().before(node.getLastModifiedAsDate()))
            addItemDataToList("MNP", "cgnt_contentReports.status.modifiedNotPublished", "modified not published", node);
        else if (!node.hasProperty(Constants.PUBLISHED) &&  node.getLastPublishedAsDate() != null)
            addItemDataToList("NVP", "cgnt_contentReports.status.neverPublished", "never published", node);
        else if (node.getLastPublishedAsDate() != null && node.hasProperty(Constants.PUBLISHED) && !node.getProperty(Constants.PUBLISHED).getBoolean())
            addItemDataToList("UNP", "cgnt_contentReports.status.unpublished", "unpublished", node);
    }

    /**
     * getJson
     *
     * @return {@link JSONObject}
     * @throws JSONException
     * @throws RepositoryException
     */
    public JSONObject getJson() throws JSONException, RepositoryException {
        JSONObject jsonObject = new JSONObject();
        JSONArray jArray = new JSONArray();
        JSONObject jsonObjectItem;

        JSONArray jsonArrayItemDetail;
        JSONObject jsonObjectSubItemDetail;

        for (String key : dataMap.keySet()) {
            jsonObjectItem = new JSONObject();
            jsonObjectItem.put(PROPERTY_NAME, dataMap.get(key).get(PROPERTY_NAME));

            /* part of items detail */
            jsonArrayItemDetail = new JSONArray();
            for (JCRNodeWrapper nodeItem : (ArrayList<JCRNodeWrapper>) dataMap.get(key).get(PROPERTY_LIST)) {
                jsonObjectSubItemDetail = new JSONObject();
                jsonObjectSubItemDetail.put("path", nodeItem.getPath());
                jsonObjectSubItemDetail.put("canonicalPath", nodeItem.getCanonicalPath());
                jsonObjectSubItemDetail.put("identifier", nodeItem.getIdentifier());
                jsonObjectSubItemDetail.put("title", nodeItem.hasProperty("jcr:title") ? nodeItem.getPropertyAsString("jcr:title") : "");
                jsonObjectSubItemDetail.put("displayableName", WordUtils.abbreviate(nodeItem.getDisplayableName(),90,130,"..."));
                jsonObjectSubItemDetail.put("name", nodeItem.getName());
                jsonObjectSubItemDetail.put("displayTitle", nodeItem.hasProperty("jcr:title") ? nodeItem.getPropertyAsString("jcr:title") : nodeItem.getDisplayableName());
                jsonObjectSubItemDetail.put("primaryNodeType", nodeItem.getPrimaryNodeType());
                jsonObjectSubItemDetail.put("type", nodeItem.getPrimaryNodeTypeName().split(":")[1]);
                jsonObjectSubItemDetail.put("path", nodeItem.getPath());
                jsonObjectSubItemDetail.put("identifier", nodeItem.getIdentifier());
                jsonObjectSubItemDetail.put("created", dateFormat.format(nodeItem.getCreationDateAsDate()));
                jsonObjectSubItemDetail.put("lastModified", dateFormat.format(nodeItem.getLastModifiedAsDate()));
                jsonObjectSubItemDetail.put("lastModifiedBy", nodeItem.getModificationUser());
                jsonObjectSubItemDetail.put("published", nodeItem.hasProperty("j:published") ? true : false);
                jsonObjectSubItemDetail.put("lock", nodeItem.isLocked() ? true : false);
                jsonObjectSubItemDetail.put("language", StringUtils.isNotEmpty(nodeItem.getLanguage()) ? nodeItem.getLanguage() : "");
                jsonArrayItemDetail.put(jsonObjectSubItemDetail);
            }
            jsonObjectItem.put(PROPERTY_ITEMS, jsonArrayItemDetail);
            jArray.put(jsonObjectItem);
        }

        jsonObject.put("statusItems", jArray);
        return jsonObject;
    }

    /**
     * addItemDataToList
     * <p>method add a node to a list, divided by status.</p>
     *
     * @param statusName {@link String}
     * @param statusNameKey {@link String}
     * @param defaultNameValue {@link String}
     * @param node {@link JCRNodeWrapper}
     */
    private void addItemDataToList(String statusName, String statusNameKey, String defaultNameValue, JCRNodeWrapper node){
        if(!dataMap.containsKey(statusName))
            dataMap.put(statusName, new HashedMap());

        if(!dataMap.get(statusName).containsKey(PROPERTY_NAME))
            dataMap.get(statusName).put(PROPERTY_NAME,  label(statusNameKey));

        if(!dataMap.get(statusName).containsKey(PROPERTY_LIST))
            dataMap.get(statusName).put(PROPERTY_LIST, new ArrayList<JCRNodeWrapper>());

        ((ArrayList)dataMap.get(statusName).get(PROPERTY_LIST)).add(node);
    }

    /**
     * getDataMap
     *
     * @return {@link Map}
     */
    public Map<String, Map<String, Object>> getDataMap() {
        return dataMap;
    }

}
