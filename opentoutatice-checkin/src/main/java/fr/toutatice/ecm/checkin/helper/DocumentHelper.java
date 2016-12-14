/**
 * 
 */
package fr.toutatice.ecm.checkin.helper;

import static fr.toutatice.ecm.checkin.constants.CheckinConstants.OTTC_WEBID;

import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;

import fr.toutatice.ecm.checkin.constants.CheckinConstants;
import fr.toutatice.ecm.platform.service.url.WebIdResolver;


/**
 * @author david
 *
 */
public class DocumentHelper {
    
    /**
     * Utility class.
     */
    private DocumentHelper() {
        super();
    }
    
    /**
     * @param document
     * @return webId of document.
     */
    public static String getId(DocumentModel document){
        return (String) document.getPropertyValue(OTTC_WEBID);
    }
    
    /**
     * @param session
     * @param webId
     * @return path from webId.
     */
    public static String getPathFromId(CoreSession session, String webId){
        DocumentModel document = WebIdResolver.getLiveDocumentByWebId(session, webId);
        if(document != null){
            return document.getPathAsString();
        }
        return StringUtils.EMPTY;
    }
    
    /**
     * @param document
     * @return parent path of document.
     */
    public static String getParentPath(DocumentModel document){
        return StringUtils.substringBefore(document.getPathAsString(), "/".concat(document.getName()));
    }
    
    /**
     * @param session
     * @param documentRef
     * @return path of document reference.
     */
    public static String getPath(CoreSession session, DocumentRef documentRef){
        DocumentModel document = session.getDocument(documentRef);
        if(document != null){
            return document.getPathAsString();
        }
        // TODO: Exception?
        return StringUtils.EMPTY;
    }
    
    /**
     * Get checkined webId of draft document.
     * 
     * @param draft
     * @return checkined webId of draft document
     */
    public static String getCheckinedIdFromDraftDoc(DocumentModel draft){
        if(draft.hasFacet(CheckinConstants.DRAFT_FACET)){
            return (String) draft.getPropertyValue(CheckinConstants.CHECKINED_DOC_ID);
        }
        return StringUtils.EMPTY;
    }
    
    /**
     * Get draft webId of checkined document.
     * 
     * @param checkined
     * @return draft webId of checkined document
     */
    public static String getDraftIdFromCheckinedDoc(DocumentModel checkined){
        if(checkined.hasFacet(CheckinConstants.CHECKINED_IN_FACET)){
            return (String) checkined.getPropertyValue(CheckinConstants.DRAFT_ID);
        }
        return StringUtils.EMPTY;
    }
    
    /**
     * Set data models of document as dirty to be saved
     * (case of checkedIn document update with draft bean data:
     * cf CheckinActions#checkout).
     * 
     * @param document
     */
    public static DocumentModel setDirty(DocumentModel document){
        Collection<DataModel> dataModelsCollection = document.getDataModelsCollection();
        if(dataModelsCollection != null){
            for (DataModel dm : dataModelsCollection) {
                Set<String> fieldsNames = dm.getMap().keySet();
                for(String fieldName : fieldsNames){
                    dm.setDirty(fieldName);
                }
            }
        }
        return document;
    }
    
}
