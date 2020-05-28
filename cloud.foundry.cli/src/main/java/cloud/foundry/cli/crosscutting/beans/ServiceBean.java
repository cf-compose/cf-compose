package cloud.foundry.cli.crosscutting.beans;

import org.cloudfoundry.operations.services.ServiceInstanceSummary;
import org.cloudfoundry.operations.services.ServiceInstanceType;

import java.util.List;

/**
 * Bean holding all data that is related to an instance of a service.
 */
public class ServiceBean implements Bean {

    private String id;
    /**
     * TODO : replace this field, since it will be stored in the map in the bean above (SpecBean)
     * TODO : requires adjustment in the service operations
     */
    private String name;
    private String service;
    private List<String> applications;
    private String lastOperation;
    private String plan;
    private List<String> tags;
    private ServiceInstanceType type;

    public ServiceBean(ServiceInstanceSummary serviceInstanceSummary) {
        this.id = serviceInstanceSummary.getId();
        this.service = serviceInstanceSummary.getService();
        this.applications = serviceInstanceSummary.getApplications();
        this.lastOperation = serviceInstanceSummary.getLastOperation();
        this.plan = serviceInstanceSummary.getPlan();
        this.tags = serviceInstanceSummary.getTags();
        this.type = serviceInstanceSummary.getType();
    }

    public ServiceBean() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getApplications() {
        return applications;
    }

    public void setApplications(List<String> applications) {
        this.applications = applications;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLastOperation() {
        return lastOperation;
    }

    public void setLastOperation(String lastOperation) {
        this.lastOperation = lastOperation;
    }

    public String getPlan() {
        return plan;
    }

    public void setPlan(String plan) {
        this.plan = plan;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public ServiceInstanceType getType() {
        return type;
    }

    public void setType(ServiceInstanceType type) {
        this.type = type;
    }
}