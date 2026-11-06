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

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Abstract base for reports that execute JCR SQL2 queries.
 *
 * @author toto
 */
public abstract class QueryReport extends BaseReport {
    private static Logger logger = LoggerFactory.getLogger(QueryReport.class);
    protected List<Map<String, String>> dataList;

    protected QueryReport(JCRSiteNode siteNode) {
        super(siteNode);
        this.dataList = new ArrayList<>();
    }

    /**
     * addItem
     * <p>add item to the iReport class,
     * custom implementation in each child class.</p>
     *
     * @param node {@link JCRNodeWrapper}
     * @throws RepositoryException
     */
    public abstract void addItem(JCRNodeWrapper node) throws RepositoryException;

    /**
     * getQueryResult
     * <p>The method returns the NodeIterator,
     * result of the query execution.</p>
     *
     * @param queryStr {@link String}
     * @param session {@link JCRSessionWrapper}
     * @param offset
     * @param limit
     * @return {@link NodeIterator}
     * @throws RepositoryException
     */
    protected NodeIterator getQueryResult(String queryStr, JCRSessionWrapper session, int offset, int limit) throws RepositoryException{
        // Getting the items  nodes.
        Query query = session.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
        query.setOffset(offset);
        query.setLimit(limit);
        return  query.execute().getNodes();
    }

    protected long getQueryResultCount(String queryStr, JCRSessionWrapper session) throws RepositoryException{
        // Getting the items  nodes.
        Query query = session.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
        return  query.execute().getNodes().getSize();
    }


    /**
     * fillReport
     * <p>The method is the in charge to fill the BaseReport object,
     * with the data obtained from the query.</p>
     *
     * @param session {@link JCRSessionWrapper}
     * @param strQuery {@link String}
     * @param offset
     * @param limit
     * @return {@link BaseReport}
     * @throws JSONException
     */
    protected void fillReport(JCRSessionWrapper session, String strQuery, int offset, int limit) throws JSONException {
        if(StringUtils.isNotEmpty(strQuery)) {
            try {
             /* filling the content nodes */
                NodeIterator iterator = getQueryResult(strQuery, session, offset, limit);
                while (iterator.hasNext()) {
                    JCRNodeWrapper nodeItem = (JCRNodeWrapper) iterator.next();
                    addItem(nodeItem);
                }
            } catch (RepositoryException rex) {
                logger.error("getAjaxFromQuery: problem executing the jcr:query[" + strQuery + "]", rex);
            }
        }
    }

    /**
     * getTotalCount
     *
     * @param session {@link JCRSessionWrapper}
     * @param strQuery {@link String}
     * @return {@link BaseReport}
     * @throws JSONException
     */
    protected long getTotalCount(JCRSessionWrapper session, String strQuery) throws JSONException {
        if(StringUtils.isNotEmpty(strQuery)) {
            try {
             /* filling the content nodes */
                return getQueryResultCount(strQuery, session);

            } catch (RepositoryException rex) {
                logger.error("getAjaxFromQuery: problem executing the jcr:query[" + strQuery + "]", rex);
            }
        }
        return 0;
    }

    /**
     * Builds a nodeMap pre-populated with the common node properties shared across
     * multiple report implementations (path, name, type, author, etc.).
     *
     * @param node the JCR node to inspect
     * @return a mutable map with common properties already set
     * @throws RepositoryException if a JCR property cannot be read
     */
    protected Map<String, String> buildBaseNodeMap(JCRNodeWrapper node) throws RepositoryException {
        Map<String, String> nodeMap = new HashMap<>();
        nodeMap.put("nodePath", node.getPath());
        nodeMap.put("nodeUrl", node.getUrl());
        nodeMap.put("nodeName", node.getName());
        nodeMap.put("nodeType", node.getPrimaryNodeTypeName());
        nodeMap.put("nodeTypeTechName", node.getPrimaryNodeTypeName().split(":")[1]);
        nodeMap.put("nodeTypeName", node.getPrimaryNodeType().getName());
        nodeMap.put("nodeTypePrefix", node.getPrimaryNodeType().getPrefix());
        nodeMap.put("nodeTypeAlias", node.getPrimaryNodeType().getAlias());
        nodeMap.put("nodeAuthor", node.getCreationUser());
        return nodeMap;
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

        for (Map<String, String> nodeMap : this.dataList) {
            jArray.put(new JSONObject(nodeMap));
        }
        jsonObject.put("siteName", siteNode.getName());
        jsonObject.put("siteDisplayableName", siteNode.getDisplayableName());
        jsonObject.put("items", jArray);
        return jsonObject;
    }




}
