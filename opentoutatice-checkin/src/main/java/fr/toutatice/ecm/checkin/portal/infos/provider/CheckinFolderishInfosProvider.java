/**
 *
 */
package fr.toutatice.ecm.checkin.portal.infos.provider;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.NuxeoException;
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
    
    /** Logger. */
    private static final Log log = LogFactory.getLog(CheckinFolderishInfosProvider.class);

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
    // FIXME: modify fetchInfos to take ES into account (new param or abstract class)?
    @Override
    public Map<String, Object> fetchInfos(CoreSession coreSession, DocumentModel currentDocument) throws NuxeoException {
        // For Trace logs
        long begin = System.currentTimeMillis();
        
        // Infos
        Map<String, Object> infos = new HashMap<String, Object>(1);

        if (currentDocument.isFolder()) {
            // For Trace logs
            long b1 = System.currentTimeMillis();
            
            // Draft folder id
            String draftsFolderId = DocumentCheckinHelper.getInstance().getDraftsFolderId(coreSession, currentDocument);
            
            if(log.isTraceEnabled()){
                long e1 = System.currentTimeMillis();
                log.trace(" [getDraftsFolderPath]: " + String.valueOf(e1 - b1 + " ms"));
            }
            
            // Checkout parent identifier
            String checkoutParentId = DocumentHelper.getId(currentDocument);
            // Query
            StringBuilder query = new StringBuilder();
            query.append("SELECT ecm:uuid FROM Document ");
            query.append("WHERE ecm:parentId = '").append(draftsFolderId).append("' ");
            query.append("AND ecm:currentLifeCycleState <> 'deleted' ");
            query.append("AND ottcDft:checkoutParentId = '").append(checkoutParentId).append("' ");
            
            // For Trace logs
            long b2 = System.currentTimeMillis();
            
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
                IterableQueryResult draftsRows = null;

                try {
                    draftsRows = coreSession.queryAndFetch(query.toString(), NXQL.NXQL, new Object[0]);

                    if (draftsRows != null) {
                        // Draft count
                        draftCount = draftsRows.size();
                    }
                } finally {
                    if (draftsRows != null) {
                        draftsRows.close();
                    }
                }
            }
            
            if(log.isTraceEnabled()){
                long e2 = System.currentTimeMillis();
                log.trace(" [Drafts count]: " + String.valueOf(e2 - b2 + " ms"));
            }

            // Store infos
            infos.put("draftCount", draftCount);
        
    }
        
        if(log.isTraceEnabled()){
            long end = System.currentTimeMillis();
            log.trace(": " + String.valueOf(end - begin) + " ms");
        }

        return infos;
        
    }

}
