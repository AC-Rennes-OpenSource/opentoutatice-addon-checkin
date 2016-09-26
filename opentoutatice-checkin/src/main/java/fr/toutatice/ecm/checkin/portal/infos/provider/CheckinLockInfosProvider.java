/**
 * 
 */
package fr.toutatice.ecm.checkin.portal.infos.provider;

import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;

import fr.toutatice.ecm.checkin.helper.DocumentCheckinHelper;
import fr.toutatice.ecm.platform.service.lock.DocumentLockInfosProviderImpl;


/**
 * Specificity of lock in checkin case.
 * 
 * @author david
 *
 */
public class CheckinLockInfosProvider extends DocumentLockInfosProviderImpl {
    
    @Override
    public Map<String, Object> fetchInfos(CoreSession coreSession,
            DocumentModel currentDocument) throws ClientException {

        Map<String, Object> lockInfos = super.fetchInfos(coreSession, currentDocument);
        
        DocumentCheckinHelper checkinHelper = DocumentCheckinHelper.getInstance();
        if(checkinHelper.isDraft(currentDocument) || checkinHelper.hasDraft(currentDocument)){
            
            if(checkinHelper.isDraft(currentDocument)){
                lockInfos.remove(LOCK_CREATION_DATE);
                lockInfos.remove(LOCK_OWNER);
                lockInfos.put(LOCK_STATUS, LockStatus.no_lock);
            } else if(checkinHelper.hasDraft(currentDocument)){
                lockInfos.put(LOCK_STATUS, LockStatus.locked);
            }
            
            return lockInfos;
        
        }
        
        return lockInfos;
        
    }

}
