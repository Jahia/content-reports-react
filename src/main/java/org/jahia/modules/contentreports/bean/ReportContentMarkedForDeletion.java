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
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.decorator.JCRSiteNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.util.*;
import java.util.stream.Stream;

/**
 * The ReportContentMarkedForDeletion Class.
 * <p>
 * Created by Jonathan Sinovassin-naik.
 */
public class ReportContentMarkedForDeletion extends QueryReport {
    private static Logger logger = LoggerFactory.getLogger(ReportLockedContent.class);
    private long totalContent;
    private int sortCol;
    private String searchPath;
    private String order;
    private String[] resultFields = { "j:nodename", "jcr:primaryType", "j:nodename" };
    private JCRSessionWrapper sessionWrapper;
    private SearchContentType reportType;

    /**
     * Instantiates a new Report content marked for deletion.
     *
     * @param siteNode the site node {@link JCRSiteNode}
     */
    public ReportContentMarkedForDeletion(JCRSiteNode siteNode, String searchPath, SearchContentType reportType, int sortCol,
            String order) {
        super(siteNode);
        this.sortCol = sortCol;
        this.order = order;
        this.searchPath = searchPath;
        this.reportType = reportType;
    }

    @Override public void execute(JCRSessionWrapper session, int offset, int limit) throws RepositoryException, JSONException {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM").append(reportType.equals(BaseReport.SearchContentType.PAGE) ? "[jnt:page] " : "[jnt:content] ")
                .append("AS item ").append("WHERE item.[jcr:mixinTypes] = 'jmix:markedForDeletionRoot' ")
                .append("and ISDESCENDANTNODE(item,['").append(searchPath).append("']) ").append(" order by item.[")
                .append(resultFields[sortCol]).append("] ").append(order);

        String query = queryBuilder.toString();
        sessionWrapper = session;
        fillReport(session, query, offset, limit);
        totalContent = getTotalCount(session, query);
    }

    /**
     * addItem
     *
     * @param node {@link JCRNodeWrapper}
     * @throws RepositoryException
     */
    public void addItem(JCRNodeWrapper node) throws RepositoryException {

        JCRNodeWrapper itemParentPage = node;
        if (!node.isNodeType("jnt:page")) {
            itemParentPage = JCRContentUtils.getParentOfType(node, "jnt:page");
        }
        Map<String, String> nodeMap = new HashedMap();
        nodeMap.put("nodePath", node.getPath());
        nodeMap.put("nodeName", node.getName());
        nodeMap.put("nodeType", node.getPrimaryNodeType().getLabel(this.defaultLocale));
        nodeMap.put("nodeTypeName", node.getPrimaryNodeType().getName());
        if (itemParentPage != null) {
            nodeMap.put("nodeUsedInPagePath", itemParentPage.getPath());
            nodeMap.put("nodePresentOnPage", "true");
        } else {
            nodeMap.put("nodeUsedInPagePath", node.getParent().getPath());
            nodeMap.put("nodePresentOnPage", "false");
        }
        nodeMap.put("subNodesMarkedForDeletion", this.countSubNodes(node).toString());
        nodeMap.put("nodeDisplayableName", WordUtils.abbreviate(node.getDisplayableName(), 90, 130, "..."));
        nodeMap.put("nodeTitle", (node.hasI18N(this.locale) && node.getI18N(this.defaultLocale).hasProperty("jcr:title")) ?
                node.getI18N(this.defaultLocale).getProperty("jcr:title").getString() :
                "");
        nodeMap.put("displayTitle", StringUtils.isNotEmpty(nodeMap.get("nodeTitle")) ? nodeMap.get("nodeTitle") : nodeMap.get("nodeName"));
        nodeMap.put("publishStatus", getPublicationStatusOfANode(node));
        this.dataList.add(nodeMap);
    }

    @Override public JSONObject getJson() throws JSONException, RepositoryException {

        JSONArray jArray = new JSONArray();

        for (Map<String, String> nodeMap : this.dataList) {
            JSONArray item = new JSONArray();
            item.put(nodeMap.get("nodeDisplayableName"));
            item.put(nodeMap.get("nodeType"));
            item.put(nodeMap.get("nodePath"));
            item.put(nodeMap.get("nodeUsedInPagePath"));
            item.put(nodeMap.get("subNodesMarkedForDeletion"));
            item.put(nodeMap.get("publishStatus"));
            item.put(Boolean.valueOf(nodeMap.get("nodePresentOnPage")) ? "nodePresentOnPage" : "nodeNotPresentOnPage");
            jArray.put(item);
        }
        return buildJsonResponse(totalContent, jArray);
    }

    private Long countSubNodes(JCRNodeWrapper node) {
        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder
                .append("SELECT count AS [rep:count(skipChecks=1)] FROM [jmix:markedForDeletion] AS item where ISDESCENDANTNODE(item,['")
                .append(node.getPath()).append("'])");
        try {
            if (hasToCountSubNodes(node)) {
                return sessionWrapper.getWorkspace().getQueryManager().createQuery(queryBuilder.toString(), Query.JCR_SQL2).execute()
                        .getRows().nextRow().getValue("count").getLong();
            }
        } catch (RepositoryException rex) {
            logger.error("countSubNodes: problem executing the jcr:query[" + queryBuilder.toString() + "]", rex);
        }
        return 0L;
    }

    private Boolean hasToCountSubNodes(JCRNodeWrapper node){
        return Stream.of("jnt:page", "jnt:navMenuText", "jnt:folder", "jnt:contentFolder").anyMatch(nodeType -> {
            try {
                return node.isNodeType(nodeType);
            } catch (RepositoryException e) {
                logger.error("countSubNodes: Error while checking node type of" + node.getName(), e);
            }
            return false;
        });
    }
    private String getPublicationStatusOfANode(JCRNodeWrapper node) {
        String publishStatus;
        try {
            if (node.hasProperty("j:published") && node.getProperty("j:published").getValue().getBoolean()) {
                publishStatus = label("cgnt_contentReports.status.published");
            } else {
                publishStatus = label("cgnt_contentReports.status.notPublished");
            }
        } catch (RepositoryException e) {
            logger.error("getPublicationStatusOfANode: problem while getting the publish status for the node " + node.getName(), e);
            publishStatus = "";
        }
        return publishStatus;
    }
}
