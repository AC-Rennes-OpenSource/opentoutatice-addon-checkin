/**
 * 
 */
package fr.toutatice.ecm.checkin.portal.infos.provider;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsResult;
import org.nuxeo.elasticsearch.query.NxQueryBuilder;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.checkin.helper.DocumentCheckinHelper;
import fr.toutatice.ecm.checkin.helper.DocumentHelper;
import fr.toutatice.ecm.platform.core.services.infos.provider.DocumentInformationsProvider;


/**
 * @author david
 *
 */
public class CheckinFolderishInfosProvider implements DocumentInformationsProvider {
    
    /** Drafts query and fetch clause. */
    private static final String DRAFTS_QUERY_AND_FETCH_CLAUSE = "select ecm:uuid from Document where ";  
    /** Drafts query. */
    public static final String DRAFTS_QUERY_WHERE_CLAUSE = " ecm:path startswith '%s' and ecm:mixinType = 'OttcDraft'"
            .concat(" and ottcDft:checkoutParentId = '%s' and ecm:currentLifeCycleState <> 'deleted'");
    /** Query and fetch drafts. */
    public static final String DRAFTS_QUERY_AND_FETCH = DRAFTS_QUERY_AND_FETCH_CLAUSE.concat(DRAFTS_QUERY_WHERE_CLAUSE);
    
    /** Elasticseach service. */
    private static ElasticSearchService esService;
    
    /**
     * Constructor.
     */
    public CheckinFolderishInfosProvider() {
        super();
    }
    
    protected static ElasticSearchService getElasticSearchService(){
        if(esService == null){
            esService = Framework.getService(ElasticSearchService.class);
        }
        return esService;
    }
    
    /**
     * Checks if Folderish has Drafts.
     */
    @Override
    public Map<String, Object> fetchInfos(CoreSession coreSession, DocumentModel currentDocument) throws ClientException {
        Map<String, Object> infos = new HashMap<String, Object>(1);
        if(currentDocument.isFolder()){
            
            NxQueryBuilder queryBuilder = new NxQueryBuilder(coreSession);
            String userWsPath = DocumentCheckinHelper.getInstance().getDraftsFolderPath(coreSession, currentDocument);
            String checkoutParentId = DocumentHelper.getId(currentDocument);
            String query = String.format(DRAFTS_QUERY_AND_FETCH, userWsPath, checkoutParentId);
            queryBuilder.nxql(query);
            
            EsResult draftsIds = getElasticSearchService().queryAndAggregate(queryBuilder);
            if(draftsIds != null && draftsIds.getRows() != null){
                Boolean hasDrafts  = Boolean.valueOf(draftsIds.getRows().size() > 0);
                infos.put("hasDrafts", hasDrafts);
            }
            
        }
        return infos;
    }

}
