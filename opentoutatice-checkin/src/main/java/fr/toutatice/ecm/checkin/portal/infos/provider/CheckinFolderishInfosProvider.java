/**
 *
 */
package fr.toutatice.ecm.checkin.portal.infos.provider;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.elasticsearch.ElasticSearchConstants;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.checkin.helper.DocumentCheckinHelper;
import fr.toutatice.ecm.checkin.helper.DocumentHelper;
import fr.toutatice.ecm.platform.core.services.infos.provider.DocumentInformationsProvider;


/**
 * Checkin folderish infos provider
 *
 * @author David Chevrier
 * @see DocumentInformationsProvider
 */
public class CheckinFolderishInfosProvider implements DocumentInformationsProvider {

    /** ElasticSearch's server enabled indicator. */
    private static boolean isEsEnabled;

    /** ElasticSearch service. */
    private final ElasticSearchService esService;

    /**
     * Constructor.
     */
    public CheckinFolderishInfosProvider() {
        super();
        // Is ElasticSearch activated
        isEsEnabled = Framework.isBooleanPropertyTrue(ElasticSearchConstants.ES_ENABLED_PROPERTY);
        
        // ElasticSearch service
        this.esService = Framework.getService(ElasticSearchService.class);
    }


    /**
     * {@inheritDoc}
     */
    // FIXME: modify fetchInfos to take ES into account (new param or abstract class)
    @Override
    public Map<String, Object> fetchInfos(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {
        // Infos
        Map<String, Object> infos = new HashMap<String, Object>(1);

        if (currentDocument.isFolder()) {
            // Draft folder path
            String userWsPath = DocumentCheckinHelper.getInstance().getDraftsFolderPath(coreSession, currentDocument);
            // Checkout parent identifier
            String checkoutParentId = DocumentHelper.getId(currentDocument);
            // Query
            StringBuilder query = new StringBuilder();
            query.append("SELECT ecm:uuid FROM Document ");
            query.append("WHERE ecm:path STARTSWITH '").append(userWsPath).append("' ");
            query.append("AND ecm:mixinType = 'OttcDraft' ");
            query.append("AND ottcDft:checkoutParentId = '").append(checkoutParentId).append("' ");
            query.append("AND ecm:currentLifeCycleState <> 'deleted' ");

            long draftCount = 0;
            if (isEsEnabled) {
                // ES query
                NxQueryBuilder queryBuilder = new NxQueryBuilder(coreSession).nxql(query.toString());

                EsResult draftsIds = this.esService.queryAndAggregate(queryBuilder);

                if ((draftsIds != null) && (draftsIds.getRows() != null)) {
                    // Draft count
                    draftCount = draftsIds.getRows().size();
                }
            } else {
                // VCS query
                IterableQueryResult draftsRows = coreSession.queryAndFetch(query.toString(), NXQL.NXQL, new Object[0]);

                if (draftsRows != null) {
                    // Draft count
                    draftCount = draftsRows.size();
                }
            }

            // Store infos
            infos.put("draftCount", draftCount);
        }

        return infos;
    }

}
