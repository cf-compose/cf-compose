package cloud.foundry.cli.logic.apply;

import cloud.foundry.cli.crosscutting.exceptions.ApplyException;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationManifestBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.container.CfContainerChange;
import cloud.foundry.cli.logic.diff.change.map.CfMapChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.operations.ApplicationsOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.LinkedList;
import java.util.List;

/**
 * This class is responsible to build the requests in the context of
 * applications according to the CfChanges. The class does creates the request
 * tasks by implementing the {@link CfChangeVisitor} interface.
 */
public class ApplicationRequestsPlaner implements CfChangeVisitor {

    private static final Log log = Log.getLog(ApplicationRequestsPlaner.class);

    private final ApplicationsOperations appOperations;
    private final String applicationName;
    private final List<Mono<Void>> requests;

    private ApplicationRequestsPlaner(ApplicationsOperations appOperations, String applicationName) {
        this.appOperations = appOperations;
        this.applicationName = applicationName;
        this.requests = new LinkedList<>();
    }

    /**
     * Creates the request for CfNewObject
     * 
     * @param newObject the CfNewObject to be visited
     * 
     * @throws IllegalArgumentException if the newObject is neither an
     *                                  ApplicationBean or an
     *                                  ApplicationManifestBean
     * @throws ApplyException           that wraps the exceptions that can occur
     *                                  during the creation procedure.
     */
    @Override
    public void visitNewObject(CfNewObject newObject) {
        Object affectedObject = newObject.getAffectedObject();
        if (affectedObject instanceof ApplicationBean) {
            try {
                addCreateAppRequest((ApplicationBean) affectedObject);
            } catch (CreationException | IllegalArgumentException | NullPointerException | SecurityException e) {
                throw new ApplyException(e);
            }
        } else if (!(affectedObject instanceof ApplicationManifestBean)) {
            throw new IllegalArgumentException("Only changes of applications and manifests are permitted.");
        }
        return;
    }

    private void addCreateAppRequest(ApplicationBean affectedObject) throws CreationException {
        this.requests.add(this.appOperations.create(this.applicationName, affectedObject, false));
    }

    /**
     * Creates the requests for CfObjectValueChanged
     * 
     * @param objectValueChanged the CfObjectValueChanged to be visited
     */
    @Override
    public void visitObjectValueChanged(CfObjectValueChanged objectValueChanged) {
        // TODO: later US
        return;
    }

    /**
     * Creates the requests for CfRemovedObject
     * 
     * @param removedObject the CfRemovedObject to be visited
     * @throws ApplyException           if an error during the apply logic occurs.
     *                                  May contain another exception inside with
     *                                  more details
     * @throws IllegalArgumentException if the newObject is neither an
     *                                  ApplicationBean or an
     *                                  ApplicationManifestBean
     */
    @Override
    public void visitRemovedObject(CfRemovedObject removedObject) {
              
        Object affectedObject = removedObject.getAffectedObject();
        if (affectedObject instanceof ApplicationBean) {
            addRemoveAppRequest();
        } else if (!(affectedObject instanceof ApplicationManifestBean)) {
            throw new IllegalArgumentException("Only changes of applications and manifests are permitted.");
        }
        return;
    }

    private void addRemoveAppRequest() {
        this.requests.add(this.appOperations.remove(this.applicationName));
    }

    /**
     * Creates the requests CfContainerChange
     * 
     * @param containerChange the CfContainerChange to be visited
     */
    @Override
    public void visitContainerChange(CfContainerChange containerChange) {
        // TODO: later US
        return;
    }

    /**
     * Creates the requests for CfMapChange
     * 
     * @param mapChange the CfMapChange to be visited
     */
    @Override
    public void visitMapChange(CfMapChange mapChange) {
        // TODO: later US
        return;
    }

    /**
     * Creates the remove/create requests for one application.
     * 
     * @param appOperations      the ApplicationOperations object used for
     * @param applicationName    the name of the application
     * @param applicationChanges a list with all the Changes found during diff for
     *                           that specific application
     * @return flux of all requests that are required to apply the changes
     */
    public static Flux<Void> apply(ApplicationsOperations appOperations, String applicationName,
        List<CfChange> applicationChanges) {
        ApplicationRequestsPlaner applicationRequestsPlaner = new ApplicationRequestsPlaner(appOperations,
            applicationName);

        for (CfChange applicationChange : applicationChanges) {
            applicationChange.accept(applicationRequestsPlaner);
        }

        return Flux.merge(applicationRequestsPlaner.requests);
    }

}
