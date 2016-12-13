/**
 * 
 */
package fr.toutatice.ecm.checkin.listener;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.IterableQueryResult;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.query.sql.NXQL;

import fr.toutatice.ecm.checkin.helper.DraftsQueryHelper;
import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;


/**
 * @author david
 *
 */
public class FolderishInfosListener implements EventListener {
    
    /** Folderish webIds request. */
    private static final String DELETED_FOLDERISH_WEBIDS_QUERY = "select ttc:webid from Document where ecm:mixinType = 'Folderish' "
            + "and ecm:ancestorId = '%s' and ecm:currentLifeCycleState = 'deleted' and ecm:isVersion = 0 and ecm:isProxy = 0";

    /**
     * Store removed Folderish webIds to remove Drafts
     * (via parentCheckoutId).
     */
    @Override
    public void handleEvent(Event event) throws ClientException {
        if(DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName())
                && event.getContext() instanceof DocumentEventContext){
            
            DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
            CoreSession session = docCtx.getCoreSession();
            DocumentModel srcDoc = docCtx.getSourceDocument();
            
            if(srcDoc.isFolder() && srcDoc.hasSchema(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE)){
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
        StringBuffer wIds = new StringBuffer().append("('")
                .append(currentWebId)
                .append("'");
        
        IterableQueryResult results = session.queryAndFetch(String.format(DELETED_FOLDERISH_WEBIDS_QUERY, srcDoc.getId()), NXQL.NXQL, new Object[0]);
        if(results != null){
            
            Iterator<Map<String, Serializable>> iterator = results.iterator();
            
            while(iterator.hasNext()){
                wIds.append(",'");
                
                String webId = (String) iterator.next().get(ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID);
                wIds.append(webId);
                
                wIds.append("'");
            }
            
            results.close();
        }
        wIds.append(")");
        docCtx.setProperty(DraftsQueryHelper.PARENT_CHECKOUT_IDS_LIST, wIds.toString());
    }
    
    

}
