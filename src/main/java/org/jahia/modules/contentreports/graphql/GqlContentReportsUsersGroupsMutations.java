package org.jahia.modules.contentreports.graphql;

import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLNonNull;
import org.jahia.modules.contentreports.service.ReportUsersAndGroupsGenerator;
import org.jahia.modules.contentreports.service.ReportUsersAndGroupsService;
import org.jahia.modules.graphql.provider.dxm.BaseGqlClientException;
import org.jahia.osgi.BundleUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRSessionFactory;
import org.jahia.services.content.JCRSessionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@GraphQLName("ContentReportsUsersGroupsMutations")
@GraphQLDescription("Users and groups report mutations")
public class GqlContentReportsUsersGroupsMutations {

    private static final Logger LOGGER = LoggerFactory.getLogger(GqlContentReportsUsersGroupsMutations.class);
    private static final String PERMISSION_DENIED = "Permission denied";
    private static final List<String> DEFAULT_USER_PROPERTIES = Arrays.asList("j:firstName", "j:lastName");

    @GraphQLField
    @GraphQLName("generate")
    @GraphQLDescription("Generates a CSV report of all users and their groups and stores it in JCR")
    public Boolean generate(
            @GraphQLName("csvRootPath") @GraphQLNonNull final String csvRootPath,
            @GraphQLName("userPropertiesToExport") final List<String> userPropertiesToExport) throws RepositoryException {
        final ReportUsersAndGroupsService service = BundleUtils.getOsgiService(ReportUsersAndGroupsService.class, null);
        if (service == null) {
            LOGGER.error("ReportUsersAndGroupsService is not available");
            return Boolean.FALSE;
        }
        // The target is a folder the user can add files to
        final String rootPath = ReportUsersAndGroupsGenerator.normalizeRootPath(csvRootPath);
        final JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
        if (!session.nodeExists(rootPath) || !session.getNode(rootPath).isNodeType("jnt:folder")) {
            throw new BaseGqlClientException("Target folder not found or not a folder", null, Collections.emptyMap());
        }
        if (!session.getNode(rootPath).hasPermission("jcr:addChildNodes")) {
            throw new BaseGqlClientException(PERMISSION_DENIED, null, Collections.emptyMap());
        }
        LOGGER.info("Users and groups report requested by {} into {}", session.getUser().getUsername(), rootPath);
        final List<String> propertiesToExport = userPropertiesToExport != null ? userPropertiesToExport : DEFAULT_USER_PROPERTIES;
        return service.generate(rootPath, propertiesToExport);
    }

    @GraphQLField
    @GraphQLName("deleteReport")
    @GraphQLDescription("Deletes a generated users and groups report file from JCR by its JCR path")
    public Boolean deleteReport(@GraphQLName("path") @GraphQLNonNull final String path) throws RepositoryException {
        if (!ReportUsersAndGroupsGenerator.isReportFilePath(path)) {
            LOGGER.warn("Refusing to delete a non users-and-groups report path: {}", path);
            return Boolean.FALSE;
        }
        final JCRSessionWrapper session = JCRSessionFactory.getInstance().getCurrentUserSession();
        if (!session.nodeExists(path)) {
            return Boolean.TRUE;
        }
        final JCRNodeWrapper node = session.getNode(path);
        if (!node.isNodeType("jnt:file")) {
            throw new BaseGqlClientException("Not a report file", null, Collections.emptyMap());
        }
        if (!node.hasPermission("jcr:removeNode")) {
            throw new BaseGqlClientException(PERMISSION_DENIED, null, Collections.emptyMap());
        }
        node.remove();
        session.save();
        LOGGER.info("Users and groups report {} deleted by {}", path, session.getUser().getUsername());
        return Boolean.TRUE;
    }
}
