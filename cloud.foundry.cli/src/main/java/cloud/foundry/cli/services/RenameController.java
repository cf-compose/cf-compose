package cloud.foundry.cli.services;


import cloud.foundry.cli.crosscutting.exceptions.UpdateException;
import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.operations.ApplicationsOperations;
import cloud.foundry.cli.operations.ServicesOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import reactor.core.publisher.Mono;

import java.util.concurrent.Callable;

import static picocli.CommandLine.*;
import static picocli.CommandLine.usage;

/**
 * TODO logs docu readme
 */
@Command(name = "rename", header = "%n@|green TODO|@", subcommands = {RenameController.RenameApplicationCommand.class,
        RenameController.RenameServiceCommand.class})
public class RenameController implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        usage(this, System.out);
        return 0;
    }

    @Command(name = "application", description = "Rename an application")
    static class RenameApplicationCommand implements Callable<Integer> {

        private static final Log log = Log.getLog(RenameApplicationCommand.class);

        @Mixin
        LoginCommandOptions loginOptions;

        @Parameters(index = "0", description = "The current name of the app")
        private String currentName;

        @Parameters(index = "1", description = "The new name of the app")
        private String newName;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ApplicationsOperations applicationOperations = new ApplicationsOperations(cfOperations);
            Mono<Void> toRename = applicationOperations.rename(newName, currentName);
            try {
                toRename.block();
            } catch (RuntimeException e) {
                throw new UpdateException(e);
            }
            return 0;
        }
    }

    @Command(name = "service", description = "Rename a service")
    static class RenameServiceCommand implements Callable<Integer> {

        private static final Log log = Log.getLog(RenameController.RenameServiceCommand.class);
        @Mixin
        LoginCommandOptions loginOptions;
        @Parameters(index = "0", description = "The current name of the service")
        private String currentName;
        @Parameters(index = "1", description = "The new name of the service")
        private String newName;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);
            ServicesOperations servicesOperations = new ServicesOperations(cfOperations);
            Mono<Void> toRename = servicesOperations.rename(newName, currentName);
            try {
                toRename.block();
            } catch (RuntimeException e) {
                throw new UpdateException(e);
            }
            return 0;
        }
    }
}
