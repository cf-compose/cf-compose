package cloud.foundry.cli.logic.apply;

import cloud.foundry.cli.crosscutting.exceptions.ApplyExcpetion;
import cloud.foundry.cli.crosscutting.exceptions.CreationException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.beans.ApplicationBean;
import cloud.foundry.cli.logic.diff.change.CfChange;
import cloud.foundry.cli.logic.diff.change.object.CfNewObject;
import cloud.foundry.cli.logic.diff.change.object.CfObjectValueChanged;
import cloud.foundry.cli.logic.diff.change.object.CfRemovedObject;
import cloud.foundry.cli.operations.ApplicationsOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;

import java.util.List;

public class AppApplyResolver implements ApplyVisitor {

    private final DefaultCloudFoundryOperations cfOperations;
    private final String applicationName;

    public AppApplyResolver(DefaultCloudFoundryOperations cfOperations, String applicationName) {
        this.cfOperations = cfOperations;
        this.applicationName = applicationName;
    }

    @Override
    public void visitNewObject(CfNewObject newObject) throws ApplyExcpetion {
        //TODO: this US
        Object affectedObject = newObject.getAffectedObject();
        //TODO: determine what type the affected object is
        if (affectedObject instanceof ApplicationBean) {
            try {
                doCreateNewApp((ApplicationBean) affectedObject);
            } catch (CreationException e) {
                throw new ApplyExcpetion(e.getMessage());
            }
        }
        return;
    }

    private void doCreateNewApp(ApplicationBean affectedObject) throws CreationException {
        ApplicationsOperations applicationsOperations = new ApplicationsOperations(cfOperations);
        applicationsOperations.create(this.applicationName, affectedObject, false);
        Log.info("App created:", applicationName);
    }

    @Override
    public void visitObjectValueChanged(CfObjectValueChanged objectValueChanged) {
        //TODO: later US
        return;
    }

    @Override
    public void visitRemovedObject(CfRemovedObject removedObject) {
        //TODO: later US
        return;
    }

    public void applyOnAppChanges(List<CfChange> applicationChanges) throws ApplyExcpetion {
        for (CfChange applicationChange : applicationChanges) {
            applicationChange.accept(this);
        }
    }
}
