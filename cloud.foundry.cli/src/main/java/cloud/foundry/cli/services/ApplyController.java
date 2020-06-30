package cloud.foundry.cli.services;

import cloud.foundry.cli.crosscutting.logging.Log;
import cloud.foundry.cli.crosscutting.mapping.CfOperationsCreator;
import cloud.foundry.cli.crosscutting.mapping.YamlMapper;
import cloud.foundry.cli.crosscutting.mapping.beans.SpecBean;
import cloud.foundry.cli.logic.ApplyLogic;
import cloud.foundry.cli.operations.ServicesOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import picocli.CommandLine;

import java.util.concurrent.Callable;

/**
 * This class realizes the functionality that is needed for the apply commands. They provide the service of manipulating
 * the state of a cloud foundry instance such that it matches with a provided configuration file.
 */
@CommandLine.Command(name = "apply",
        header = "%n@|green Apply the configuration from a given yaml file to your cf instance.|@",
        mixinStandardHelpOptions = true,
        subcommands = {ApplyController.ApplyApplicationCommand.class,
                        ApplyController.ApplyServiceCommand.class})
public class ApplyController implements Callable<Integer> {

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    //TODO update the description as soon as the command does more than just creating applications
    @CommandLine.Command(name = "applications", description = "Create applications that are present in the given yaml" +
            " file, but not in your cf instance.")
    static class ApplyApplicationCommand implements Callable<Integer> {

        private static final Log log = Log.getLog(ApplyApplicationCommand.class);

        @CommandLine.Mixin
        private LoginCommandOptions loginOptions;

        @CommandLine.Mixin
        private YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);

            ApplyLogic applyLogic = new ApplyLogic(cfOperations);

            log.info("Interpreting YAML file...");
            SpecBean desiredSpecBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            log.info("YAML file interpreted.");

            applyLogic.applyApplications(desiredSpecBean.getApps());

            return 0;
        }
    }

    @CommandLine.Command(name = "services", description = "Create/remove services that are present in the given yaml" +
            " file, but not in your cf instance.")
    static class ApplyServiceCommand implements Callable<Integer> {

        private static final Log log = Log.getLog(ApplyServiceCommand.class);

        @CommandLine.Mixin
        private LoginCommandOptions loginOptions;

        @CommandLine.Mixin
        private YamlCommandOptions yamlCommandOptions;

        @Override
        public Integer call() throws Exception {
            DefaultCloudFoundryOperations cfOperations = CfOperationsCreator.createCfOperations(loginOptions);

            ApplyLogic applyLogic = new ApplyLogic(new ServicesOperations(cfOperations));

            log.info("Interpreting YAML file...");
            SpecBean desiredSpecBean = YamlMapper.loadBean(yamlCommandOptions.getYamlFilePath(), SpecBean.class);
            log.info("YAML file interpreted.");

            applyLogic.applyServices(desiredSpecBean.getServices());

            return 0;
        }
    }
}
