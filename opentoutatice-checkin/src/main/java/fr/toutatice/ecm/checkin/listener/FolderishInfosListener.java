/**
 * 
 */
package fr.toutatice.ecm.checkin.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.elasticsearch.api.ElasticSearchService;
import org.nuxeo.elasticsearch.api.EsIterableQueryResultImpl;
import org.nuxeo.elasticsearch.api.EsScrollResult;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.checkin.helper.DraftsQueryHelper;
import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;
import fr.toutatice.ecm.platform.core.query.helper.ToutaticeEsQueryHelper;


/**
 * @author david
 *
 */
public class FolderishInfosListener implements EventListener {

    private static final Log log = LogFactory.getLog(FolderishInfosListener.class);

    /** Folderish webIds request. */
    private static final String DELETED_FOLDERISH_WEBIDS_QUERY = "select ttc:webid from Document where ecm:mixinType = 'Folderish' "
            + "and ecm:ancestorId = '%s' and ecm:currentLifeCycleState = 'deleted' and ecm:isVersion = 0 and ecm:isProxy = 0";

    /** Elasticsearch Service. */
    private static ElasticSearchService ess;

    private static ElasticSearchService getEss() {
        if (ess == null) {
            ess = Framework.getService(ElasticSearchService.class);
        }
        return ess;
    }

    /**
     * Store removed Folderish webIds to remove Drafts
     * (via parentCheckoutId).
     */
    @Override
    public void handleEvent(Event event) throws NuxeoException {
        if (DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName()) && event.getContext() instanceof DocumentEventContext) {

            DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
            CoreSession session = docCtx.getCoreSession();
            DocumentModel srcDoc = docCtx.getSourceDocument();

            if (srcDoc.isFolder() && srcDoc.hasSchema(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE)) {
                storeRemovedWebids(docCtx, session, srcDoc);
            }

        }
    }

    /**
     * Store removed Folderish webIds to remove Drafts
     * (via parentCheckoutId).
     * 
     * @param docCtx
     * @param session
     * @param srcDoc
     */
    private void storeRemovedWebids(DocumentEventContext docCtx, CoreSession session, DocumentModel srcDoc) {
        // Store at least currentFolder
        String currentWebId = (String) srcDoc.getPropertyValue(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID);
        StringBuffer wIds = new StringBuffer().append("('").append(currentWebId).append("'");

        EsScrollResult scrollResult = ToutaticeEsQueryHelper.unrestrictedScroll(session, String.format(DELETED_FOLDERISH_WEBIDS_QUERY, srcDoc.getId()));
        EsIterableQueryResultImpl iterable = new EsIterableQueryResultImpl(getEss(), scrollResult);

        while (iterable.hasNext()) {
            wIds.append(",'");

            String webId = (String) iterable.next().get(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID);
            wIds.append(webId);

            wIds.append("'");

        }
        iterable.close();

        wIds.append(")");

        docCtx.setProperty(DraftsQueryHelper.PARENT_CHECKOUT_IDS_LIST, wIds.toString());

        if (log.isDebugEnabled()) {
            log.debug("Folderish to remove: " + wIds.toString());
        }
    }


}
