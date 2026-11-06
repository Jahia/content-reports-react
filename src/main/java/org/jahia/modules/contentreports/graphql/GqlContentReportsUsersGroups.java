package org.jahia.modules.contentreports.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.api.Constants;
import org.jahia.modules.contentreports.service.ReportUsersAndGroupsGenerator;
import org.jahia.modules.contentreports.service.ReportUsersAndGroupsService;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRContentUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.jahia.services.content.nodetypes.ExtendedNodeType;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.NodeTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.query.Query;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@GraphQLName("ContentReportsUsersGroups")
@GraphQLDescription("Users and groups report administration")
public class GqlContentReportsUsersGroups {

    private static final Logger LOGGER = LoggerFactory.getLogger(GqlContentReportsUsersGroups.class);

    @GraphQLField
    @GraphQLName("userProperties")
    @GraphQLDescription("Returns all property names defined on the jnt:user node type, sorted alphabetically")
    public List<String> getUserProperties() {
        try {
            final ExtendedNodeType userType = NodeTypeRegistry.getInstance().getNodeType("jnt:user");
            return Arrays.stream(userType.getPropertyDefinitions())
                    .map(ExtendedPropertyDefinition::getName)
                    .filter(name -> !name.startsWith("jcr:"))
                    .filter(name -> !name.startsWith("nt:"))
                    .filter(name -> !name.startsWith("rep:"))
                    .filter(name -> !name.equals("j:password"))
                    .filter(name -> !name.equals("*"))
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        } catch (NoSuchNodeTypeException e) {
            LOGGER.error("jnt:user node type not found", e);
            return Collections.emptyList();
        }
    }

    @GraphQLField
    @GraphQLName("isGenerating")
    @GraphQLDescription("Returns true if a users and groups report is currently being generated")
    public Boolean getIsGenerating() {
        final ReportUsersAndGroupsService service = BundleUtils.getOsgiService(ReportUsersAndGroupsService.class, null);
        return service != null && service.isGenerating();
    }

    @GraphQLField
    @GraphQLName("files")
    @GraphQLDescription("Lists generated users and groups report CSV files under the given JCR root path, sorted by creation date descending")
    public List<GqlContentReportsUsersGroupsReportFile> getFiles(
            @GraphQLName("csvRootPath") @GraphQLNonNull final String csvRootPath) {
        final String folderPath = ReportUsersAndGroupsGenerator.normalizeRootPath(csvRootPath) + "/" + ReportUsersAndGroupsGenerator.REPORT_FOLDER_NAME;
        try {
            final JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
            final List<GqlContentReportsUsersGroupsReportFile> files = new ArrayList<>();
            if (session.nodeExists(folderPath)) {
                final String queryStmt = String.format(
                        "SELECT * FROM [jnt:file] AS f WHERE ISDESCENDANTNODE(f, '%s')",
                        JCRContentUtils.sqlEncode(folderPath));
                final NodeIterator iterator = session.getWorkspace().getQueryManager()
                        .createQuery(queryStmt, Query.JCR_SQL2)
                        .execute()
                        .getNodes();

                while (iterator.hasNext()) {
                    final JCRNodeWrapper node = (JCRNodeWrapper) iterator.nextNode();
                    try {
                        final String createdAt = DateTimeFormatter.ISO_INSTANT.format(
                                node.getProperty("jcr:created").getDate().toInstant().atOffset(ZoneOffset.UTC));
                        files.add(new GqlContentReportsUsersGroupsReportFile(
                                node.getPath(),
                                "/files/" + Constants.EDIT_WORKSPACE + node.getPath(),
                                createdAt));
                    } catch (RepositoryException e) {
                        LOGGER.warn("Could not read metadata for users and groups report {}", node.getPath(), e);
                    }
                }
            }
            files.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            return files;
        } catch (RepositoryException e) {
            LOGGER.error("Error listing users and groups reports at {}", folderPath, e);
            return new ArrayList<>();
        }
    }

    @GraphQLName("ContentReportsUsersGroupsReportFile")
    @GraphQLDescription("A generated users and groups report CSV file")
    public static class GqlContentReportsUsersGroupsReportFile {

        private final String path;
        private final String downloadUrl;
        private final String createdAt;

        public GqlContentReportsUsersGroupsReportFile(String path, String downloadUrl, String createdAt) {
            this.path = path;
            this.downloadUrl = downloadUrl;
            this.createdAt = createdAt;
        }

        @GraphQLField
        @GraphQLName("path")
        @GraphQLDescription("JCR path of the report file")
        public String getPath() {
            return path;
        }

        @GraphQLField
        @GraphQLName("downloadUrl")
        @GraphQLDescription("URL to download the report file")
        public String getDownloadUrl() {
            return downloadUrl;
        }

        @GraphQLField
        @GraphQLName("createdAt")
        @GraphQLDescription("ISO-8601 creation timestamp")
        public String getCreatedAt() {
            return createdAt;
        }
    }
}
