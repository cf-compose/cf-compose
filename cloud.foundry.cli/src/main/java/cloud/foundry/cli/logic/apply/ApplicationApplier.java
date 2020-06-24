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

import java.util.List;

/**
 * This class is responsible to apply in the context of applications according to the CfChanges.
 * The class performs the apply task by implementing the {@link CfChangeVisitor} interface.
 */
public class ApplicationApplier implements CfChangeVisitor {

    private static final Log log = Log.getLog(ApplicationApplier.class);

    private final ApplicationsOperations appOperations;
    private final String applicationName;

    private ApplicationApplier(ApplicationsOperations appOperations, String applicationName) {
        this.appOperations = appOperations;
        this.applicationName = applicationName;
    }

    /**
     * Apply logic for CfNewObject
     * @param newObject the CfNewObject to be visited
     * @throws IllegalArgumentException if the newObject is neither an ApplicationBean or an ApplicationManifestBean
     * @throws ApplyException that wraps the exceptions that can occur during the creation procedure.
     */
    @Override
    public void visitNewObject(CfNewObject newObject) {
        Object affectedObject = newObject.getAffectedObject();
        if (affectedObject instanceof ApplicationBean) {
            try {
                doCreateNewApp((ApplicationBean) affectedObject);
            } catch (CreationException | IllegalArgumentException | NullPointerException | SecurityException e) {
                throw new ApplyException(e);
            }
        }
        else if (!(affectedObject instanceof ApplicationManifestBean)) {
            throw new IllegalArgumentException("Only changes of applications and manifests are permitted.");
        }
        return;
    }

    private void doCreateNewApp(ApplicationBean affectedObject) throws CreationException {
        this.appOperations.create(this.applicationName, affectedObject, false);
        log.info("App created:", applicationName);
    }

    /**
     * Apply logic for CfObjectValueChanged
     * @param objectValueChanged the CfObjectValueChanged to be visited
     */
    @Override
    public void visitObjectValueChanged(CfObjectValueChanged objectValueChanged) {
        //TODO: later US
        return;
    }

    /**
     * Apply logic for CfRemovedObject
     * @param removedObject the CfRemovedObject to be visited
     */
    @Override
    public void visitRemovedObject(CfRemovedObject removedObject) {
        //TODO: later US
        return;
    }

    /**
     * Apply logic for CfContainerChange
     * @param containerChange the CfContainerChange to be visited
     */
    @Override
    public void visitContainerChange(CfContainerChange containerChange) {
        //TODO: later US
        return;
    }

    /**
     * Apply logic for CfMapChange
     * @param mapChange the CfMapChange to be visited
     */
    @Override
    public void visitMapChange(CfMapChange mapChange) {
        //TODO: later US
        return;
    }

    /**
     * Apply for all changes regarding one application. If an error occurs, the applying procedure is
     * discontinued and the app stops.
     * @param appOperations the ApplicationOperations object used for
     * @param applicationName the name of the application
     * @param applicationChanges a list with all the Changes found during diff for that specific application
     * @throws ApplyException if an error during the apply logic occurs. May contain another exception inside
     * with more details
     * @throws IllegalArgumentException if the newObject is neither an ApplicationBean or an ApplicationManifestBean
     */
    public static void apply(ApplicationsOperations appOperations, String applicationName,
                             List<CfChange> applicationChanges) {
        ApplicationApplier applicationApplier = new ApplicationApplier(appOperations, applicationName);
        for (CfChange applicationChange : applicationChanges) {
            applicationChange.accept(applicationApplier);
        }
    }
}
